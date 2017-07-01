package com.krishagni.catissueplus.core.de.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.events.ListQueryAuditLogsCriteria;

public interface QueryAuditLogDao extends Dao<QueryAuditLog>{
	Long getLogsCount(ListQueryAuditLogsCriteria crit);

	List<QueryAuditLog> getLogs(ListQueryAuditLogsCriteria crit);
}
