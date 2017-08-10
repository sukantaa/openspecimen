package com.krishagni.catissueplus.core.common.domain.factory.impl;


import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRuleFactoryRegistrar;
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

	private LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setLabelPrintRuleFactoryRegistrar(LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar) {
		this.labelPrintRuleFactoryRegistrar = labelPrintRuleFactoryRegistrar;
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

		LabelPrintRule labelPrintRule = labelPrintRuleFactoryRegistrar.getFactory(detail.getObjectType())
				.createLabelPrintRule(detail.getRule());
		rule.setRule(labelPrintRule);
	}
}