package com.fourkites.shipment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base sealed interface for domain events using Java 21 records
 * Ensures type-safe event handling with pattern matching
 */
public sealed interface DomainEventRecord {
    String eventId();
    Instant timestamp();
    String aggregateId();
    Long version();
    
    // Event records implementing the sealed interface
    record ShipmentCreated(
        String eventId,
        Instant timestamp,
        String aggregateId,
        Long version,
        String shipmentNumber,
        String customerId,
        String carrierId,
        String mode,
        AddressData origin,
        AddressData destination
    ) implements DomainEventRecord {
        public ShipmentCreated {
            eventId = eventId != null ? eventId : UUID.randomUUID().toString();
            timestamp = timestamp != null ? timestamp : Instant.now();
        }
    }
    
    record ShipmentDispatched(
        String eventId,
        Instant timestamp,
        String aggregateId,
        Long version,
        String vehicleId,
        String driverId,
        Instant dispatchTime
    ) implements DomainEventRecord {
        public ShipmentDispatched {
            eventId = eventId != null ? eventId : UUID.randomUUID().toString();
            timestamp = timestamp != null ? timestamp : Instant.now();
        }
    }
    
    record ShipmentDelivered(
        String eventId,
        Instant timestamp,
        String aggregateId,
        Long version,
        Instant deliveryTime,
        String receivedBy,
        String signature,
        byte[] proofOfDelivery
    ) implements DomainEventRecord {
        public ShipmentDelivered {
            eventId = eventId != null ? eventId : UUID.randomUUID().toString();
            timestamp = timestamp != null ? timestamp : Instant.now();
        }
    }
    
    record ShipmentCancelled(
        String eventId,
        Instant timestamp,
        String aggregateId,
        Long version,
        String reason,
        String cancelledBy,
        Instant cancellationTime
    ) implements DomainEventRecord {
        public ShipmentCancelled {
            eventId = eventId != null ? eventId : UUID.randomUUID().toString();
            timestamp = timestamp != null ? timestamp : Instant.now();
        }
    }
    
    record ShipmentException(
        String eventId,
        Instant timestamp,
        String aggregateId,
        Long version,
        String exceptionType,
        String description,
        String severity,
        String reportedBy
    ) implements DomainEventRecord {
        public ShipmentException {
            eventId = eventId != null ? eventId : UUID.randomUUID().toString();
            timestamp = timestamp != null ? timestamp : Instant.now();
        }
    }
    
    record StopVisited(
        String eventId,
        Instant timestamp,
        String aggregateId,
        Long version,
        String stopId,
        Instant arrivalTime,
        Instant departureTime,
        String stopType
    ) implements DomainEventRecord {
        public StopVisited {
            eventId = eventId != null ? eventId : UUID.randomUUID().toString();
            timestamp = timestamp != null ? timestamp : Instant.now();
        }
    }
    
    // Value record for address data in events
    record AddressData(
        String city,
        String state,
        String country,
        String zipCode,
        Double latitude,
        Double longitude
    ) {}
    
    // Pattern matching helper for event processing
    static String describeEvent(DomainEventRecord event) {
        return switch (event) {
            case ShipmentCreated e -> "Shipment %s created for customer %s".formatted(
                e.shipmentNumber(), e.customerId()
            );
            case ShipmentDispatched e -> "Shipment dispatched with vehicle %s at %s".formatted(
                e.vehicleId(), e.dispatchTime()
            );
            case ShipmentDelivered e -> "Shipment delivered to %s at %s".formatted(
                e.receivedBy(), e.deliveryTime()
            );
            case ShipmentCancelled e -> "Shipment cancelled by %s: %s".formatted(
                e.cancelledBy(), e.reason()
            );
            case ShipmentException e -> "Exception [%s]: %s".formatted(
                e.exceptionType(), e.description()
            );
            case StopVisited e -> "Stop %s visited at %s".formatted(
                e.stopId(), e.arrivalTime()
            );
        };
    }
    
    // Get event type for routing
    static String getEventType(DomainEventRecord event) {
        return switch (event) {
            case ShipmentCreated _ -> "shipment.created";
            case ShipmentDispatched _ -> "shipment.dispatched";
            case ShipmentDelivered _ -> "shipment.delivered";
            case ShipmentCancelled _ -> "shipment.cancelled";
            case ShipmentException _ -> "shipment.exception";
            case StopVisited _ -> "stop.visited";
        };
    }
}