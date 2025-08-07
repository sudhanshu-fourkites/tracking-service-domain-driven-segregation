package com.fourkites.shipment.domain;

public enum ShipmentStatus {
    CREATED,
    CONFIRMED,
    DISPATCHED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    EXCEPTION,
    ON_HOLD,
    RETURNED
}