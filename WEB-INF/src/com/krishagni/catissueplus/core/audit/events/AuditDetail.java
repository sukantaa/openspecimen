package com.krishagni.catissueplus.core.audit.events;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class AuditDetail {

	private UserSummary createdBy;

	private Date createdOn;

	private UserSummary lastUpdatedBy;

	private Date lastUpdatedOn;

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public UserSummary getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public void setLastUpdatedBy(UserSummary lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public Date getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Date lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}
}
