package com.fourkites.shipment.domain.event;

import com.fourkites.shipment.domain.Shipment;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ShipmentDispatchedEvent extends DomainEvent {
    private final String shipmentNumber;
    private final LocalDateTime actualPickupTime;
    
    public ShipmentDispatchedEvent(Shipment shipment) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.actualPickupTime = shipment.getActualPickupTime();
    }
}