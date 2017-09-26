package com.krishagni.catissueplus.core.administrative.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenRequestListCriteria extends AbstractListCriteria<SpecimenRequestListCriteria> {

	private Long catalogId;

	@Override
	public SpecimenRequestListCriteria self() {
		return this;
	}

	public Long catalogId() {
		return catalogId;
	}

	public SpecimenRequestListCriteria catalogId(Long catalogId) {
		this.catalogId = catalogId;
		return this;
	}
}
