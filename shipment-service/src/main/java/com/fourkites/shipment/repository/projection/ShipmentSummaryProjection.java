package com.fourkites.shipment.repository.projection;

import com.fourkites.shipment.domain.ShipmentMode;
import com.fourkites.shipment.domain.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ShipmentSummaryProjection {
    UUID getId();
    String getShipmentNumber();
    String getCustomerId();
    String getCarrierId();
    ShipmentStatus getStatus();
    ShipmentMode getMode();
    String getOriginCity();
    String getOriginState();
    String getDestinationCity();
    String getDestinationState();
    LocalDateTime getPlannedPickupTime();
    LocalDateTime getPlannedDeliveryTime();
    LocalDateTime getEstimatedDeliveryTime();
    LocalDateTime getActualDeliveryTime();
    Integer getStopCount();
    LocalDateTime getUpdatedAt();
}