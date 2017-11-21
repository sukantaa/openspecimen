package com.krishagni.catissueplus.core.exporter.services.impl;

public class ExporterContextHolder {
	private static ExporterContextHolder instance = new ExporterContextHolder();

	//
	// Thread specific contextual information
	// Stores boolean indicating whether current thread is
	// doing export operation or not.
	//
	private static ThreadLocal<Boolean> exporterCtx = null;

	private ExporterContextHolder() {
	}


	public static ExporterContextHolder getInstance() {
		return instance;
	}

	public void newContext() {
		exporterCtx = ThreadLocal.withInitial(() -> true);
	}

	public void clearContext() {
		exporterCtx.remove();
		exporterCtx = null;
	}

	public boolean isExportOp() {
		return exporterCtx != null && exporterCtx.get();
	}
}
