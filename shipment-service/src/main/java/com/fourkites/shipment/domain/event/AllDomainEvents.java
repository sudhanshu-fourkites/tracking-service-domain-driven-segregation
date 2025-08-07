package com.fourkites.shipment.domain.event;

import com.fourkites.shipment.domain.Shipment;
import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.domain.Stop;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
class ShipmentInTransitEvent extends DomainEvent {
    private final String shipmentNumber;
    
    public ShipmentInTransitEvent(Shipment shipment) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
    }
}

@Getter
class ShipmentDeliveredEvent extends DomainEvent {
    private final String shipmentNumber;
    private final LocalDateTime actualDeliveryTime;
    
    public ShipmentDeliveredEvent(Shipment shipment) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.actualDeliveryTime = shipment.getActualDeliveryTime();
    }
}

@Getter
class ShipmentCancelledEvent extends DomainEvent {
    private final String shipmentNumber;
    private final String reason;
    
    public ShipmentCancelledEvent(Shipment shipment, String reason) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.reason = reason;
    }
}

@Getter
class ShipmentETAUpdatedEvent extends DomainEvent {
    private final String shipmentNumber;
    private final LocalDateTime estimatedDeliveryTime;
    
    public ShipmentETAUpdatedEvent(Shipment shipment, LocalDateTime estimatedDeliveryTime) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }
}

@Getter
class StopAddedEvent extends DomainEvent {
    private final String shipmentNumber;
    private final Integer stopSequence;
    
    public StopAddedEvent(Shipment shipment, Stop stop) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.stopSequence = stop.getSequenceNumber();
    }
}

@Getter
class ShipmentStatusChangedEvent extends DomainEvent {
    private final String shipmentNumber;
    private final ShipmentStatus oldStatus;
    private final ShipmentStatus newStatus;
    
    public ShipmentStatusChangedEvent(Shipment shipment, ShipmentStatus oldStatus, ShipmentStatus newStatus) {
        super(shipment.getId().toString());
        this.shipmentNumber = shipment.getShipmentNumber();
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}