package com.krishagni.catissueplus.core.common.domain.factory.impl;


import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.InstitutePrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.InstitutePrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.InstitutePrintRuleDetail;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class InstitutePrintRuleFactoryImpl implements InstitutePrintRuleFactory {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public InstitutePrintRule createInstitutePrintRule(InstitutePrintRuleDetail detail) {
		InstitutePrintRule rule = new InstitutePrintRule();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		rule.setUpdatedBy(AuthUtil.getCurrentUser());
		rule.setUpdatedOn(Calendar.getInstance().getTime());

		setObjectType(detail, rule);
		setInstitute(detail, rule, ose);
		setCollectionProtocol(detail, rule, ose);
		setStatus(detail, rule);
		setRules(detail, rule);

		ose.checkAndThrow();
		return rule;
	}

	private void setObjectType(InstitutePrintRuleDetail detail, InstitutePrintRule rule) {
		if (StringUtils.isBlank(detail.getObjectType())) {
			return;
		}

		rule.setObjectType(detail.getObjectType());
	}

	private void setInstitute(InstitutePrintRuleDetail detail, InstitutePrintRule rule, OpenSpecimenException ose) {
		if (detail.getInstituteId() == null && StringUtils.isBlank(detail.getInstituteName())) {
			ose.addError(SiteErrorCode.INSTITUTE_REQUIRED);
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

	private void setCollectionProtocol(InstitutePrintRuleDetail detail, InstitutePrintRule rule, OpenSpecimenException ose) {
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
		}

		rule.setCollectionProtocol(cp);
	}

	private void setStatus(InstitutePrintRuleDetail detail, InstitutePrintRule rule) {
		if (detail.getStatus() == null) {
			return;
		}

		rule.setStatus(detail.getStatus());
	}

	private void setRules(InstitutePrintRuleDetail detail, InstitutePrintRule rule) {
		if (detail.getRules().isEmpty()) {
			return;
		}

		rule.setRules(detail.getRules());
	}
}
