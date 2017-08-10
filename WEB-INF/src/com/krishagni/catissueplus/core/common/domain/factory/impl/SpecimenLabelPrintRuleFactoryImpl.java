package com.krishagni.catissueplus.core.common.domain.factory.impl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ConfigPrintRuleErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class SpecimenLabelPrintRuleFactoryImpl implements LabelPrintRuleFactory {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
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
			ose.addError(ConfigPrintRuleErrorCode.CMD_FILES_DIR_REQUIRED);
			return;
		}

		rule.setCmdFilesDir(input.get("cmdFilesDir"));
	}

	private void setCpShortTitle(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
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

		rule.setCpShortTitle(cp.getShortTitle());
	}

	private void setSpecimenClass(Map<String, String> inputMap, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (!isValidPermissibleValue("specimen_type", inputMap.get("specimenClass")) &&
				StringUtils.isNotBlank(inputMap.get("specimenClass"))) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_CLASS);
			return;
		}

		rule.setSpecimenClass(inputMap.get("specimenClass"));
	}

	private void setSpecimenType(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (!isValidPermissibleValue("specimen_Type", input.get("specimenType")) &&
				StringUtils.isNotBlank(input.get("specimenType"))) {
			ose.addError(SrErrorCode.INVALID_SPECIMEN_TYPE);
			return;
		}

		rule.setSpecimenType(input.get("specimenType"));
	}

	private Boolean isValidPermissibleValue(String attribute, String input) {
		PermissibleValue pvs = daoFactory.getPermissibleValueDao().getByValue(attribute, input);
		return pvs != null ? true : false;
	}
}
