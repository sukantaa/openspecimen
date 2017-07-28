package com.krishagni.catissueplus.core.common.domain.factory.impl;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
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

	private LabelTmplTokenRegistrar printLabelTokensRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	@Override
	public ConfigPrintRule createConfigPrintRule(ConfigPrintRuleDetail detail) {
		ConfigPrintRule rule = new ConfigPrintRule();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		rule.setUpdatedBy(AuthUtil.getCurrentUser());
		rule.setUpdatedOn(Calendar.getInstance().getTime());

		setObjectType(detail, rule);
		setInstitute(detail, rule, ose);
		setCollectionProtocol(detail, rule, ose);
		setActivityStatus(detail, rule, ose);
		setRules(detail, rule, ose);

		ose.checkAndThrow();
		return rule;
	}

	private void setObjectType(ConfigPrintRuleDetail detail, ConfigPrintRule rule) {
		if (StringUtils.isBlank(detail.getObjectType())) {
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
		setIpAddressMatcher(detail, rule, ose);
		setPrinterName(detail, rule, ose);
		setCmdFilesDir(detail, rule, ose);
		setLabelDesign(detail, rule, ose);
		setDataTokens(detail, rule, ose);
		setMessageSource(detail, rule, ose);
		setCmdFileFmt(detail, rule, ose);
		setCpShortTitle(detail, rule, ose);
		setVisitSite(detail, rule, ose);
		setSpecimenClass(detail, rule, ose);
		setSpecimenType(detail, rule, ose);
		setLineage(detail, rule, ose);
		return rule;
	}

	private void setLabelType(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getLabelType())) {
			return;
		}

		rule.setLabelType(detail.getLabelType());
	}

	private void setIpAddressMatcher(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getIpAddressMatcher())) {
			return;
		}

		IpAddressMatcher ipAddressMatcher = null;
		try {
			ipAddressMatcher = new IpAddressMatcher(detail.getIpAddressMatcher());
		} catch (Exception e) {
			ose.addError(ConfigPrintRuleErrorCode.IP_RANGE_INVALID, detail.getIpAddressMatcher());
			return;
		}

		rule.setIpAddressMatcher(ipAddressMatcher);
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

	private void setLabelDesign(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getLabelDesign())) {
			return;
		}

		rule.setLabelDesign(detail.getLabelDesign());
	}

	private void setDataTokens(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(detail.getDataTokens())) {
			return;
		}

		List<LabelTmplToken> dataTokens = new ArrayList<>();
		for (String key : detail.getDataTokens()) {
			LabelTmplToken token = printLabelTokensRegistrar.getToken(key);
			if (token == null) {
				ose.addError(ConfigPrintRuleErrorCode.LABEL_TOKEN_NOT_FOUND, key);
			}
			dataTokens.add(token);
		}

		rule.setDataTokens(dataTokens);
	}

	private void setMessageSource(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (detail.getMessageSource() == null) {
			return;
		}

		rule.setMessageSource(detail.getMessageSource());
	}

	private void setCmdFileFmt(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getCmdFileFmt())) {
			return;
		}

		rule.setCmdFileFmt(detail.getCmdFileFmt());
	}

	private void setCpShortTitle(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
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

		rule.setCpShortTitle(cp.getShortTitle());
	}
	private void setVisitSite(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getVisitSite())) {
			return;
		}

		Site site = daoFactory.getSiteDao().getSiteByName(detail.getVisitSite());
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND);
		}
		rule.setVisitSite(detail.getVisitSite());
	}

	private void setSpecimenClass(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (!validPermissibleValue(detail.getSpecimenClass()) && StringUtils.isNotBlank(detail.getSpecimenClass())) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_CLASS);
			return;
		}

		rule.setSpecimenClass(detail.getSpecimenClass());
	}

	private void setSpecimenType(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (!validPermissibleValue(detail.getSpecimenType()) && StringUtils.isNotBlank(detail.getSpecimenType())) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_TYPE);
			return;
		}

		rule.setSpecimenType(detail.getSpecimenType());
	}

	private void setLineage(SpecimenLabelPrintRuleDetail detail, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getLineage())) {
			return;
		}

		if (!Specimen.isValidLineage(detail.getLineage())) {
			ose.addError(SpecimenErrorCode.INVALID_LINEAGE);
			return;
		}

		rule.setLineage(detail.getLineage());
	}

	private Boolean validPermissibleValue(String input) {
		PermissibleValue pvs = daoFactory.getPermissibleValueDao().getByValue("specimen_type", input);
		return pvs != null ? true : false;
	}
}