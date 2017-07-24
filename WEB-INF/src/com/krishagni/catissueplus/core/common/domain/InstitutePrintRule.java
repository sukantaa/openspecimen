package com.krishagni.catissueplus.core.common.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;

public class InstitutePrintRule extends BaseEntity {
	private String objectType;

	private Institute institute;

	private CollectionProtocol collectionProtocol;

	private User updatedBy;

	private Date updatedOn;

	private String status;

	private transient List<LabelPrintRule> rules;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<LabelPrintRule> getRules() {
		return rules;
	}

	public void setRules(List<LabelPrintRule> rules) {
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
		List<LabelPrintRule> list = null;
		try {
			list = getReadMapper().readValue(ruleDefJson, TypeFactory.defaultInstance()
				.constructCollectionType(List.class, LabelPrintRule.class));
		} catch (Exception e) {
			throw new RuntimeException("Error marshalling JSON to print rule", e);
		}

		this.rules = list;
	}

	private ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}

	private ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibilityChecker(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE)
				.withCreatorVisibility(Visibility.NONE));
		return mapper;
	}
}
