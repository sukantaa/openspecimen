package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;

public class SpecimenLabelPrintRuleDetail {
	private String labelType;

	private String printerName;

	private String cmdFilesDir;

	private String specimenClass;

	private String specimenType;

	public String getLabelType() {
		return labelType;
	}

	public void setLabelType(String labelType) {
		this.labelType = labelType;
	}

	public String getPrinterName() {
		return printerName;
	}

	public void setPrinterName(String printerName) {
		this.printerName = printerName;
	}

	public String getCmdFilesDir() {
		return cmdFilesDir;
	}

	public void setCmdFilesDir(String cmdFilesDir) {
		this.cmdFilesDir = cmdFilesDir;
	}


	public String getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
	}

	public String getSpecimenType() {
		return specimenType;
	}

	public void setSpecimenType(String specimenType) {
		this.specimenType = specimenType;
	}

	public static SpecimenLabelPrintRuleDetail from(SpecimenLabelPrintRule rule) {
		SpecimenLabelPrintRuleDetail detail = new SpecimenLabelPrintRuleDetail();

		detail.setLabelType(rule.getLabelType());
		detail.setPrinterName(rule.getPrinterName());
		detail.setCmdFilesDir(rule.getCmdFilesDir());
		detail.setSpecimenClass(rule.getSpecimenClass());
		detail.setSpecimenType(rule.getSpecimenType());

		return detail;
	}

	public static List<SpecimenLabelPrintRuleDetail> from(Collection<SpecimenLabelPrintRule> specimenLabelPrintRules) {
		return specimenLabelPrintRules.stream().map(SpecimenLabelPrintRuleDetail::from).collect(Collectors.toList());
	}
}
