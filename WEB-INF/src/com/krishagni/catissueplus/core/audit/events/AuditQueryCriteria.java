package com.krishagni.catissueplus.core.audit.events;

public class AuditQueryCriteria {
	private String objectName;

	private Long objectId;

	public AuditQueryCriteria(String objectName, Long objectId) {
		this.objectName = objectName;
		this.objectId = objectId;
	}

	public String getObjectName() {
		return objectName;
	}

	public Long getObjectId() {
		return objectId;
	}
}
