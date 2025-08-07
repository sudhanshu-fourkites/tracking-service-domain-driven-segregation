package com.fourkites.shipment.domain;

import java.time.LocalDateTime;

/**
 * Sealed hierarchy for shipment states using Java 21 features
 * Implements state pattern with type safety
 */
public sealed interface ShipmentState 
    permits ShipmentState.Created, ShipmentState.Confirmed, ShipmentState.Dispatched,
            ShipmentState.InTransit, ShipmentState.Delivered, ShipmentState.Cancelled,
            ShipmentState.Exception {
    
    ShipmentStatus status();
    LocalDateTime timestamp();
    boolean canTransitionTo(ShipmentState newState);
    String describe();
    
    // State implementations as records
    record Created(LocalDateTime timestamp) implements ShipmentState {
        public Created {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.CREATED;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return switch (newState) {
                case Confirmed _, Cancelled _ -> true;
                default -> false;
            };
        }
        
        @Override
        public String describe() {
            return "Shipment created and awaiting confirmation";
        }
    }
    
    record Confirmed(LocalDateTime timestamp, String confirmedBy) implements ShipmentState {
        public Confirmed {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.CONFIRMED;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return switch (newState) {
                case Dispatched _, Cancelled _ -> true;
                default -> false;
            };
        }
        
        @Override
        public String describe() {
            return "Shipment confirmed and ready for dispatch";
        }
    }
    
    record Dispatched(LocalDateTime timestamp, String vehicleId, String driverId) implements ShipmentState {
        public Dispatched {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.DISPATCHED;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return switch (newState) {
                case InTransit _, Cancelled _ -> true;
                default -> false;
            };
        }
        
        @Override
        public String describe() {
            return "Shipment dispatched with vehicle " + vehicleId;
        }
    }
    
    record InTransit(LocalDateTime timestamp, Double currentLatitude, Double currentLongitude) implements ShipmentState {
        public InTransit {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.IN_TRANSIT;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return switch (newState) {
                case Delivered _, Cancelled _, Exception _ -> true;
                default -> false;
            };
        }
        
        @Override
        public String describe() {
            return "Shipment in transit at location (" + currentLatitude + ", " + currentLongitude + ")";
        }
    }
    
    record Delivered(LocalDateTime timestamp, String receivedBy, String signature) implements ShipmentState {
        public Delivered {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.DELIVERED;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return false; // Terminal state
        }
        
        @Override
        public String describe() {
            return "Shipment delivered to " + receivedBy;
        }
    }
    
    record Cancelled(LocalDateTime timestamp, String reason, String cancelledBy) implements ShipmentState {
        public Cancelled {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.CANCELLED;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return false; // Terminal state
        }
        
        @Override
        public String describe() {
            return "Shipment cancelled: " + reason;
        }
    }
    
    record Exception(LocalDateTime timestamp, String exceptionType, String description) implements ShipmentState {
        public Exception {
            timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        }
        
        @Override
        public ShipmentStatus status() {
            return ShipmentStatus.EXCEPTION;
        }
        
        @Override
        public boolean canTransitionTo(ShipmentState newState) {
            return switch (newState) {
                case InTransit _, Cancelled _ -> true;
                default -> false;
            };
        }
        
        @Override
        public String describe() {
            return "Shipment exception: " + exceptionType + " - " + description;
        }
    }
    
    // Pattern matching helper for state transitions
    static String describeTransition(ShipmentState from, ShipmentState to) {
        return switch (from, to) {
            case (Created _, Confirmed c) -> "Shipment confirmed by " + c.confirmedBy();
            case (Confirmed _, Dispatched d) -> "Shipment dispatched with vehicle " + d.vehicleId();
            case (Dispatched _, InTransit _) -> "Shipment started transit";
            case (InTransit _, Delivered d) -> "Shipment delivered to " + d.receivedBy();
            case (_, Cancelled c) -> "Shipment cancelled: " + c.reason();
            case (InTransit _, Exception e) -> "Exception occurred: " + e.exceptionType();
            case (Exception _, InTransit _) -> "Exception resolved, shipment back in transit";
            default -> "Invalid transition from " + from.status() + " to " + to.status();
        };
    }
}