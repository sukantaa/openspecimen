package com.krishagni.catissueplus.core.common.domain.factory.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.service.PvValidator;

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

		Set<String> tokenNames = new HashSet<String>(Arrays.asList(input.get("dataTokens").split(",")));
		List<LabelTmplToken> dataTokens = new ArrayList<>();
		for (String key : tokenNames) {
			LabelTmplToken token = printLabelTokensRegistrar.getToken(key);
			if (token != null) {
				dataTokens.add(token);
			}
		}

		if (tokenNames.size() != dataTokens.size()) {
			dataTokens.forEach(token -> tokenNames.remove(token.getName()));
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKEN_NOT_FOUND, tokenNames, tokenNames.size());
		}

		rule.setDataTokens(dataTokens);
	}
}
