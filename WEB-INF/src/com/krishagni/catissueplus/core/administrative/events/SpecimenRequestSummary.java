package com.krishagni.catissueplus.core.administrative.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;

public class SpecimenRequestSummary {
	private Long id;

	private Long catalogId;

	private String requestorEmailId;

	private String irbId;

	private Long dpId;

	private String dpShortTitle;

	private Date dateOfRequest;

	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public String getRequestorEmailId() {
		return requestorEmailId;
	}

	public void setRequestorEmailId(String requestorEmailId) {
		this.requestorEmailId = requestorEmailId;
	}

	public String getIrbId() {
		return irbId;
	}

	public void setIrbId(String irbId) {
		this.irbId = irbId;
	}

	public Long getDpId() {
		return dpId;
	}

	public void setDpId(Long dpId) {
		this.dpId = dpId;
	}

	public String getDpShortTitle() {
		return dpShortTitle;
	}

	public void setDpShortTitle(String dpShortTitle) {
		this.dpShortTitle = dpShortTitle;
	}

	public Date getDateOfRequest() {
		return dateOfRequest;
	}

	public void setDateOfRequest(Date dateOfRequest) {
		this.dateOfRequest = dateOfRequest;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static void copyTo(SpecimenRequest request, SpecimenRequestSummary summary) {
		summary.setId(request.getId());
		summary.setCatalogId(request.getCatalogId());
		summary.setRequestorEmailId(request.getRequestorEmailId());
		summary.setIrbId(request.getIrbId());
		summary.setDateOfRequest(request.getDateOfRequest());
		summary.setActivityStatus(request.getActivityStatus());

		if (request.getDp() != null) {
			summary.setDpId(request.getDp().getId());
			summary.setDpShortTitle(request.getDp().getShortTitle());
		}
	}

	public static SpecimenRequestSummary from(SpecimenRequest request) {
		SpecimenRequestSummary result = new SpecimenRequestSummary();
		copyTo(request, result);
		return result;
	}

	public static List<SpecimenRequestSummary> from(Collection<SpecimenRequest> requests) {
		return requests.stream().map(SpecimenRequestSummary::from).collect(Collectors.toList());
	}
}