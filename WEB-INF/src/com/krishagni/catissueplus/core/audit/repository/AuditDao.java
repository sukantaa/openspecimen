package com.krishagni.catissueplus.core.audit.repository;

import java.util.Date;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface AuditDao extends Dao<UserApiCallLog> {
	AuditDetail getAuditDetail(String auditTable, Long objectId);
	
	Date getLatestApiCallTime(Long userId, String token);
}

