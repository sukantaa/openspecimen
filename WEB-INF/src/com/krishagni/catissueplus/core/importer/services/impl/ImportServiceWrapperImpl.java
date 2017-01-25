package com.krishagni.catissueplus.core.importer.services.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.importer.services.ImportServiceWrapper;
import com.krishagni.commons.util.MessageUtil;
import com.krishagni.importer.domain.ImportJob;
import com.krishagni.importer.events.FileRecordsDetail;
import com.krishagni.importer.events.ImportDetail;
import com.krishagni.importer.events.ImportJobDetail;
import com.krishagni.importer.events.ObjectSchemaCriteria;
import com.krishagni.importer.repository.ListImportJobsCriteria;
import com.krishagni.importer.services.ImportConfig;
import com.krishagni.importer.services.ImportService;

public class ImportServiceWrapperImpl implements ImportServiceWrapper, InitializingBean {
	private ImportService importSvc;

	public void setImportSvc(ImportService importSvc) {
		this.importSvc = importSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ImportJobDetail>> getImportJobs(RequestEvent<ListImportJobsCriteria> req) {
		ListImportJobsCriteria crit = req.getPayload();
		if (AuthUtil.isInstituteAdmin()) {
			crit.instituteId(AuthUtil.getCurrentUserInstitute().getId());
		} else if (!AuthUtil.isAdmin()) {
			crit.userId(AuthUtil.getCurrentUser().getId());
		}

		return Utility.invokeFn(importSvc::getImportJobs, req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ImportJobDetail> getImportJob(RequestEvent<Long> req) {
		return Utility.invokeFn(importSvc::getImportJob, req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getImportJobFile(RequestEvent<Long> req) {
		return Utility.invokeFn(importSvc::getImportJobFile, req);
	}

	@Override
	public ResponseEvent<String> uploadImportJobFile(RequestEvent<InputStream> req) {
		return Utility.invokeFn(importSvc::uploadImportJobFile, req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ImportJobDetail> importObjects(RequestEvent<ImportDetail> req) {
		return Utility.invokeFn(importSvc::importObjects, req);
	}

	@Override
	public ResponseEvent<String> getInputFileTemplate(RequestEvent<ObjectSchemaCriteria> req) {
		return Utility.invokeFn(importSvc::getInputFileTemplate, req);
	}

	@Override
	public ResponseEvent<List<Map<String, Object>>> processFileRecords(RequestEvent<FileRecordsDetail> req) {
		return Utility.invokeFn(importSvc::processFileRecords, req);
	}

	@Override
	public ResponseEvent<ImportJobDetail> stopJob(RequestEvent<Long> req) {
		return Utility.invokeFn(importSvc::stopJob, req);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setConfig();
	}

	private void setConfig() {
		importSvc.setConfig(new ImportConfig() {
			@Override
			public String getDataDir() {
				return ConfigUtil.getInstance().getDataDir();
			}

			@Override
			public boolean isAccessAllowed(ImportJob job) {
				User currentUser = AuthUtil.getCurrentUser();
				return currentUser.isAdmin() || currentUser.equals(job.getCreatedBy());
			}

			@Override
			public String getDateFmt() {
				return ConfigUtil.getInstance().getDeDateFmt();
			}

			@Override
			public String getTimeFmt() {
				return ConfigUtil.getInstance().getTimeFmt();
			}

			@Override
			public Integer getAtomicitySize() {
				return ConfigUtil.getInstance().getIntSetting("common", CFG_MAX_TXN_SIZE, MAX_RECS_PER_TXN);
			}

			@Override
			public char getFieldSeparator() {
				return Utility.getFieldSeparator();
			}

			@Override
			public Authentication getAuth() {
				return AuthUtil.getAuth();
			}

			@Override
			public void onFinish(ImportJob job) {
				sendJobStatusNotification(job);
			}
		});
	}

	private void sendJobStatusNotification(ImportJob job) {
		String entityName = getEntityName(job);
		String op = getMsg("bulk_import_ops_" + job.getType());
		String [] subjParams = new String[] {
				job.getId().toString(),
				op,
				entityName
		};

		Map<String, Object> props = new HashMap<>();
		props.put("job", job);
		props.put("entityName", entityName);
		props.put("op", op);
		props.put("status", getMsg("bulk_import_statuses_" + job.getStatus()));
		props.put("atomic", job.isAtomic());
		props.put("$subject", subjParams);

		String[] rcpts = {job.getCreatedBy().getEmailAddress()};
		EmailUtil.getInstance().sendEmail(JOB_STATUS_EMAIL_TMPL, rcpts, null, props);
	}

	private String getEntityName(ImportJob job) {
		String entityName;

		if (job.getName().equals(EXTENSIONS)) {
			entityName = job.getParams().get("formName") + " (" + job.getParams().get("entityType") + ")";
		} else {
			entityName = getMsg("bulk_import_entities_" + job.getName());
		}

		return entityName;
	}

	private String getMsg(String key, Object ... params) {
		return MessageUtil.getInstance().getMessage(key, params);
	}

	private static final int MAX_RECS_PER_TXN = 10000;

	private static final String CFG_MAX_TXN_SIZE = "import_max_records_per_txn";

	private static final String JOB_STATUS_EMAIL_TMPL = "import_job_status_notif";

	private static final String EXTENSIONS = "extensions";
}
