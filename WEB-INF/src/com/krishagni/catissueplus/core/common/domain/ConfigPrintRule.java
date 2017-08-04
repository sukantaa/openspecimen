package com.krishagni.catissueplus.core.common.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.util.Status;

public class ConfigPrintRule extends BaseEntity {
	private String objectType;

	private Institute institute;

	private CollectionProtocol collectionProtocol;

	private User updatedBy;

	private Date updatedOn;

	private String activityStatus;

	private transient List<SpecimenLabelPrintRule> rules;

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Institute getInstitute() {
		return institute;
	}

	public void setInstitute(Institute institute) {
		this.institute = institute;
	}

	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public List<SpecimenLabelPrintRule> getRules() {
		return rules;
	}

	public void setRules(List<SpecimenLabelPrintRule> rules) {
		this.rules = rules;
	}

	public String getRuleDefJson() {
		try {
			return getWriteMapper().writeValueAsString(rules);
		} catch (Exception e) {
			throw new RuntimeException("Error marshalling print rule to JSON", e);
		}
	}

	public void setRuleDefJson(String ruleDefJson) {
		List<SpecimenLabelPrintRule> list = null;
		try {
			list = getReadMapper().readValue(ruleDefJson, TypeFactory.defaultInstance()
				.constructCollectionType(List.class, SpecimenLabelPrintRule.class));
		} catch (Exception e) {
			throw new RuntimeException("Error marshalling JSON to print rule", e);
		}

		this.rules = list;
	}

	public void update(ConfigPrintRule rule) {
		updateStatus(rule.getActivityStatus());
		if (isDisabled()) {
			return;
		}

		setObjectType(rule.getObjectType());
		setInstitute(rule.getInstitute());
		setCollectionProtocol(rule.getCollectionProtocol());
		setUpdatedBy(rule.getUpdatedBy());
		setUpdatedOn(rule.getUpdatedOn());
		setRules(rule.getRules());
	}

	private ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}

	private ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setVisibilityChecker(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE)
				.withCreatorVisibility(Visibility.NONE));
		return mapper;
	}

	private boolean isDisabled() {
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus());
	}

	private void updateStatus(String activityStatus) {
		if (getActivityStatus().equals(activityStatus)) {
			return;
		}

		setActivityStatus(activityStatus);
	}
}
