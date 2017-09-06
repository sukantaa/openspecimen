package com.krishagni.catissueplus.core.audit.services;

import java.util.List;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditQueryCriteria;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface AuditService {
	ResponseEvent<List<AuditDetail>> getAuditDetail(RequestEvent<List<AuditQueryCriteria>> req);

	ResponseEvent<List<RevisionDetail>> getRevisions(RequestEvent<List<AuditQueryCriteria>> req);

	// Internal APIs

	void insertApiCallLog(UserApiCallLog userAuditLog);
	
	long getTimeSinceLastApiCall(Long userId, String token);
}
