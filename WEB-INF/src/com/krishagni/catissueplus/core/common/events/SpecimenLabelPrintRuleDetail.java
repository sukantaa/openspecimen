package com.krishagni.catissueplus.core.common.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.MessageSource;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class SpecimenLabelPrintRuleDetail {
	private String labelType;

	private String ipAddressMatcher;

	private String printerName;

	private String cmdFilesDir;

	private String labelDesign;

	private List<String> dataTokens;

	private MessageSource messageSource;

	private String cmdFileFmt;

	private Long cpId;

	private String cpTitle;

	private String cpShortTitle;

	private String visitSite;

	private String specimenClass;

	private String specimenType;

	private String lineage;

	public String getLabelType() {
		return labelType;
	}

	public void setLabelType(String labelType) {
		this.labelType = labelType;
	}

	public String getIpAddressMatcher() {
		return ipAddressMatcher;
	}

	public void setIpAddressMatcher(String ipAddressMatcher) {
		this.ipAddressMatcher = ipAddressMatcher;
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

	public String getLabelDesign() {
		return labelDesign;
	}

	public void setLabelDesign(String labelDesign) {
		this.labelDesign = labelDesign;
	}

	public List<String> getDataTokens() {
		return dataTokens;
	}

	public void setDataTokens(List<String> dataTokens) {
		this.dataTokens = dataTokens;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getCmdFileFmt() {
		return cmdFileFmt;
	}

	public void setCmdFileFmt(String cmdFileFmt) {
		this.cmdFileFmt = cmdFileFmt;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
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

	public String getVisitSite() {
		return visitSite;
	}

	public void setVisitSite(String visitSite) {
		this.visitSite = visitSite;
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

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	public static SpecimenLabelPrintRuleDetail from(SpecimenLabelPrintRule rule) {
		SpecimenLabelPrintRuleDetail detail = new SpecimenLabelPrintRuleDetail();

		detail.setLabelType(rule.getLabelType());
		detail.setIpAddressMatcher(rule.getIpAddressMatcher() != null ? getIpAddress(rule.getIpAddressMatcher()) : null);
		detail.setPrinterName(rule.getPrinterName());
		detail.setCmdFilesDir(rule.getCmdFilesDir());
		detail.setLabelDesign(rule.getLabelDesign());
		detail.setDataTokens(CollectionUtils.isNotEmpty(rule.getDataTokens()) ? getTokenKeys(rule.getDataTokens()) :null);
		detail.setMessageSource(rule.getMessageSource());
		detail.setCmdFileFmt(rule.getCmdFileFmt() != null ? rule.getCmdFileFmt().toString() : null);
		detail.setCpShortTitle(rule.getCpShortTitle());
		detail.setVisitSite(rule.getVisitSite());
		detail.setSpecimenClass(rule.getSpecimenClass());
		detail.setSpecimenType(rule.getSpecimenType());
		detail.setLineage(rule.getLineage());

		return detail;
	}

	public static List<SpecimenLabelPrintRuleDetail> from(Collection<SpecimenLabelPrintRule> specimenLabelPrintRules) {
		return specimenLabelPrintRules.stream().map(SpecimenLabelPrintRuleDetail::from).collect(Collectors.toList());
	}

	private static List<String> getTokenKeys(List<LabelTmplToken> dataTokens) {
		List<String> keys = new ArrayList<>();
		for (LabelTmplToken token: dataTokens) {
			keys.add(token.getName());
		}

		return keys;
	}

	private static String getIpAddress(IpAddressMatcher ipAddressMatcher2) {
		String input;
		Map<String, String> map = new HashMap<String, String>();
		try {
			input = getWriteMapper().writeValueAsString(ipAddressMatcher2);
			map = getReadMapper().readValue(input, new TypeReference<Map<String, String>>(){});
		} catch (Exception e) {
			throw new RuntimeException("Error marshalling Ip Address to JSON", e);
		}

		String ipAddress = map.get("requiredAddress") + "/" + map.get("nMaskBits");
		return ipAddress;
	}

	private static ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}

	private static ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setVisibilityChecker(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE)
				.withCreatorVisibility(Visibility.NONE));
		return mapper;
	}
}
