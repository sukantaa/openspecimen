package com.krishagni.catissueplus.core.exporter.services.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileReader;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvReader;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.exporter.domain.ExportErrorCode;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.events.ExportDetail;
import com.krishagni.catissueplus.core.exporter.events.ExportJobDetail;
import com.krishagni.catissueplus.core.exporter.repository.ExportJobDao;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.services.ObjectSchemaFactory;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.nutility.IoUtil;

public class ExportServiceImpl implements ExportService {
	private final static Log logger = LogFactory.getLog(ExportServiceImpl.class);

	private Map<String, Supplier<Function<ExportJob, List<? extends Object>>>> genFactories = new HashMap<>();

	private ExportJobDao exportJobDao;

	private ObjectSchemaFactory schemaFactory;

	private ThreadPoolTaskExecutor taskExecutor;

	public void setSchemaFactory(ObjectSchemaFactory schemaFactory) {
		this.schemaFactory = schemaFactory;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setExportJobDao(ExportJobDao exportJobDao) {
		this.exportJobDao = exportJobDao;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportJobDetail> exportObjects(RequestEvent<ExportDetail> req) {
		if (!AuthUtil.isAdmin() && !AuthUtil.isInstituteAdmin()) {
			return ResponseEvent.userError(RbacErrorCode.INST_ADMIN_RIGHTS_REQ, AuthUtil.getCurrentUserInstitute().getName());
		}

		ExportDetail detail = req.getPayload();

		ObjectSchema schema = schemaFactory.getSchema(detail.getObjectType());
		if (schema == null) {
			return ResponseEvent.userError(ExportErrorCode.INVALID_OBJECT_TYPE, detail.getObjectType());
		}

		Supplier<Function<ExportJob, List<? extends Object>>> generator = genFactories.get(detail.getObjectType());
		if (generator == null) {
			return ResponseEvent.userError(ExportErrorCode.NO_GEN_FOR_OBJECT_TYPE, detail.getObjectType());
		}

		ExportJob job = new ExportJob();
		job.setName(detail.getObjectType());
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setCreationTime(Calendar.getInstance().getTime());
		job.setParams(detail.getParams());
		job.setSchema(schema);
		job.setRecordIds(detail.getRecordIds());
		exportJobDao.saveOrUpdate(job.markInProgress(), true);

		taskExecutor.execute(new ExportTask(job));
		return ResponseEvent.response(ExportJobDetail.from(job));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getExportFile(RequestEvent<Long> req) {
		Long jobId = req.getPayload();
		ExportJob job = exportJobDao.getById(jobId);
		if (job == null) {
			return ResponseEvent.userError(ExportErrorCode.JOB_NOT_FOUND, jobId);
		}

		if (!AuthUtil.isAdmin() && !job.getCreatedBy().equals(AuthUtil.getCurrentUser())) {
			return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return ResponseEvent.response(getOutputFile(job));
	}

	@Override
	public void registerObjectsGenerator(String type, Supplier<Function<ExportJob, List<? extends Object>>> genFactory) {
		genFactories.put(type, genFactory);
	}

	private class ExportTask implements Runnable {
		private ExportJob job;

		private Function<ExportJob, List<? extends Object>> generator;

		private Map<String, Integer> fieldInstances = new HashMap<>();

		private CsvWriter writer;

		private CsvReader reader;

		private long written = 0;

		private File filesDir;

		private long filesCount = 0;

		private SimpleDateFormat df;

		private SimpleDateFormat tf;

		ExportTask(ExportJob inputJob) {
			job = inputJob;
			generator = genFactories.get(job.getName()).get();

			String dateFmt = ConfigUtil.getInstance().getDeDateFmt();
			df = new SimpleDateFormat(dateFmt);
			tf = new SimpleDateFormat(dateFmt + " " + ConfigUtil.getInstance().getTimeFmt());
		}

		@Override
		public void run() {
			if (!new File(getJobDir(job)).mkdirs()) {
				failed();
				return;
			}

			try {
				AuthUtil.setCurrentUser(job.getCreatedBy());
				generateRawRecordsFile();
				generateOutputFile();
				generateOutputZip(job);
				completed();
			} catch (Exception e) {
				failed();
				logger.error("Error exporting records", e);
			} finally {
				AuthUtil.clearCurrentUser();
				cleanupFiles(job);
				sendJobStatusNotification(job);
			}
		}

		private void generateRawRecordsFile()
		throws IOException {
			ObjectSchema.Record record = job.getSchema().getRecord();
			List<ObjectSchema.Field> fileFields = getFileFields(record);

			try {
				openWriter(getRawDataFile(job));

				long records = 0;
				List<? extends Object> objects;
				while (true) {
					objects = getObjects();
					if (CollectionUtils.isEmpty(objects)) {
						break;
					}

					for (Object object : objects) {
						write(getRawDataRow("", "", record, object));
						++records;

						if (records % 50 == 0) {
							updateRecordsCount(records);
						}
					}
				}

				if (records % 50 != 0) {
					updateRecordsCount(records);
				}

				flush();
			} finally {
				closeWriter();
			}
		}

		private List<String> getRawDataRow(String namePrefix, String captionPrefix, ObjectSchema.Record record, Object object) {
			List<String> row = new ArrayList<>();

			for (ObjectSchema.Field field : record.getFields()) {
				String caption = captionPrefix + field.getCaption();

				if (field.isMultiple()) {
					Collection<Object> collection = getCollection(object, field.getAttribute());
					int count = 0;
					for (Object element : collection) {
						row.add(caption + "#" + ++count);
						row.add(element.toString());
					}

					updateFieldCount(namePrefix + field.getAttribute(), count);
				} else {
					String value = getString(object, field);
					if ("file".equals(field.getType()) && StringUtils.isNotBlank(value)) {
						value = ++filesCount + "_" + value;
						writeFileData(object, field, value);
					}

					row.add(caption);
					row.add(value);
				}
			}

			for (ObjectSchema.Record subRecord : record.getSubRecords()) {
				String srName = namePrefix + subRecord.getAttribute();
				String srCaption = captionPrefix;
				if (StringUtils.isNotBlank(subRecord.getCaption())) {
					srCaption += subRecord.getCaption() + "#";
				}

				if (subRecord.isMultiple()) {
					Collection<Object> collection = getCollection(object, subRecord.getAttribute());
					int count = 0;
					for (Object element : collection) {
						row.addAll(getRawDataRow(srName + ".", srCaption + ++count + "#", subRecord, element));
					}

					updateFieldCount(srName, count);
				} else {
					Object subObject = getObject(object, subRecord.getAttribute());
					if (subObject != null) {
						row.addAll(getRawDataRow(srName + ".", srCaption, subRecord, subObject));
					}
				}
			}

			return row;
		}

		private List<ObjectSchema.Field> getFileFields(ObjectSchema.Record record) {
			return record.getFields().stream().filter(f -> "file".equals(f.getType())).collect(Collectors.toList());
		}

		private void writeFileData(Object object, ObjectSchema.Field field, String filename) {
			try {
				if (filesDir == null) {
					filesDir = new File(getJobDir(job), "files");
				}

				File destFile = new File(filesDir, filename);
				String fileVar = field.getFile();
				if (StringUtils.isBlank(fileVar)) {
					fileVar = "documentFile";
				}

				Object value = getObject(object, fileVar);
				FileUtils.copyFile((File) value, destFile);
			} catch (IOException ioe) {
				throw new RuntimeException("Error writing to file", ioe);
			}
		}

		private void generateOutputFile() {
			try {
				openReader(getRawDataFile(job));
				openWriter(getOutputCsvFile(job));

				write(getHeaderRow("", "", job.getSchema().getRecord()));

				String[] row;
				while ((row = nextLine()) != null) {
					Map<String, String> rowValueMap = new HashMap<>();
					for (int i = 0; i < row.length; i += 2) {
						rowValueMap.put(row[i], row[i + 1]);
					}

					write(getDataRow("", "", job.getSchema().getRecord(), rowValueMap));
				}

				flush();
			} finally {
				closeReader();
				closeWriter();
			}
		}

		private List<String> getHeaderRow(String namePrefix, String captionPrefix, ObjectSchema.Record record) {
			List<String> row = new ArrayList<>();

			for (ObjectSchema.Field field : record.getFields()) {
				if (field.getAttribute().equals(ID_ATTR)) {
					continue;
				}

				String caption = captionPrefix + field.getCaption();
				if (field.isMultiple()) {
					int count = getFieldCount(namePrefix + field.getAttribute());
					for (int i = 1; i <= count; ++i) {
						row.add(caption + "#" + i);
					}
				} else {
					row.add(caption);
				}
			}

			for (ObjectSchema.Record subRecord : record.getSubRecords()) {
				String srName = namePrefix + subRecord.getAttribute();
				String srCaption = captionPrefix;
				if (StringUtils.isNotBlank(subRecord.getCaption())) {
					srCaption += subRecord.getCaption() + "#";
				}

				if (subRecord.isMultiple()) {
					int count = getFieldCount(srName);
					for (int i = 1; i <= count; ++i) {
						row.addAll(getHeaderRow(srName + ".", srCaption + i + "#", subRecord));
					}
				} else {
					row.addAll(getHeaderRow(srName + ".", srCaption, subRecord));
				}
			}

			return row;
		}

		private List<String> getDataRow(String namePrefix, String captionPrefix, ObjectSchema.Record record, Map<String, String> valueMap) {
			List<String> result = new ArrayList<>();

			for (ObjectSchema.Field field : record.getFields()) {
				if (field.getAttribute().equals(ID_ATTR)) {
					//
					// hack to exclude ID field from the exported file.
					//
					continue;
				}

				String caption = captionPrefix + field.getCaption();
				if (field.isMultiple()) {
					Integer count = getFieldCount(namePrefix + field.getAttribute());
					for (int i = 1; i <= count; ++i) {
						String key = caption + "#" + i;
						result.add(valueMap.get(key));
					}
				} else {
					result.add(valueMap.get(caption));
				}
			}

			for (ObjectSchema.Record subRecord : record.getSubRecords()) {
				String srName = namePrefix + subRecord.getAttribute();
				String srCaption = captionPrefix;
				if (StringUtils.isNotBlank(subRecord.getCaption())) {
					srCaption += subRecord.getCaption() + "#";
				}

				if (subRecord.isMultiple()) {
					int count = getFieldCount(srName);
					for (int i = 1; i <= count; ++i) {
						result.addAll(getDataRow(srName + ".", srCaption + i + "#", subRecord, valueMap));
					}
				} else {
					result.addAll(getDataRow(srName + ".", srCaption, subRecord, valueMap));
				}
			}

			return result;
		}

		private void updateFieldCount(String field, int count) {
			Integer value = fieldInstances.get(field);
			if (value == null || count > value) {
				fieldInstances.put(field, count == 0 ? 1 : count);
			}
		}

		private int getFieldCount(String field) {
			Integer count = fieldInstances.get(field);
			return count == null ? 1 : count;
		}

		private void openWriter(String file) {
			writer = CsvFileWriter.createCsvFileWriter(new File(file));
		}

		private void write(List<String> row) {
			writer.writeNext(row.toArray(new String[0]));
			++written;

			if (written % 50 == 0) {
				flush();
			}
		}

		private void flush() {
			try {
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException("Error writing to CSV file", e);
			}
		}

		private void closeWriter() {
			IOUtils.closeQuietly(writer);
			written = 0;
		}

		private void openReader(String file) {
			reader = CsvFileReader.createCsvFileReader(file, false);
		}

		private String[] nextLine() {
			return reader.next() ? reader.getRow() : null;
		}

		private void closeReader() {
			IOUtils.closeQuietly(reader);
		}

		private String getString(Object object, ObjectSchema.Field field) {
			try {
				String result;

				Object value = getObject(object, field.getAttribute());
				if (object instanceof Date && "date".equals(field.getType())) {
					result = getDateString(value);
				} else if (object instanceof Date && "datetime".equals(field.getType())) {
					result = getDateTimeString(value);
				} else {
					result = ObjectUtils.toString(value);
				}

				return result;
			} catch (Exception e) {
				throw new IllegalArgumentException("Error obtaining value of property: " + field.getAttribute(), e);
			}
		}

		private Object getObject(Object object, String propertyName) {
			try {
				return BeanUtilsBean2.getInstance().getPropertyUtils().getProperty(object, propertyName);
			} catch (Exception e) {
				throw new IllegalArgumentException("Error obtaining value of property: " + propertyName, e);
			}
		}

		private Collection<Object> getCollection(Object object, String propertyName) {
			try {
				Object value = getObject(object, propertyName);
				return (value == null) ? Collections.emptyList() : (Collection<Object>) value;
			} catch (Exception e) {
				throw new IllegalArgumentException("Error obtaining value of property: " + propertyName, e);
			}
		}

		private String getDateString(Object input) {
			return input == null ? null : df.format(input);
		}

		private String getDateTimeString(Object input) {
			return input == null ? null : tf.format(input);
		}

		private void updateRecordsCount(long recordsCnt) {
			job.setTotalRecords(recordsCnt);
			saveJob();
		}

		private void completed() {
			job.setEndTime(Calendar.getInstance().getTime());
			job.markCompleted();
			saveJob();
		}

		private void failed() {
			job.setEndTime(Calendar.getInstance().getTime());
			job.markFailed();
			saveJob();
		}

		@PlusTransactional
		private List<? extends Object> getObjects() {
			return generator.apply(job);
		}

		@PlusTransactional
		private void saveJob() {
			exportJobDao.saveOrUpdate(job);
		}
	}

	private String getRawDataFile(ExportJob job) {
		return getJobDir(job) + File.separator + "raw-data.csv";
	}

	private String getOutputCsvFile(ExportJob job) {
		return getJobDir(job) + File.separator + "output.csv";
	}

	private String getOutputFile(ExportJob job) {
		File zipFile = new File(getJobDir(job), "output.zip");
		if (zipFile.exists()) {
			return zipFile.getAbsolutePath();
		}

		return getOutputCsvFile(job);
	}

	private void cleanupFiles(ExportJob job) {
		new File(getRawDataFile(job)).delete();
		new File(getOutputCsvFile(job)).delete();
	}

	private void generateOutputZip(ExportJob job) throws IOException {
		String jobDir = getJobDir(job);
		File jobZipFile = new File(jobDir + ".zip");
		IoUtil.zipFiles(jobDir, jobZipFile.getAbsolutePath(), Collections.singletonList(getRawDataFile(job)));
		FileUtils.moveFile(jobZipFile, new File(jobDir, "output.zip"));
	}

	private void sendJobStatusNotification(ExportJob job) {
		String entityName = getMsg("export_entities_" + job.getName());

		String [] subjParams = {job.getId().toString(), entityName};

		Map<String, Object> props = new HashMap<>();
		props.put("job", job);
		props.put("entityName", entityName);
		props.put("status", getMsg("export_statuses_" + job.getStatus().name().toLowerCase()));
		props.put("$subject", subjParams);

		String[] rcpts = {job.getCreatedBy().getEmailAddress()};
		EmailUtil.getInstance().sendEmail(JOB_STATUS_EMAIL_TMPL, rcpts, null, props);
	}


	private String getMsg(String key, Object ... params) {
		return MessageUtil.getInstance().getMessage(key, params);
	}


	private String getJobDir(ExportJob job) {
		return ConfigUtil.getInstance().getDataDir() + File.separator +
			"export-jobs" + File.separator + job.getId();
	}

	private static final String JOB_STATUS_EMAIL_TMPL = "export_job_status_notif";

	private static final String ID_ATTR = "id";
}
