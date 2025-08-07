package com.fourkites.shipment.exception;

public class ConcurrentModificationException extends RuntimeException {
    
    public ConcurrentModificationException(String message) {
        super(message);
    }
    
    public ConcurrentModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}