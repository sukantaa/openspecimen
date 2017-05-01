package com.krishagni.catissueplus.core.exporter.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;

public class ExportJob extends BaseEntity {
	public enum Status {
		COMPLETED,
		FAILED,
		IN_PROGRESS,
		STOPPED
	}

	private String name;

	private volatile ExportJob.Status status;

	private Long totalRecords = 0L;

	private User createdBy;

	private Date creationTime;

	private Date endTime;

	private Map<String, String> params = new HashMap<>();

	private transient ObjectSchema schema;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExportJob.Status getStatus() {
		return status;
	}

	public void setStatus(ExportJob.Status status) {
		this.status = status;
	}

	public Long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public ObjectSchema getSchema() {
		return schema;
	}

	public void setSchema(ObjectSchema schema) {
		this.schema = schema;
	}

	public ExportJob markCompleted() {
		setStatus(Status.COMPLETED);
		return this;
	}

	public ExportJob markInProgress() {
		setStatus(Status.IN_PROGRESS);
		return this;
	}

	public ExportJob markFailed() {
		setStatus(Status.FAILED);
		return this;
	}
}
