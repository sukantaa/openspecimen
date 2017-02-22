package com.krishagni.catissueplus.core.de.events;

import java.util.List;

public class GetFieldValuesOp {
	private Long cpId;

	private List<String> fields;

	private String searchTerm;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> facets) {
		this.fields = facets;
	}

	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}
}
