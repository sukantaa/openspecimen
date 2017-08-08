package com.krishagni.catissueplus.core.common.domain.factory.impl;


import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.ConfigPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ConfigPrintRuleErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class ConfigPrintRuleFactoryImpl implements ConfigPrintRuleFactory {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ConfigPrintRule createConfigPrintRule(ConfigPrintRuleDetail detail) {
		ConfigPrintRule rule = new ConfigPrintRule();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		rule.setUpdatedBy(AuthUtil.getCurrentUser());
		rule.setUpdatedOn(Calendar.getInstance().getTime());

		setObjectType(detail, rule, ose);
		setInstitute(detail, rule, ose);
		setActivityStatus(detail, rule, ose);
		setRule(detail, rule, ose);

		ose.checkAndThrow();
		return rule;
	}

	private void setObjectType(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getObjectType())) {
			ose.addError(ConfigPrintRuleErrorCode.OBJECT_TYPE_REQUIRED);
			return;
		}

		rule.setObjectType(detail.getObjectType());
	}

	private void setInstitute(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		if (detail.getInstituteId() == null && StringUtils.isBlank(detail.getInstituteName())) {
			return;
		}

		Institute institute = null;
		Object key = null;
		if (detail.getInstituteId() != null) {
			institute = daoFactory.getInstituteDao().getById(detail.getInstituteId());
			key = detail.getInstituteId();
		} else if (StringUtils.isNotBlank(detail.getInstituteName())) {
			institute = daoFactory.getInstituteDao().getInstituteByName(detail.getInstituteName());
			key = detail.getInstituteName();
		}

		if (institute == null) {
			ose.addError(InstituteErrorCode.NOT_FOUND, key);
			return;
		}
		rule.setInstitute(institute);
	}

	private void setActivityStatus(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			rule.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}

		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}

		rule.setActivityStatus(activityStatus);
	}

	private void setRule(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		if (detail.getRule() == null) {
			ose.addError(ConfigPrintRuleErrorCode.RULES_REQUIRED);
			return;
		}

		if (detail.getObjectType().equals("SPECIMEN")) {
			setSpecimenLabelPrintRule(detail, rule, ose);
		} else if(detail.getObjectType().equals("VISIT")) {
			setVisitPrintRule(detail, rule, ose);
		}
	}

	private void setSpecimenLabelPrintRule(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		Map<String, String> ruleMap = new HashMap<String, String>();

		setLabelType(detail.getRule(), ruleMap, ose);
		setPrinterName(detail.getRule(), ruleMap, ose);
		setCmdFilesDir(detail.getRule(), ruleMap, ose);
		setCpShortTitle(detail.getRule(), ruleMap, ose);
		setSpecimenClass(detail.getRule(), ruleMap, ose);
		setSpecimenType(detail.getRule(), ruleMap, ose);

		rule.setRule(ruleMap);
	}

	private void setVisitPrintRule(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		Map<String, String> ruleMap = new HashMap<String, String>();

		setLabelType(detail.getRule(), ruleMap, ose);
		setPrinterName(detail.getRule(), ruleMap, ose);
		setCmdFilesDir(detail.getRule(), ruleMap, ose);
		setCpShortTitle(detail.getRule(), ruleMap, ose);

		rule.setRule(ruleMap);
	}

	private void setLabelType(Map<String, String> input, Map<String, String> rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelType"))) {
			return;
		}

		rule.put("labelType", input.get("labelType"));
	}

	private void setPrinterName(Map<String, String> input, Map<String, String> rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("printerName"))) {
			return;
		}

		rule.put("printerName", input.get("printerName"));
	}

	private void setCmdFilesDir(Map<String, String> input, Map<String, String> rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("cmdFilesDir"))) {
			ose.addError(ConfigPrintRuleErrorCode.CMD_FILES_DIR_REQUIRED);
			return;
		}

		rule.put("cmdFilesDir", input.get("cmdFilesDir"));
	}

	private void setCpShortTitle(Map<String, String> input, Map<String, String> rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("cpId")) && StringUtils.isBlank(input.get("cpTitle"))
				&& StringUtils.isBlank(input.get("cpShortTitle"))) {
			return;
		}
		CollectionProtocol cp = null;
		Object key = null;

		if (input.get("cpId") != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(Long.valueOf(input.get("cpId")).longValue());
			key = input.get("cpId");
		} else if (StringUtils.isNotBlank(input.get("cpTitle"))) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(input.get("cpTitle"));
			key = input.get("cpTitle");
		} else if (StringUtils.isNotBlank(input.get("cpShortTitle"))) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(input.get("cpShortTitle"));
			key = input.get("cpShortTitle");
		}

		if (cp == null && key != null) {
			ose.addError(CpErrorCode.NOT_FOUND);
			return;
		}

		rule.put("cpShortTitle", cp.getShortTitle());
	}

	private void setSpecimenClass(Map<String, String> inputMap, Map<String, String> ruleMap, OpenSpecimenException ose) {
		if (!isValidPermissibleValue("specimen_type", inputMap.get("specimenClass")) &&
				StringUtils.isNotBlank(inputMap.get("specimenClass"))) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_CLASS);
			return;
		}

		ruleMap.put("specimenClass", inputMap.get("specimenClass"));
	}

	private void setSpecimenType(Map<String, String> input, Map<String, String> rule, OpenSpecimenException ose) {
		if (!isValidPermissibleValue("specimen_Type", input.get("specimenType")) &&
				StringUtils.isNotBlank(input.get("specimenType"))) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_TYPE);
			return;
		}

		rule.put("specimenType", input.get("specimenType"));
	}

	private Boolean isValidPermissibleValue(String attribute, String input) {
		PermissibleValue pvs = daoFactory.getPermissibleValueDao().getByValue(attribute, input);
		return pvs != null ? true : false;
	}
}