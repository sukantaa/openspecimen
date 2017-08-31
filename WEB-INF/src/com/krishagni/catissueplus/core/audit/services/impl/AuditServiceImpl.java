package com.krishagni.catissueplus.core.audit.services.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.domain.factory.AuditErrorCode;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditQueryCriteria;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.ObjectAccessorFactory;

public class AuditServiceImpl implements AuditService {

	private DaoFactory daoFactory;

	private ObjectAccessorFactory objectAccessorFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setObjectAccessorFactory(ObjectAccessorFactory objectAccessorFactory) {
		this.objectAccessorFactory = objectAccessorFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuditDetail> getAuditDetail(RequestEvent<AuditQueryCriteria> req) {
		AuditQueryCriteria crit = req.getPayload();
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		if (accessor == null) {
			throw OpenSpecimenException.userError(AuditErrorCode.ENTITY_NOT_FOUND, crit.getObjectName());
		}

		accessor.ensureReadAllowed(crit.getObjectId());
		AuditDetail detail = daoFactory.getAuditDao().getAuditDetail(accessor.getAuditTable(), crit.getObjectId());
		return ResponseEvent.response(detail);
	}

	@Override
	@PlusTransactional
	public void insertApiCallLog(UserApiCallLog userAuditLog) {
		daoFactory.getAuditDao().saveOrUpdate(userAuditLog);
	}

	@Override
	@PlusTransactional
	public long getTimeSinceLastApiCall(Long userId, String token) {
		Date lastApiCallTime = daoFactory.getAuditDao().getLatestApiCallTime(userId, token);
		long timeSinceLastApiCallInMilli = Calendar.getInstance().getTime().getTime() - lastApiCallTime.getTime();
		return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastApiCallInMilli);
	}
}