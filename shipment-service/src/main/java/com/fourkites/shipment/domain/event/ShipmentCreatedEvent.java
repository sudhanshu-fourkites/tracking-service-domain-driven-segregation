package com.fourkites.shipment.domain.event;

import com.fourkites.shipment.domain.Shipment;
import lombok.Getter;
import java.util.UUID;

@Getter
public class ShipmentCreatedEvent extends DomainEvent {
    private final String shipmentNumber;
    private final String customerId;
    private final String carrierId;
    
    public ShipmentCreatedEvent(Shipment shipment) {
        super(shipment.getId() != null ? shipment.getId().toString() : UUID.randomUUID().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.customerId = shipment.getCustomerId();
        this.carrierId = shipment.getCarrierId();
    }
}