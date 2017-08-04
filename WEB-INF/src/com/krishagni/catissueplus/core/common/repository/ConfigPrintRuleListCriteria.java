package com.krishagni.catissueplus.core.common.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ConfigPrintRuleListCriteria extends AbstractListCriteria<ConfigPrintRuleListCriteria> {
	private String objectType;

	private String cpTitle;

	private String instituteName;

	private String userName;

	private String activityStatus;

	@Override
	public ConfigPrintRuleListCriteria self() {
		return this;
	}

	public String objectType() {
		return this.objectType;
	}

	public ConfigPrintRuleListCriteria objectType(String objectType) {
		this.objectType = objectType;
		return self();
	}

	public String cpTitle() {
		return this.cpTitle;
	}

	public ConfigPrintRuleListCriteria cpTitle(String cpTitle) {
		this.cpTitle = cpTitle;
		return self();
	}

	public String instituteName() {
		return this.instituteName;
	}

	public ConfigPrintRuleListCriteria instituteName(String instituteName) {
		this.instituteName = instituteName;
		return self();
	}

	public String userName() {
		return this.userName;
	}

	public ConfigPrintRuleListCriteria userName(String userName) {
		this.userName = userName;
		return self();
	}

	public String activityStatus() {
		return this.activityStatus;
	}

	public ConfigPrintRuleListCriteria activityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
		return self();
	}
}
