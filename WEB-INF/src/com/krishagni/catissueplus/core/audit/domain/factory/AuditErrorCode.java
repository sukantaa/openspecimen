package com.krishagni.catissueplus.core.audit.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum AuditErrorCode implements ErrorCode {

	ENTITY_NOT_FOUND;

	@Override
	public String code() {
		return "AUDIT_" + this.name();
	}
}
