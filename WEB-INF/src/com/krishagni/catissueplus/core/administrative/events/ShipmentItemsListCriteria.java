package com.krishagni.catissueplus.core.administrative.events;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ShipmentItemsListCriteria extends AbstractListCriteria<ShipmentItemsListCriteria> {

	private Long shipmentId;

	@Override
	public ShipmentItemsListCriteria self() {
		return this;
	}

	public Long shipmentId() {
		return shipmentId;
	}

	public ShipmentItemsListCriteria shipmentId(Long shipmentId) {
		this.shipmentId = shipmentId;
		return self();
	}
}
