# DDD Implementation Guide
## Practical Implementation of Domain-Driven Design Concepts

## 1. Tactical DDD Patterns - Code Examples

### 1.1 Aggregate Root Implementation

```java
package com.fourkites.shipment.domain;

@Entity
@Table(name = "shipments")
public class Shipment extends BaseAggregateRoot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String shipmentNumber;
    
    @Embedded
    private ShipmentDetails details;
    
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shipment_id")
    private List<Stop> stops = new ArrayList<>();
    
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Private constructor for JPA
    protected Shipment() {}
    
    // Factory method for creation
    public static Shipment create(CreateShipmentCommand command) {
        // Enforce invariants
        validateShipmentCreation(command);
        
        Shipment shipment = new Shipment();
        shipment.shipmentNumber = generateShipmentNumber();
        shipment.details = ShipmentDetails.from(command);
        shipment.status = ShipmentStatus.CREATED;
        
        // Raise domain event
        shipment.addDomainEvent(new ShipmentCreatedEvent(
            shipment.id,
            shipment.shipmentNumber,
            command.getCustomerId(),
            command.getCarrierId()
        ));
        
        return shipment;
    }
    
    // Business method with invariant enforcement
    public void addStop(Stop stop) {
        // Invariant: Cannot add stops to delivered shipments
        if (this.status == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException(
                "Cannot add stops to delivered shipment"
            );
        }
        
        // Invariant: Stop sequence must be unique
        if (stops.stream().anyMatch(s -> 
            s.getSequenceNumber().equals(stop.getSequenceNumber()))) {
            throw new DomainException(
                "Stop with sequence " + stop.getSequenceNumber() + " already exists"
            );
        }
        
        stops.add(stop);
        stop.setShipment(this);
        
        addDomainEvent(new StopAddedEvent(this.id, stop.getId()));
    }
    
    // State transition with validation
    public void transitionTo(ShipmentStatus newStatus) {
        if (!isValidTransition(this.status, newStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition from %s to %s", 
                    this.status, newStatus)
            );
        }
        
        ShipmentStatus oldStatus = this.status;
        this.status = newStatus;
        
        addDomainEvent(new ShipmentStatusChangedEvent(
            this.id, oldStatus, newStatus
        ));
    }
    
    private boolean isValidTransition(ShipmentStatus from, ShipmentStatus to) {
        return switch (from) {
            case CREATED -> to == ShipmentStatus.CONFIRMED 
                        || to == ShipmentStatus.CANCELLED;
            case CONFIRMED -> to == ShipmentStatus.DISPATCHED 
                         || to == ShipmentStatus.CANCELLED;
            case DISPATCHED -> to == ShipmentStatus.IN_TRANSIT 
                          || to == ShipmentStatus.CANCELLED;
            case IN_TRANSIT -> to == ShipmentStatus.DELIVERED 
                          || to == ShipmentStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
    
    // Domain events handling
    protected void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    @DomainEvents
    public Collection<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    @AfterDomainEventPublication
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

### 1.2 Value Object Implementation

```java
package com.fourkites.shipment.domain;

@Embeddable
public final class Address {
    
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;
    
    @Column(name = "address_line_2", length = 255)
    private String addressLine2;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 50)
    private String state;
    
    @Column(name = "zip_code", length = 20)
    private String zipCode;
    
    @Column(name = "country", length = 50)
    private String country;
    
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
    
    // Private constructor for JPA
    protected Address() {}
    
    // Factory method with validation
    public static Address of(String addressLine1, String city, 
                            String state, String zipCode, String country,
                            BigDecimal latitude, BigDecimal longitude) {
        // Validate required fields
        Objects.requireNonNull(addressLine1, "Address line 1 is required");
        Objects.requireNonNull(city, "City is required");
        Objects.requireNonNull(state, "State is required");
        Objects.requireNonNull(country, "Country is required");
        
        // Validate coordinates
        if (latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 
            || latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new IllegalArgumentException("Invalid latitude");
        }
        
        if (longitude != null && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 
            || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new IllegalArgumentException("Invalid longitude");
        }
        
        Address address = new Address();
        address.addressLine1 = addressLine1;
        address.city = city;
        address.state = state;
        address.zipCode = zipCode;
        address.country = country;
        address.latitude = latitude;
        address.longitude = longitude;
        
        return address;
    }
    
    // Business method
    public double distanceTo(Address other) {
        if (this.latitude == null || this.longitude == null 
            || other.latitude == null || other.longitude == null) {
            throw new IllegalStateException("Coordinates required for distance calculation");
        }
        
        // Haversine formula
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(other.latitude.doubleValue() - this.latitude.doubleValue());
        double dLon = Math.toRadians(other.longitude.doubleValue() - this.longitude.doubleValue());
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(this.latitude.doubleValue())) * 
                   Math.cos(Math.toRadians(other.latitude.doubleValue())) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return earthRadius * c;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Address address = (Address) o;
        return Objects.equals(addressLine1, address.addressLine1) &&
               Objects.equals(city, address.city) &&
               Objects.equals(state, address.state) &&
               Objects.equals(zipCode, address.zipCode) &&
               Objects.equals(country, address.country);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, city, state, zipCode, country);
    }
    
    // Getters only - immutable
    public String getAddressLine1() { return addressLine1; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public String getCountry() { return country; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
}
```

### 1.3 Domain Service Implementation

```java
package com.fourkites.shipment.domain.service;

@DomainService
@Component
public class ShipmentRoutingService {
    
    private final GeospatialService geospatialService;
    private final TrafficService trafficService;
    
    public ShipmentRoutingService(GeospatialService geospatialService,
                                  TrafficService trafficService) {
        this.geospatialService = geospatialService;
        this.trafficService = trafficService;
    }
    
    /**
     * Optimizes the route for a shipment considering multiple factors
     * This is a domain service because:
     * 1. It involves multiple aggregates
     * 2. The logic doesn't naturally belong to any single entity
     * 3. It represents a domain concept (route optimization)
     */
    public OptimizedRoute optimizeRoute(Shipment shipment, 
                                        List<Stop> proposedStops,
                                        RouteOptimizationCriteria criteria) {
        
        // Complex domain logic that doesn't belong to a single entity
        List<Stop> sortedStops = sortStopsByProximity(
            shipment.getOrigin(), 
            proposedStops, 
            shipment.getDestination()
        );
        
        // Consider time windows
        sortedStops = adjustForTimeWindows(sortedStops);
        
        // Consider traffic patterns
        TrafficPrediction traffic = trafficService.getPredictedTraffic(
            sortedStops, 
            shipment.getPlannedPickupTime()
        );
        
        // Calculate total distance and time
        RouteMetrics metrics = calculateRouteMetrics(sortedStops, traffic);
        
        // Apply business rules
        if (criteria.isMinimizeDistance()) {
            sortedStops = minimizeDistance(sortedStops);
        } else if (criteria.isMinimizeTime()) {
            sortedStops = minimizeTime(sortedStops, traffic);
        }
        
        // Validate against constraints
        validateRouteConstraints(sortedStops, shipment);
        
        return new OptimizedRoute(
            sortedStops,
            metrics.getTotalDistance(),
            metrics.getEstimatedDuration(),
            metrics.getEstimatedFuelCost()
        );
    }
    
    private void validateRouteConstraints(List<Stop> stops, Shipment shipment) {
        // Ensure pickup is before all deliveries
        Stop firstPickup = stops.stream()
            .filter(s -> s.getType() == StopType.PICKUP)
            .findFirst()
            .orElseThrow(() -> new DomainException("No pickup stop found"));
            
        Stop lastDelivery = stops.stream()
            .filter(s -> s.getType() == StopType.DELIVERY)
            .reduce((first, second) -> second)
            .orElseThrow(() -> new DomainException("No delivery stop found"));
            
        if (firstPickup.getSequenceNumber() > lastDelivery.getSequenceNumber()) {
            throw new DomainException("Invalid route: pickup must precede delivery");
        }
        
        // Check capacity constraints
        if (shipment.getWeight().compareTo(BigDecimal.valueOf(80000)) > 0) {
            throw new DomainException("Shipment exceeds maximum weight capacity");
        }
    }
}
```

### 1.4 Domain Event Implementation

```java
package com.fourkites.shipment.domain.event;

public abstract class DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;
    
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = aggregateId;
    }
    
    public UUID getEventId() { return eventId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getAggregateId() { return aggregateId; }
}

public class ShipmentCreatedEvent extends DomainEvent {
    private final String shipmentNumber;
    private final String customerId;
    private final String carrierId;
    private final Address origin;
    private final Address destination;
    
    public ShipmentCreatedEvent(String aggregateId, 
                               String shipmentNumber,
                               String customerId,
                               String carrierId,
                               Address origin,
                               Address destination) {
        super(aggregateId);
        this.shipmentNumber = shipmentNumber;
        this.customerId = customerId;
        this.carrierId = carrierId;
        this.origin = origin;
        this.destination = destination;
    }
    
    // Getters...
}
```

### 1.5 Repository Pattern with Specification

```java
package com.fourkites.shipment.domain.repository;

public interface ShipmentRepository {
    // Basic CRUD
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(UUID id);
    void delete(Shipment shipment);
    
    // Domain-specific queries
    Optional<Shipment> findByShipmentNumber(String shipmentNumber);
    List<Shipment> findByCustomerIdAndStatus(String customerId, ShipmentStatus status);
    
    // Specification pattern for complex queries
    List<Shipment> findAll(Specification<Shipment> specification);
    Page<Shipment> findAll(Specification<Shipment> specification, Pageable pageable);
}

// Specification implementation
public class ActiveShipmentsSpecification implements Specification<Shipment> {
    @Override
    public Predicate toPredicate(Root<Shipment> root, 
                                 CriteriaQuery<?> query, 
                                 CriteriaBuilder cb) {
        return cb.and(
            cb.notEqual(root.get("status"), ShipmentStatus.DELIVERED),
            cb.notEqual(root.get("status"), ShipmentStatus.CANCELLED)
        );
    }
}

// Usage
List<Shipment> activeShipments = shipmentRepository.findAll(
    new ActiveShipmentsSpecification()
        .and(new CustomerSpecification(customerId))
        .and(new DateRangeSpecification(startDate, endDate))
);
```

## 2. Application Service Layer

```java
package com.fourkites.shipment.application;

@Service
@Transactional
public class ShipmentApplicationService {
    
    private final ShipmentRepository shipmentRepository;
    private final ShipmentRoutingService routingService;
    private final DomainEventPublisher eventPublisher;
    
    public ShipmentApplicationService(ShipmentRepository shipmentRepository,
                                     ShipmentRoutingService routingService,
                                     DomainEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.routingService = routingService;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Application service orchestrates the use case
     * It coordinates between domain objects but contains no domain logic
     */
    public ShipmentDTO createShipment(CreateShipmentCommand command) {
        // Use factory to create aggregate
        Shipment shipment = Shipment.create(command);
        
        // Use domain service for complex operations
        if (command.isOptimizeRoute()) {
            OptimizedRoute route = routingService.optimizeRoute(
                shipment,
                command.getStops(),
                command.getOptimizationCriteria()
            );
            
            route.getStops().forEach(shipment::addStop);
        }
        
        // Persist aggregate
        shipment = shipmentRepository.save(shipment);
        
        // Publish domain events
        shipment.getDomainEvents().forEach(eventPublisher::publish);
        
        // Return DTO (never expose domain model)
        return ShipmentMapper.toDTO(shipment);
    }
    
    public void updateShipmentStatus(UUID shipmentId, ShipmentStatus newStatus) {
        // Load aggregate
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        
        // Execute domain logic
        shipment.transitionTo(newStatus);
        
        // Persist changes
        shipmentRepository.save(shipment);
        
        // Publish events
        shipment.getDomainEvents().forEach(eventPublisher::publish);
    }
}
```

## 3. Anti-Corruption Layer Example

```java
package com.fourkites.shipment.infrastructure.acl;

@Component
public class ExternalTMSAdapter {
    
    private final TMSClient tmsClient;
    private final ShipmentTranslator translator;
    
    /**
     * Anti-Corruption Layer protects our domain from external system changes
     */
    public Optional<ShipmentDTO> importShipmentFromTMS(String tmsShipmentId) {
        try {
            // Call external system
            TMSShipmentResponse tmsResponse = tmsClient.getShipment(tmsShipmentId);
            
            // Translate external model to our domain model
            CreateShipmentCommand command = translator.translate(tmsResponse);
            
            // Validate according to our domain rules
            validateImportedShipment(command);
            
            // Return domain-compliant DTO
            return Optional.of(ShipmentDTO.from(command));
            
        } catch (TMSException e) {
            // Shield domain from external system exceptions
            log.error("Failed to import from TMS: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private void validateImportedShipment(CreateShipmentCommand command) {
        // Apply our domain rules to external data
        if (!isValidShipmentMode(command.getMode())) {
            throw new InvalidShipmentDataException(
                "Unsupported shipment mode from TMS: " + command.getMode()
            );
        }
        
        if (!isWithinServiceArea(command.getOrigin(), command.getDestination())) {
            throw new OutOfServiceAreaException(
                "Shipment route is outside service area"
            );
        }
    }
}

@Component
public class ShipmentTranslator {
    
    /**
     * Translates between external TMS model and our domain model
     */
    public CreateShipmentCommand translate(TMSShipmentResponse tmsShipment) {
        return CreateShipmentCommand.builder()
            .shipmentNumber(generateInternalNumber(tmsShipment.getRefNumber()))
            .customerId(mapCustomerId(tmsShipment.getShipper()))
            .carrierId(mapCarrierId(tmsShipment.getCarrier()))
            .mode(mapShipmentMode(tmsShipment.getTransportMode()))
            .origin(mapAddress(tmsShipment.getPickupLocation()))
            .destination(mapAddress(tmsShipment.getDeliveryLocation()))
            .plannedPickupTime(mapDateTime(tmsShipment.getPickupDate()))
            .plannedDeliveryTime(mapDateTime(tmsShipment.getDeliveryDate()))
            .build();
    }
    
    private ShipmentMode mapShipmentMode(String tmsMode) {
        return switch (tmsMode.toUpperCase()) {
            case "TL", "FTL" -> ShipmentMode.TRUCK_FTL;
            case "LTL", "PARTIAL" -> ShipmentMode.TRUCK_LTL;
            case "RAIL", "TRAIN" -> ShipmentMode.RAIL;
            case "SEA", "OCEAN" -> ShipmentMode.OCEAN;
            case "AIR", "FLIGHT" -> ShipmentMode.AIR;
            default -> ShipmentMode.TRUCK_FTL; // Default mapping
        };
    }
}
```

## 4. Event-Driven Saga Implementation

```java
package com.fourkites.shipment.application.saga;

@Component
@Slf4j
public class ShipmentCancellationSaga {
    
    private final ShipmentRepository shipmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SagaRepository sagaRepository;
    
    @KafkaListener(topics = "shipment-cancellation-requested")
    public void handle(ShipmentCancellationRequestedEvent event) {
        String sagaId = UUID.randomUUID().toString();
        Saga saga = Saga.start(sagaId, "ShipmentCancellation", event);
        
        try {
            // Step 1: Update shipment status
            updateShipmentStatus(event.getShipmentId(), ShipmentStatus.CANCELLING);
            saga.markStepCompleted("UpdateStatus");
            
            // Step 2: Stop tracking
            stopTracking(event.getShipmentId());
            saga.markStepCompleted("StopTracking");
            
            // Step 3: Notify stakeholders
            notifyStakeholders(event.getShipmentId(), event.getReason());
            saga.markStepCompleted("NotifyStakeholders");
            
            // Step 4: Process refund if applicable
            if (event.isRefundRequired()) {
                processRefund(event.getShipmentId());
                saga.markStepCompleted("ProcessRefund");
            }
            
            // Final step: Mark as cancelled
            updateShipmentStatus(event.getShipmentId(), ShipmentStatus.CANCELLED);
            saga.complete();
            
        } catch (Exception e) {
            log.error("Saga failed at step: {}", saga.getCurrentStep(), e);
            compensate(saga);
            saga.fail(e.getMessage());
        } finally {
            sagaRepository.save(saga);
        }
    }
    
    private void compensate(Saga saga) {
        // Compensate in reverse order
        saga.getCompletedSteps().descendingIterator().forEachRemaining(step -> {
            switch (step) {
                case "ProcessRefund" -> reverseRefund(saga.getAggregateId());
                case "NotifyStakeholders" -> sendCancellationReversal(saga.getAggregateId());
                case "StopTracking" -> resumeTracking(saga.getAggregateId());
                case "UpdateStatus" -> revertStatus(saga.getAggregateId());
            }
        });
    }
}
```

## 5. Testing Domain Logic

```java
package com.fourkites.shipment.domain;

class ShipmentTest {
    
    @Test
    void should_create_shipment_with_valid_data() {
        // Given
        CreateShipmentCommand command = CreateShipmentCommand.builder()
            .customerId("CUST-001")
            .carrierId("CARR-001")
            .origin(createTestAddress())
            .destination(createTestAddress())
            .plannedPickupTime(LocalDateTime.now().plusDays(1))
            .plannedDeliveryTime(LocalDateTime.now().plusDays(3))
            .build();
        
        // When
        Shipment shipment = Shipment.create(command);
        
        // Then
        assertThat(shipment).isNotNull();
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(shipment.getDomainEvents()).hasSize(1);
        assertThat(shipment.getDomainEvents().get(0))
            .isInstanceOf(ShipmentCreatedEvent.class);
    }
    
    @Test
    void should_enforce_status_transition_invariant() {
        // Given
        Shipment shipment = createTestShipment();
        shipment.transitionTo(ShipmentStatus.DELIVERED);
        
        // When/Then
        assertThatThrownBy(() -> 
            shipment.transitionTo(ShipmentStatus.IN_TRANSIT))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("Cannot transition from DELIVERED to IN_TRANSIT");
    }
    
    @Test
    void should_maintain_stop_sequence_invariant() {
        // Given
        Shipment shipment = createTestShipment();
        Stop stop1 = Stop.create(1, StopType.PICKUP, createTestAddress());
        Stop stop2 = Stop.create(1, StopType.DELIVERY, createTestAddress());
        
        // When
        shipment.addStop(stop1);
        
        // Then
        assertThatThrownBy(() -> shipment.addStop(stop2))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Stop with sequence 1 already exists");
    }
}
```

## Key DDD Implementation Principles

1. **Protect Invariants**: Aggregates enforce business rules
2. **Express Intent**: Use ubiquitous language in code
3. **Isolate Domain**: Keep domain logic free from infrastructure concerns
4. **Model Behavior**: Focus on what objects do, not just data
5. **Use Events**: Communicate between bounded contexts via events
6. **Test Domain**: Unit test domain logic extensively
7. **Avoid Anemic Models**: Put behavior with data in domain objects

This implementation guide demonstrates how DDD concepts translate into actual Java code, maintaining clean architecture and business logic integrity.