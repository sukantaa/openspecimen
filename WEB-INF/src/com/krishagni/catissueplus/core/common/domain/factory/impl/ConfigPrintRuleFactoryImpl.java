package com.krishagni.catissueplus.core.common.domain.factory.impl;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.ConfigPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ConfigPrintRuleErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.SpecimenLabelPrintRuleDetail;
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
		setCollectionProtocol(detail, rule, ose);
		setActivityStatus(detail, rule, ose);
		setRules(detail, rule, ose);

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

	private void setCollectionProtocol(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		CollectionProtocol cp = null;
		Object key = null;

		if (detail.getCpId() != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
			key = detail.getCpId();
		} else if (StringUtils.isNotBlank(detail.getCpTitle())) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(detail.getCpTitle());
			key = detail.getCpTitle();
		} else if (StringUtils.isNotBlank(detail.getCpShortTitle())) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(detail.getCpShortTitle());
			key = detail.getCpShortTitle();
		}

		if (cp == null && key != null) {
			ose.addError(CpErrorCode.NOT_FOUND);
			return;
		}

		rule.setCollectionProtocol(cp);
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

	private void setRules(ConfigPrintRuleDetail detail, ConfigPrintRule rule, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(detail.getRules())) {
			ose.addError(ConfigPrintRuleErrorCode.RULES_REQUIRED);
			return;
		}

		List<SpecimenLabelPrintRule> list = createSpecimenLabelPrintRules(detail.getRules(), ose);
		rule.setRules(list);
	}

	private List<SpecimenLabelPrintRule> createSpecimenLabelPrintRules(List<SpecimenLabelPrintRuleDetail> details, OpenSpecimenException ose) {
		List<SpecimenLabelPrintRule> list = new ArrayList<>();
		for (SpecimenLabelPrintRuleDetail detail : details) {
			list.add(createSpecimenLabelPrintRule(detail, ose));
		}

		return list;
	}

	private SpecimenLabelPrintRule createSpecimenLabelPrintRule(SpecimenLabelPrintRuleDetail detail, OpenSpecimenException ose) {
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();

		setLabelType(detail, rule, ose);
		setPrinterName(detail, rule, ose);
		setCmdFilesDir(detail, rule, ose);
		setSpecimenClass(detail, rule, ose);
		setSpecimenType(detail, rule, ose);

		return rule;
	}

	private void setLabelType(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getLabelType())) {
			return;
		}

		rule.setLabelType(detail.getLabelType());
	}

	private void setPrinterName(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getPrinterName())) {
			return;
		}

		rule.setPrinterName(detail.getPrinterName());
	}

	private void setCmdFilesDir(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getCmdFilesDir())) {
			ose.addError(ConfigPrintRuleErrorCode.CMD_FILES_DIR_REQUIRED);
			return;
		}

		rule.setCmdFilesDir(detail.getCmdFilesDir());
	}

	private void setSpecimenClass(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (!isValidPermissibleValue("specimen_type", detail.getSpecimenClass()) &&
				StringUtils.isNotBlank(detail.getSpecimenClass())) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_CLASS);
			return;
		}

		rule.setSpecimenClass(detail.getSpecimenClass());
	}

	private void setSpecimenType(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (!isValidPermissibleValue("specimen_type", detail.getSpecimenType()) &&
				StringUtils.isNotBlank(detail.getSpecimenType())) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_TYPE);
			return;
		}

		rule.setSpecimenType(detail.getSpecimenType());
	}

	private Boolean isValidPermissibleValue(String attribute, String input) {
		PermissibleValue pvs = daoFactory.getPermissibleValueDao().getByValue(attribute, input);
		return pvs != null ? true : false;
	}
}