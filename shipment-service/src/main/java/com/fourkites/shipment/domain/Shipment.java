package com.fourkites.shipment.domain;

import com.fourkites.shipment.domain.event.*;
import com.fourkites.shipment.exception.InvalidShipmentStateException;
import com.fourkites.shipment.exception.InvalidStateTransitionException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipment_number", columnList = "shipmentNumber"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_carrier_id", columnList = "carrierId"),
    @Index(name = "idx_customer_id", columnList = "customerId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"stops", "events"})
@EntityListeners(AuditingEntityListener.class)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String shipmentNumber;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String carrierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentMode mode;

    @Embedded
    private Address origin;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "addressLine1", column = @Column(name = "dest_address_line1")),
        @AttributeOverride(name = "addressLine2", column = @Column(name = "dest_address_line2")),
        @AttributeOverride(name = "city", column = @Column(name = "dest_city")),
        @AttributeOverride(name = "state", column = @Column(name = "dest_state")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "dest_zip_code")),
        @AttributeOverride(name = "country", column = @Column(name = "dest_country")),
        @AttributeOverride(name = "latitude", column = @Column(name = "dest_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "dest_longitude"))
    })
    private Address destination;

    private LocalDateTime plannedPickupTime;
    private LocalDateTime actualPickupTime;
    private LocalDateTime plannedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private LocalDateTime estimatedDeliveryTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(precision = 10, scale = 2)
    private BigDecimal volume;

    private Integer pieceCount;
    private String commodityDescription;
    private String referenceNumber;
    private String poNumber;
    private String billOfLading;

    @Column(precision = 10, scale = 2)
    private BigDecimal declaredValue;

    private String specialInstructions;
    private Boolean temperatureControlled;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
    private Boolean hazmat;
    private String hazmatClass;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Stop> stops = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShipmentEvent> events = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "shipment_tags", joinColumns = @JoinColumn(name = "shipment_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
    
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    // Domain behavior methods
    
    public static Shipment create(String shipmentNumber, String customerId, String carrierId,
                                  ShipmentMode mode, Address origin, Address destination,
                                  LocalDateTime plannedPickupTime, LocalDateTime plannedDeliveryTime) {
        // Validate invariants
        if (plannedDeliveryTime.isBefore(plannedPickupTime)) {
            throw new IllegalArgumentException("Delivery time must be after pickup time");
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("Origin and destination cannot be the same");
        }
        
        Shipment shipment = new Shipment();
        shipment.shipmentNumber = shipmentNumber;
        shipment.customerId = customerId;
        shipment.carrierId = carrierId;
        shipment.status = ShipmentStatus.CREATED;
        shipment.mode = mode;
        shipment.origin = origin;
        shipment.destination = destination;
        shipment.plannedPickupTime = plannedPickupTime;
        shipment.plannedDeliveryTime = plannedDeliveryTime;
        shipment.createdAt = LocalDateTime.now();
        shipment.updatedAt = LocalDateTime.now();
        
        shipment.addDomainEvent(new ShipmentCreatedEvent(shipment));
        return shipment;
    }
    
    public void dispatch() {
        validateCanDispatch();
        this.status = ShipmentStatus.DISPATCHED;
        this.actualPickupTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new ShipmentDispatchedEvent(this));
    }
    
    public void startTransit() {
        if (this.status != ShipmentStatus.DISPATCHED) {
            throw new InvalidShipmentStateException("Can only start transit for dispatched shipments");
        }
        this.status = ShipmentStatus.IN_TRANSIT;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new ShipmentInTransitEvent(this));
    }
    
    public void deliver(LocalDateTime deliveryTime) {
        if (this.status != ShipmentStatus.IN_TRANSIT) {
            throw new InvalidShipmentStateException("Can only deliver shipments in transit");
        }
        if (deliveryTime.isBefore(this.actualPickupTime)) {
            throw new IllegalArgumentException("Delivery time cannot be before pickup time");
        }
        this.status = ShipmentStatus.DELIVERED;
        this.actualDeliveryTime = deliveryTime;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new ShipmentDeliveredEvent(this));
    }
    
    public void cancel(String reason) {
        if (!canBeCancelled()) {
            throw new InvalidShipmentStateException("Cannot cancel shipment in status: " + status);
        }
        this.status = ShipmentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        
        ShipmentEvent cancellationEvent = ShipmentEvent.builder()
            .shipment(this)
            .eventType("CANCELLED")
            .eventTimestamp(LocalDateTime.now())
            .description("Shipment cancelled: " + reason)
            .build();
        this.addEvent(cancellationEvent);
        
        addDomainEvent(new ShipmentCancelledEvent(this, reason));
    }
    
    public void updateEstimatedDelivery(LocalDateTime estimatedDeliveryTime) {
        if (this.status == ShipmentStatus.DELIVERED || this.status == ShipmentStatus.CANCELLED) {
            throw new InvalidShipmentStateException("Cannot update ETA for " + status + " shipment");
        }
        if (estimatedDeliveryTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Estimated delivery cannot be in the past");
        }
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new ShipmentETAUpdatedEvent(this, estimatedDeliveryTime));
    }
    
    public void addStop(Stop stop) {
        // Validate stop can be added
        if (this.status == ShipmentStatus.DELIVERED || this.status == ShipmentStatus.CANCELLED) {
            throw new InvalidShipmentStateException("Cannot add stops to " + status + " shipment");
        }
        
        // Ensure unique sequence numbers
        if (stops.stream().anyMatch(s -> s.getSequenceNumber().equals(stop.getSequenceNumber()))) {
            throw new IllegalArgumentException("Stop with sequence " + stop.getSequenceNumber() + " already exists");
        }
        
        stops.add(stop);
        stop.setShipment(this);
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new StopAddedEvent(this, stop));
    }

    public void addEvent(ShipmentEvent event) {
        events.add(event);
        event.setShipment(this);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void transitionTo(ShipmentStatus newStatus) {
        if (!isValidTransition(this.status, newStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition from %s to %s", this.status, newStatus)
            );
        }
        ShipmentStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new ShipmentStatusChangedEvent(this, oldStatus, newStatus));
    }
    
    // Validation methods
    
    private void validateCanDispatch() {
        if (this.status != ShipmentStatus.CREATED && this.status != ShipmentStatus.CONFIRMED) {
            throw new InvalidShipmentStateException("Can only dispatch created or confirmed shipments");
        }
        if (this.stops.isEmpty()) {
            throw new IllegalStateException("Cannot dispatch shipment without stops");
        }
    }
    
    private boolean canBeCancelled() {
        return status != ShipmentStatus.DELIVERED && status != ShipmentStatus.CANCELLED;
    }
    
    private boolean isValidTransition(ShipmentStatus from, ShipmentStatus to) {
        return switch (from) {
            case CREATED -> to == ShipmentStatus.CONFIRMED || to == ShipmentStatus.CANCELLED;
            case CONFIRMED -> to == ShipmentStatus.DISPATCHED || to == ShipmentStatus.CANCELLED;
            case DISPATCHED -> to == ShipmentStatus.IN_TRANSIT || to == ShipmentStatus.CANCELLED;
            case IN_TRANSIT -> to == ShipmentStatus.DELIVERED || to == ShipmentStatus.CANCELLED 
                           || to == ShipmentStatus.EXCEPTION;
            case EXCEPTION -> to == ShipmentStatus.IN_TRANSIT || to == ShipmentStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
            default -> false;
        };
    }
    
    // Domain events handling
    
    protected void addDomainEvent(DomainEvent event) {
        if (domainEvents == null) {
            domainEvents = new ArrayList<>();
        }
        domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return domainEvents != null ? new ArrayList<>(domainEvents) : new ArrayList<>();
    }
    
    public void clearDomainEvents() {
        if (domainEvents != null) {
            domainEvents.clear();
        }
    }
}