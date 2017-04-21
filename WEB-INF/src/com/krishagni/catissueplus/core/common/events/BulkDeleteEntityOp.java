package com.krishagni.catissueplus.core.common.events;

import java.util.Set;

public class BulkDeleteEntityOp {
	private Set<Long> ids;

	private boolean forceDelete;

	public Set<Long> getIds() {
		return ids;
	}

	public void setIds(Set<Long> ids) {
		this.ids = ids;
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}
}
