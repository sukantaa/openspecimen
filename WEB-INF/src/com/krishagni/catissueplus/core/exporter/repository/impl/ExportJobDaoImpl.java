package com.krishagni.catissueplus.core.exporter.repository.impl;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.repository.ExportJobDao;

public class ExportJobDaoImpl extends AbstractDao<ExportJob> implements ExportJobDao {
	@Override
	public Class<ExportJob> getType() {
		return ExportJob.class;
	}
}
