package com.fourkites.shipment.exception;

public class ShipmentAlreadyExistsException extends RuntimeException {
    
    public ShipmentAlreadyExistsException(String message) {
        super(message);
    }
    
    public ShipmentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}