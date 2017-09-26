package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class SpecimenRequest extends BaseExtensionEntity {
	private Long catalogId;

	private String catalogQueryDef;

	private String requestorEmailId;

	private String irbId;

	private Date dateOfRequest;

	private User processedBy;

	private Date dateOfProcessing;

	private DistributionProtocol dp;

	private Set<SpecimenRequestItem> items = new LinkedHashSet<>();

	private String itemsCriteriaJson;

	private String activityStatus;

	private String comments;

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public String getCatalogQueryDef() {
		return catalogQueryDef;
	}

	public void setCatalogQueryDef(String catalogQueryDef) {
		this.catalogQueryDef = catalogQueryDef;
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

	public Date getDateOfRequest() {
		return dateOfRequest;
	}

	public void setDateOfRequest(Date dateOfRequest) {
		this.dateOfRequest = dateOfRequest;
	}

	public User getProcessedBy() {
		return processedBy;
	}

	public void setProcessedBy(User processedBy) {
		this.processedBy = processedBy;
	}

	public Date getDateOfProcessing() {
		return dateOfProcessing;
	}

	public void setDateOfProcessing(Date dateOfProcessing) {
		this.dateOfProcessing = dateOfProcessing;
	}

	public DistributionProtocol getDp() {
		return dp;
	}

	public void setDp(DistributionProtocol dp) {
		this.dp = dp;
	}

	public Set<SpecimenRequestItem> getItems() {
		return items;
	}

	public void setItems(Set<SpecimenRequestItem> items) {
		this.items = items;
	}

	public String getItemsCriteriaJson() {
		return itemsCriteriaJson;
	}

	public void setItemsCriteriaJson(String itemsCriteriaJson) {
		this.itemsCriteriaJson = itemsCriteriaJson;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	@Override
	public String getEntityType() {
		return "SpecimenRequest-" + catalogId;
	}

	public Map<Long, SpecimenRequestItem> getSpecimenIdRequestItemMap() {
		return getItems().stream()
			.collect(Collectors.toMap(item -> item.getSpecimen().getId(), item -> item));
	}

	public Set<Long> getSpecimenIds() {
		return getItems().stream().map(item -> item.getSpecimen().getId()).collect(Collectors.toSet());
	}

	public void closeIfFulfilled() {
		boolean anyPending = getItems().stream().anyMatch(SpecimenRequestItem::isPending);
		if (anyPending) {
			return;
		}

		close("Automatic closure of request");
	}

	public void close(String comments) {
		setProcessedBy(AuthUtil.getCurrentUser());
		setDateOfProcessing(Calendar.getInstance().getTime());
		setComments(comments);
		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public boolean isClosed() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}
}
