package com.krishagni.catissueplus.core.administrative.domain;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenShipmentReceivedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenShipmentShippedEvent;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

@Audited
public class ShipmentSpecimen extends BaseEntity {
	private Shipment shipment;
	
	private Specimen specimen;

	private ShipmentContainer shipmentContainer;
	
	private Shipment.ItemReceiveQuality receivedQuality;

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public ShipmentContainer getShipmentContainer() {
		return shipmentContainer;
	}

	public void setShipmentContainer(ShipmentContainer shipmentContainer) {
		this.shipmentContainer = shipmentContainer;
	}

	public Shipment.ItemReceiveQuality getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(Shipment.ItemReceiveQuality receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public void ship() {
		Shipment shipment = getShipment();
		if (shipment.isSpecimenShipment()) {
			StorageContainerPosition position = new StorageContainerPosition();
			position.setContainer(shipment.getReceivingSite().getContainer());
			position.setOccupyingSpecimen(getSpecimen());
			getSpecimen().updatePosition(position, shipment.getShippedDate());
		}

		shipment.addOnSaveProc(() -> addShippedEvent(this));
	}
	
	public void receive(ShipmentSpecimen other) {
		setReceivedQuality(other.getReceivedQuality());
		updatePosition(other);
		SpecimenShipmentReceivedEvent.createForShipmentItem(this).saveRecordEntry();
	}

	public void receive(Shipment.ItemReceiveQuality receivedQuality) {
		setReceivedQuality(receivedQuality);
		SpecimenShipmentReceivedEvent.createForShipmentItem(this).saveRecordEntry();
	}

	public static ShipmentSpecimen createShipmentSpecimen(Shipment shipment, Specimen specimen) {
		ShipmentSpecimen shipmentSpmn = new ShipmentSpecimen();
		shipmentSpmn.setShipment(shipment);
		shipmentSpmn.setSpecimen(specimen);
		return shipmentSpmn;
	}

	private void addShippedEvent(ShipmentSpecimen item) {
		SpecimenShipmentShippedEvent.createForShipmentSpecimen(item).saveRecordEntry();
	}

	private void updatePosition(ShipmentSpecimen other) {
		if (!getShipment().isSpecimenShipment()) {
			return;
		}

		StorageContainerPosition position = other.getSpecimen().getPosition();
		if (getReceivedQuality() == Shipment.ItemReceiveQuality.ACCEPTABLE && position != null) {
			StorageContainer container = position.getContainer();
			if (container.isPositionOccupied(position.getPosOne(), position.getPosTwo())) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
			}

			getSpecimen().updatePosition(position, getShipment().getReceivedDate());
		}
	}
}
