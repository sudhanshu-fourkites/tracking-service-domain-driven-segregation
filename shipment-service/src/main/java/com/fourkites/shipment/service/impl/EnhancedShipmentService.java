package com.fourkites.shipment.service.impl;

import com.fourkites.shipment.domain.Shipment;
import com.fourkites.shipment.domain.ShipmentState;
import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.dto.ShipmentDTO;
import com.fourkites.shipment.event.DomainEventRecord;
import com.fourkites.shipment.exception.ResourceNotFoundException;
import com.fourkites.shipment.repository.ShipmentRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;

/**
 * Enhanced Shipment Service with Java 21 and Spring Boot 3.4 features
 * Implements virtual threads, structured concurrency, and observability
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedShipmentService {
    
    private final ShipmentRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObservationRegistry observationRegistry;
    
    /**
     * Create shipment with observability
     */
    @Transactional
    @Observed(name = "shipment.create", contextualName = "creating-shipment")
    public ShipmentDTO createShipment(ShipmentDTO dto) {
        return Observation.createNotStarted("shipment.create", observationRegistry)
            .contextualName("creating-shipment")
            .lowCardinalityKeyValue("shipment.mode", dto.getMode().toString())
            .highCardinalityKeyValue("shipment.number", dto.getShipmentNumber())
            .observe(() -> {
                log.info("Creating shipment: {}", dto.getShipmentNumber());
                
                // Create shipment with state
                var shipment = createShipmentEntity(dto);
                var saved = repository.save(shipment);
                
                // Publish event asynchronously
                publishEventAsync(createShipmentCreatedEvent(saved));
                
                return mapToDTO(saved);
            });
    }
    
    /**
     * Get shipment with caching
     */
    @Cacheable(
        value = "shipments",
        key = "#id",
        condition = "#id != null",
        unless = "#result == null"
    )
    @Observed(name = "shipment.get")
    public ShipmentDTO getShipment(UUID id) {
        log.debug("Fetching shipment: {}", id);
        
        return repository.findById(id)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
    }
    
    /**
     * Update shipment status with state validation
     */
    @Transactional
    @CacheEvict(value = "shipments", key = "#id")
    @Observed(name = "shipment.updateStatus")
    public void updateShipmentStatus(UUID id, ShipmentStatus newStatus) {
        var shipment = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
        
        // Use pattern matching for state transition
        var currentState = mapToState(shipment);
        var newState = createState(newStatus);
        
        if (!currentState.canTransitionTo(newState)) {
            throw new IllegalStateException(
                "Invalid transition from %s to %s".formatted(
                    currentState.status(), newState.status()
                )
            );
        }
        
        // Apply state change
        shipment.setStatus(newStatus);
        shipment.setUpdatedAt(LocalDateTime.now());
        repository.save(shipment);
        
        // Publish state change event
        publishEventAsync(createStateChangeEvent(shipment, currentState, newState));
        
        log.info("Shipment {} status updated: {} -> {}", 
                id, currentState.status(), newState.status());
    }
    
    /**
     * Search shipments using Spring Data JPA 3.x features
     */
    public Window<ShipmentDTO> searchShipments(
            String customerId, 
            ScrollPosition position,
            int limit) {
        
        var window = repository.findByCustomerIdOrderByCreatedAtDesc(
            customerId, 
            position, 
            Limit.of(limit)
        );
        
        return window.map(this::mapToDTO);
    }
    
    /**
     * Get comprehensive shipment status using structured concurrency
     */
    public CompletableFuture<ShipmentStatusReport> getComprehensiveStatus(UUID shipmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                // Launch parallel tasks
                var shipmentFuture = scope.fork(() -> getShipment(shipmentId));
                var eventsFuture = scope.fork(() -> getShipmentEvents(shipmentId));
                var stopsFuture = scope.fork(() -> getShipmentStops(shipmentId));
                
                scope.join();           // Wait for all tasks
                scope.throwIfFailed();  // Propagate any errors
                
                // All tasks completed successfully
                return new ShipmentStatusReport(
                    shipmentFuture.resultNow(),
                    eventsFuture.resultNow(),
                    stopsFuture.resultNow()
                );
            } catch (Exception e) {
                log.error("Error getting comprehensive status for shipment {}", shipmentId, e);
                throw new RuntimeException("Failed to get shipment status", e);
            }
        }, Thread.ofVirtual().factory());
    }
    
    /**
     * Process batch of shipments using virtual threads
     */
    public CompletableFuture<List<ShipmentDTO>> processBatch(List<UUID> shipmentIds) {
        var futures = shipmentIds.stream()
            .map(id -> CompletableFuture.supplyAsync(
                () -> processShipment(id),
                Thread.ofVirtual().factory()
            ))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList()
            );
    }
    
    // Helper methods
    
    private ShipmentState mapToState(Shipment shipment) {
        return switch (shipment.getStatus()) {
            case CREATED -> new ShipmentState.Created(shipment.getCreatedAt());
            case CONFIRMED -> new ShipmentState.Confirmed(
                shipment.getUpdatedAt(), "system"
            );
            case DISPATCHED -> new ShipmentState.Dispatched(
                shipment.getActualPickupTime(), "VEHICLE001", "DRIVER001"
            );
            case IN_TRANSIT -> new ShipmentState.InTransit(
                LocalDateTime.now(), 0.0, 0.0
            );
            case DELIVERED -> new ShipmentState.Delivered(
                shipment.getActualDeliveryTime(), "Customer", "signature"
            );
            case CANCELLED -> new ShipmentState.Cancelled(
                shipment.getUpdatedAt(), "User requested", "user"
            );
            case EXCEPTION -> new ShipmentState.Exception(
                shipment.getUpdatedAt(), "DELAY", "Traffic delay"
            );
        };
    }
    
    private ShipmentState createState(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> new ShipmentState.Created(LocalDateTime.now());
            case CONFIRMED -> new ShipmentState.Confirmed(LocalDateTime.now(), "system");
            case DISPATCHED -> new ShipmentState.Dispatched(
                LocalDateTime.now(), "VEHICLE001", "DRIVER001"
            );
            case IN_TRANSIT -> new ShipmentState.InTransit(
                LocalDateTime.now(), 0.0, 0.0
            );
            case DELIVERED -> new ShipmentState.Delivered(
                LocalDateTime.now(), "Customer", "signature"
            );
            case CANCELLED -> new ShipmentState.Cancelled(
                LocalDateTime.now(), "User requested", "user"
            );
            case EXCEPTION -> new ShipmentState.Exception(
                LocalDateTime.now(), "UNKNOWN", "Exception occurred"
            );
        };
    }
    
    private void publishEventAsync(DomainEventRecord event) {
        CompletableFuture.runAsync(() -> {
            var topic = DomainEventRecord.getEventType(event);
            kafkaTemplate.send(topic, event.aggregateId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: {}", event, ex);
                    } else {
                        log.debug("Event published: {}", DomainEventRecord.describeEvent(event));
                    }
                });
        }, Thread.ofVirtual().factory());
    }
    
    private Shipment createShipmentEntity(ShipmentDTO dto) {
        // Implementation
        return new Shipment();
    }
    
    private ShipmentDTO mapToDTO(Shipment shipment) {
        // Implementation
        return new ShipmentDTO();
    }
    
    private DomainEventRecord createShipmentCreatedEvent(Shipment shipment) {
        return new DomainEventRecord.ShipmentCreated(
            null,
            null,
            shipment.getId().toString(),
            shipment.getVersion(),
            shipment.getShipmentNumber(),
            shipment.getCustomerId(),
            shipment.getCarrierId(),
            shipment.getMode().toString(),
            null,
            null
        );
    }
    
    private DomainEventRecord createStateChangeEvent(
            Shipment shipment, 
            ShipmentState from, 
            ShipmentState to) {
        // Implementation based on state transition
        return null;
    }
    
    private ShipmentDTO processShipment(UUID id) {
        // Processing logic
        return getShipment(id);
    }
    
    private List<Object> getShipmentEvents(UUID shipmentId) {
        // Get events for shipment
        return List.of();
    }
    
    private List<Object> getShipmentStops(UUID shipmentId) {
        // Get stops for shipment
        return List.of();
    }
    
    // Result record for comprehensive status
    public record ShipmentStatusReport(
        ShipmentDTO shipment,
        List<Object> events,
        List<Object> stops
    ) {}
}