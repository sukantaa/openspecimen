package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ConfigPrintRuleDetail {
	private Long Id;

	private String objectType;

	private Long instituteId;

	private String instituteName;

	private Long cpId;

	private String cpTitle;

	private String cpShortTitle;

	private UserSummary updatedBy;

	private Date updatedOn;

	private String activityStatus;

	private List<SpecimenLabelPrintRuleDetail> rules;

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Long getInstituteId() {
		return instituteId;
	}

	public void setInstituteId(Long instituteId) {
		this.instituteId = instituteId;
	}

	public String getInstituteName() {
		return instituteName;
	}

	public void setInstituteName(String instituteName) {
		this.instituteName = instituteName;
	}

	public String getCpTitle() {
		return cpTitle;
	}

	public void setCpTitle(String cpTitle) {
		this.cpTitle = cpTitle;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public UserSummary getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(UserSummary updatedBy) {
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

	public List<SpecimenLabelPrintRuleDetail> getRules() {
		return rules;
	}

	public void setRules(List<SpecimenLabelPrintRuleDetail> rules) {
		this.rules = rules;
	}

	public static ConfigPrintRuleDetail from(ConfigPrintRule rule) {
		ConfigPrintRuleDetail detail = new ConfigPrintRuleDetail();

		CollectionProtocol cp = rule.getCollectionProtocol();
		if (cp != null) {
			detail.setCpId(cp.getId());
			detail.setCpTitle(cp.getTitle());
			detail.setCpShortTitle(cp.getShortTitle());
		}

		detail.setId(rule.getId());
		detail.setObjectType(rule.getObjectType());
		detail.setInstituteId(rule.getInstitute() != null ? rule.getInstitute().getId() : null);
		detail.setInstituteName(rule.getInstitute() != null ? rule.getInstitute().getName() : null);
		detail.setUpdatedBy(UserSummary.from(rule.getUpdatedBy()));
		detail.setUpdatedOn(rule.getUpdatedOn());
		detail.setActivityStatus(rule.getActivityStatus());
		detail.setRules(SpecimenLabelPrintRuleDetail.from(rule.getRules()));
		return detail;
	}

	public static List<ConfigPrintRuleDetail> from(Collection<ConfigPrintRule> rules) {
		return rules.stream().map(ConfigPrintRuleDetail::from).collect(Collectors.toList());
	}
}
