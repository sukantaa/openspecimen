package com.krishagni.catissueplus.core.audit.services;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface AuditService {
	ResponseEvent<AuditDetail> getAuditDetail(RequestEvent<AuditQueryCriteria> req);

	// Internal APIs

	void insertApiCallLog(UserApiCallLog userAuditLog);
	
	long getTimeSinceLastApiCall(Long userId, String token);
}
