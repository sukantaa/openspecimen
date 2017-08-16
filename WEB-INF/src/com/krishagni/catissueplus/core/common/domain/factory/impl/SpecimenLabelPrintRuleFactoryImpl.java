package com.krishagni.catissueplus.core.common.domain.factory.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule.CmdFileFmt;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.service.PvValidator;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRuleFactoryImpl implements LabelPrintRuleFactory {
	private DaoFactory daoFactory;

	private LabelTmplTokenRegistrar printLabelTokensRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	@Override
	public LabelPrintRule createLabelPrintRule(Map<String, String> def) {
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		setLabelType(def, rule, ose);
		setPrinterName(def, rule, ose);
		setCmdFilesDir(def, rule, ose);
		setCpShortTitle(def, rule, ose);
		setSpecimenClass(def, rule, ose);
		setSpecimenType(def, rule, ose);
		setDataTokens(def, rule, ose);
		setIpAddressMatcher(def, rule, ose);
		setLabelDesign(def, rule, ose);
		setCmdFileFmt(def, rule, ose);
		setLineage(def, rule, ose);
		setVisitSite(def, rule, ose);
		setUserLogin(def, rule, ose);
		ose.checkAndThrow();
		return rule;
	}

	private void setLabelType(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelType"))) {
			return;
		}

		rule.setLabelType(input.get("labelType"));
	}

	private void setPrinterName(Map<String, String> input,SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("printerName"))) {
			return;
		}

		rule.setPrinterName(input.get("printerName"));
	}

	private void setCmdFilesDir(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("cmdFilesDir"))) {
			ose.addError(PrintRuleConfigErrorCode.CMD_FILES_DIR_REQ);
			return;
		}

		rule.setCmdFilesDir(input.get("cmdFilesDir"));
	}

	private void setCpShortTitle(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String cpId = input.get("cpId");
		String cpShortTile = input.get("cpShortTitle");
		String cpTitle = input.get("cpTitle");

		if (StringUtils.isBlank(cpId) && StringUtils.isBlank(cpShortTile) && StringUtils.isBlank(cpTitle)) {
			return;
		}

		CollectionProtocol cp = null;
		Object key = null;

		if (StringUtils.isNotBlank(cpId)) {
			cp = daoFactory.getCollectionProtocolDao().getById(Long.valueOf(cpId).longValue());
			key = cpId;
		} else if (StringUtils.isNotBlank(cpShortTile)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(cpShortTile);
			key = cpShortTile;
		} else if (StringUtils.isNotBlank(cpTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(cpTitle);
			key = cpTitle;
		}

		if (cp == null) {
			if (key != null) {
				ose.addError(CpErrorCode.NOT_FOUND, key);
			}

			return;
		}

		rule.setCpShortTitle(cp.getShortTitle());
	}

	private void setSpecimenClass(Map<String, String> inputMap, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String specimenClass = inputMap.get("specimenClass");
		if (StringUtils.isBlank(specimenClass)) {
			return;
		}

		if (!PvValidator.isValid("specimen_type", specimenClass)) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_CLASS, specimenClass);
			return;
		}

		rule.setSpecimenClass(specimenClass);
	}

	private void setSpecimenType(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String specimenType = input.get("specimenType");
		if (StringUtils.isBlank(specimenType)) {
			return;
		}

		boolean isValid;
		if (StringUtils.isNotBlank(rule.getSpecimenClass())) {
			isValid = PvValidator.isValid("specimen_type", rule.getSpecimenClass(), specimenType);
		} else {
			isValid = PvValidator.isValid("specimen_type", specimenType, true);
		}

		if (!isValid) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, specimenType);
		}

		rule.setSpecimenType(specimenType);
	}

	private void setDataTokens(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("dataTokens"))) {
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKENS_REQ);
			return;
		}

		List<String> tokenNames = Utility.csvToStringList(input.get("dataTokens"));
		List<LabelTmplToken> dataTokens = new ArrayList<>();
		List<String> wrongTokenNames = new ArrayList<>();
		for (String key : tokenNames) {
			LabelTmplToken token = printLabelTokensRegistrar.getToken(key);
			if (token == null) {
				wrongTokenNames.add(key);
			} else {
				dataTokens.add(token);
			}
		}

		if (CollectionUtils.isNotEmpty(wrongTokenNames)) {
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKEN_NOT_FOUND, wrongTokenNames, wrongTokenNames.size());
		}

		rule.setDataTokens(dataTokens);
	}

	private void setIpAddressMatcher(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String ipRange = input.get("ipAddressMatcher");
		if (StringUtils.isBlank(ipRange)) {
			return;
		}

		IpAddressMatcher ipAddressMatcher = null;
		try {
			ipAddressMatcher = new IpAddressMatcher(ipRange);
		} catch (Exception e) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_IP_RANGE, ipRange);
			return;
		}

		rule.setIpAddressMatcher(ipAddressMatcher);
	}

	private void setLabelDesign(Map<String, String> input,SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelDesign"))) {
			return;
		}

		rule.setLabelDesign(input.get("labelDesign"));
	}

	private void setCmdFileFmt(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String cmdFileFmt = input.get("cmdFileFmt");
		if (CmdFileFmt.get(cmdFileFmt) == null) {
			if (StringUtils.isNotBlank(cmdFileFmt)) {
				ose.addError(PrintRuleConfigErrorCode.INVALID_CMD_FILE_FMT, cmdFileFmt);
			}

			return;
		}

		rule.setCmdFileFmt(cmdFileFmt);
	}

	private void setLineage(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String lineage = input.get("lineage");
		if (!Specimen.isValidLineage(lineage)) {
			if (StringUtils.isNotBlank(lineage)) {
				ose.addError(SpecimenErrorCode.INVALID_LINEAGE);
			}

			return;
		}

		rule.setLineage(lineage);
	}

	private void setVisitSite(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String visitSite = input.get("visitSite");

		Site site = daoFactory.getSiteDao().getSiteByName(visitSite);
		if (site == null) {
			if (visitSite != null) {
				ose.addError(SiteErrorCode.NOT_FOUND, visitSite);
			}

			return;
		}

		ensureSiteBelongsToInstitute(site, input.get("instituteName"), ose);
		rule.setVisitSite(site.getName());
	}

	private void setUserLogin(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String userLogin = input.get("userLogin");
		String domainName = input.get("domainName");
		User user = null;

		user = daoFactory.getUserDao().getUser(userLogin, domainName);
		if (user == null) {
			if (StringUtils.isNotBlank(userLogin) | StringUtils.isNotBlank(domainName) | isValidAuthDomain(domainName, ose)) {
				ose.addError(UserErrorCode.NOT_FOUND);
			}

			return;
		}

		rule.setDomainName(domainName);
		rule.setUserLogin(user.getLoginName());
	}

	private Boolean isValidAuthDomain(String domainName, OpenSpecimenException ose) {
		AuthDomain authDomain = daoFactory.getAuthDao().getAuthDomainByName(domainName);
		if (authDomain == null) {
			if (StringUtils.isNotBlank(domainName)) {
				ose.addError(UserErrorCode.DOMAIN_NOT_FOUND);
			}

			return false;
		}

		return true;
	}

	private void ensureSiteBelongsToInstitute(Site site, String instituteName, OpenSpecimenException ose) {
		if (!site.getInstitute().getName().equals(instituteName)) {
			ose.addError(SiteErrorCode.INVALID_SITE_INSTITUTE, site.getName(), instituteName);
		}
	}
}
