package com.krishagni.catissueplus.core.de.events;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ListQueryAuditLogsCriteria extends AbstractListCriteria<ListQueryAuditLogsCriteria> {
	private Long userId;

	private Long instituteId;

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
