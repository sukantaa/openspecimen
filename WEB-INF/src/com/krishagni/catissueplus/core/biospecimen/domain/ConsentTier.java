package com.krishagni.catissueplus.core.biospecimen.domain;

public class ConsentTier extends BaseEntity {
	private ConsentStatement statement;
	
	private String activityStatus;

	public ConsentStatement getStatement() {
		return statement;
	}

	public void setStatement(ConsentStatement statement) {
		this.statement = statement;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}
}
