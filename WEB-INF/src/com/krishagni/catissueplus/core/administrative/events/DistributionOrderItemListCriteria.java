package com.krishagni.catissueplus.core.administrative.events;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class DistributionOrderItemListCriteria extends AbstractListCriteria<DistributionOrderItemListCriteria> {

	private Long orderId;

	@Override
	public DistributionOrderItemListCriteria self() {
		return this;
	}

	public Long orderId() {
		return orderId;
	}

	public DistributionOrderItemListCriteria orderId(Long orderId) {
		this.orderId = orderId;
		return self();
	}
}
