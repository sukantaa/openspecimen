package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class FilesBacklogCleaner implements ScheduledTask {
	private static final Log logger = LogFactory.getLog(FilesBacklogCleaner.class);

	private static final String QUERY_EXPORT_DIR = "query-exported-data";

	private static final String LOG_FILES_DIR = "logs";

	private static final int period = 30; //30 days
	
	@Override
	public void doJob(ScheduledJobRun jobRun) 
	throws Exception {
		cleanupOlderFiles(getQueryExportDataDir(), period);
		cleanupOlderFiles(getLogFilesDir(), getLogFilesRetainPeriod());
	}

	private String getQueryExportDataDir() {
		return new StringBuilder(ConfigUtil.getInstance().getDataDir()).append(File.separator)
			.append(QUERY_EXPORT_DIR).append(File.separator)
			.toString();
	}

	private String getLogFilesDir() {
		return new StringBuilder(ConfigUtil.getInstance().getDataDir()).append(File.separator)
			.append(LOG_FILES_DIR).append(File.separator)
			.toString();
	}

	private int getLogFilesRetainPeriod() {
		return ConfigUtil.getInstance().getIntSetting("common", "log_files_retain_period", period);
	}

	private static void cleanupOlderFiles(String dataDir, int period) {
		if (period <= 0) {
			return;
		}

		Calendar timeBefore = Calendar.getInstance();
		timeBefore.setTime(new Date());
		timeBefore.add(Calendar.DATE, -period);
		Long timeInMilliseconds = timeBefore.getTimeInMillis();

		cleanupDir(dataDir, timeInMilliseconds);
	}

	private static void cleanupDir(String directory, Long timeBefore) {
		try {
			File dir = new File(directory);

			if (!dir.exists() || !dir.isDirectory()) {
				return;
			}

			for (File file : dir.listFiles()) {
				if (!file.isDirectory() && (file.lastModified() < timeBefore)) {
					file.delete();
				}
			}
		} catch (Exception e) {
			logger.error("Error deleting files from directory: " + directory, e);
		}
	}
}
