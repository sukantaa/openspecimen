package com.krishagni.catissueplus.core.de.events;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ListQueryAuditLogsCriteria extends AbstractListCriteria<ListQueryAuditLogsCriteria> {
	private Long queryId;

	private Long userId;

	private Long instituteId;

	public Long queryId() {
		return queryId;
	}

	public ListQueryAuditLogsCriteria queryId(Long queryId) {
		this.queryId = queryId;
		return self();
	}

	public Long userId() {
		return userId;
	}

	public ListQueryAuditLogsCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public Long instituteId() {
		return instituteId;
	}

	public ListQueryAuditLogsCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}

	@Override
	public ListQueryAuditLogsCriteria self() {
		return this;
	}
}
