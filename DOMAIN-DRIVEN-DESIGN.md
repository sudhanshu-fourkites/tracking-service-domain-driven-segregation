# Domain-Driven Design Documentation
## Tracking Service Microservices Architecture

## 1. Strategic Design

### 1.1 Domain Overview
The **Tracking Domain** represents a real-time shipment tracking system that monitors the movement of goods from origin to destination, providing visibility, notifications, and analytics throughout the supply chain.

### 1.2 Ubiquitous Language

| Term | Definition |
|------|------------|
| **Shipment** | A consignment of goods being transported from origin to destination |
| **Carrier** | The transportation company responsible for moving the shipment |
| **Stop** | A planned location where the shipment will arrive/depart during transit |
| **Milestone** | A significant event in the shipment lifecycle |
| **Geofence** | A virtual geographic boundary that triggers events when crossed |
| **ETA** | Estimated Time of Arrival at destination |
| **Dwell Time** | Time spent at a stop beyond planned duration |
| **Exception** | An unexpected event that impacts normal shipment flow |
| **Mode** | Transportation type (FTL, LTL, Rail, Ocean, Air, etc.) |
| **Breadcrumb** | A GPS location point in the shipment's journey |
| **SLA** | Service Level Agreement defining performance expectations |

## 2. Bounded Contexts

### 2.1 Context Map

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway                              │
│                    (Customer Facing Layer)                       │
└─────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼────────┐     ┌───────▼────────┐     ┌───────▼────────┐
│   Shipment     │     │   Location      │     │     Event       │
│   Context      │◄────┤   Context       ├────►│    Context      │
└───────┬────────┘     └───────┬────────┘     └───────┬────────┘
        │                       │                       │
        │              ┌────────▼────────┐             │
        └─────────────►│  Notification   │◄────────────┘
                       │    Context      │
                       └────────┬────────┘
                                │
                       ┌────────▼────────┐
                       │   Analytics     │
                       │    Context      │
                       └─────────────────┘

Legend:
─────► Async Communication (Kafka Events)
◄────► Bidirectional Communication
```

### 2.2 Bounded Context Details

## 📦 **Shipment Context** (Core Domain)

**Purpose**: Manages the lifecycle and core business logic of shipments

### Aggregates

#### 1. **Shipment Aggregate** (Aggregate Root)
```java
Shipment {
    - id: UUID
    - shipmentNumber: String (unique)
    - customerId: String
    - carrierId: String
    - status: ShipmentStatus
    - mode: ShipmentMode
    - origin: Address (Value Object)
    - destination: Address (Value Object)
    - stops: List<Stop> (Entities)
    - events: List<ShipmentEvent> (Entities)
    - plannedPickupTime: LocalDateTime
    - plannedDeliveryTime: LocalDateTime
    - actualPickupTime: LocalDateTime
    - actualDeliveryTime: LocalDateTime
    - estimatedDeliveryTime: LocalDateTime
    - metadata: Map<String, Object>
}
```

#### 2. **Stop Entity**
```java
Stop {
    - id: UUID
    - sequenceNumber: Integer
    - type: StopType
    - location: Address
    - plannedArrival: LocalDateTime
    - actualArrival: LocalDateTime
    - status: StopStatus
}
```

### Value Objects
- **Address**: Immutable location representation
- **Weight**: Weight with unit of measure
- **Volume**: Volume with unit of measure
- **Temperature Range**: Min/max temperature bounds

### Domain Events
- `ShipmentCreatedEvent`
- `ShipmentUpdatedEvent`
- `ShipmentCancelledEvent`
- `ShipmentDeliveredEvent`
- `StopAddedEvent`
- `StopCompletedEvent`

### Invariants
- Shipment number must be unique
- Cannot modify delivered or cancelled shipments
- Stops must maintain sequential order
- Planned delivery must be after planned pickup
- Status transitions must follow valid state machine

### Domain Services
- `ShipmentNumberGenerator`: Generates unique shipment numbers
- `ShipmentValidator`: Complex validation logic
- `RouteOptimizer`: Optimizes stop sequences

---

## 📍 **Location Context** (Supporting Domain)

**Purpose**: Handles real-time GPS tracking and geospatial operations

### Aggregates

#### 1. **TrackingSession Aggregate**
```java
TrackingSession {
    - id: UUID
    - shipmentId: String
    - deviceId: String
    - startTime: LocalDateTime
    - endTime: LocalDateTime
    - breadcrumbs: List<Breadcrumb>
    - currentLocation: GeoPoint
    - status: TrackingStatus
}
```

#### 2. **Geofence Aggregate**
```java
Geofence {
    - id: UUID
    - name: String
    - type: GeofenceType
    - boundary: Polygon
    - associatedStops: List<String>
    - triggers: List<GeofenceTrigger>
}
```

### Value Objects
- **GeoPoint**: Latitude, longitude, altitude
- **Breadcrumb**: GeoPoint + timestamp + speed + heading
- **Polygon**: List of GeoPoints forming boundary
- **Distance**: Distance with unit

### Domain Events
- `LocationUpdatedEvent`
- `GeofenceEnteredEvent`
- `GeofenceExitedEvent`
- `RouteDeviationDetectedEvent`
- `StoppedEvent`
- `MovingEvent`

### Domain Services
- `GeofenceCalculator`: Determines point-in-polygon
- `RouteDeviationDetector`: Detects route deviations
- `ETACalculator`: Calculates estimated arrival times
- `DistanceCalculator`: Haversine distance calculations

---

## 📊 **Event Context** (Supporting Domain)

**Purpose**: Captures, processes, and manages all tracking events

### Aggregates

#### 1. **EventStream Aggregate**
```java
EventStream {
    - id: UUID
    - shipmentId: String
    - events: List<TrackingEvent>
    - milestones: List<Milestone>
    - exceptions: List<Exception>
}
```

#### 2. **Milestone Aggregate**
```java
Milestone {
    - id: UUID
    - type: MilestoneType
    - plannedTime: LocalDateTime
    - achievedTime: LocalDateTime
    - status: MilestoneStatus
    - metadata: Map<String, Object>
}
```

### Value Objects
- **EventType**: Enumeration of event types
- **EventSeverity**: Critical, Warning, Info
- **EventSource**: System, Manual, Integration

### Domain Events
- `EventOccurredEvent`
- `MilestoneReachedEvent`
- `ExceptionRaisedEvent`
- `ExceptionResolvedEvent`

### Domain Services
- `EventProcessor`: Processes raw events
- `MilestoneEvaluator`: Evaluates milestone completion
- `EventAggregator`: Aggregates related events

---

## 🔔 **Notification Context** (Supporting Domain)

**Purpose**: Manages notification preferences and delivery

### Aggregates

#### 1. **NotificationProfile Aggregate**
```java
NotificationProfile {
    - id: UUID
    - userId: String
    - preferences: NotificationPreferences
    - channels: List<NotificationChannel>
    - subscriptions: List<Subscription>
}
```

#### 2. **NotificationTemplate Aggregate**
```java
NotificationTemplate {
    - id: UUID
    - name: String
    - eventType: String
    - channels: List<ChannelType>
    - content: Map<ChannelType, Template>
}
```

### Value Objects
- **NotificationChannel**: Email, SMS, Push, Webhook
- **NotificationPriority**: High, Medium, Low
- **DeliveryWindow**: Time range for notifications

### Domain Events
- `NotificationSentEvent`
- `NotificationFailedEvent`
- `NotificationAcknowledgedEvent`

### Domain Services
- `NotificationRouter`: Routes to appropriate channel
- `TemplateEngine`: Processes notification templates
- `ThrottleService`: Prevents notification flooding

---

## 📈 **Analytics Context** (Generic Domain)

**Purpose**: Provides insights and analytics

### Aggregates

#### 1. **PerformanceMetrics Aggregate**
```java
PerformanceMetrics {
    - id: UUID
    - shipmentId: String
    - onTimePerformance: Percentage
    - dwellTimes: List<DwellTime>
    - transitTime: Duration
    - deviations: List<Deviation>
}
```

### Read Models
- **DashboardView**: Real-time KPIs
- **HistoricalAnalysis**: Trend analysis
- **CarrierScorecard**: Carrier performance

### Domain Services
- `MetricsCalculator`: Calculates KPIs
- `TrendAnalyzer`: Identifies patterns
- `AnomalyDetector`: Detects anomalies

---

## 3. Aggregate Design Patterns

### 3.1 Aggregate Boundaries

```
Shipment Aggregate
├── Shipment (Root)
├── Stop (Entity)
├── ShipmentEvent (Entity)
└── Value Objects
    ├── Address
    ├── Weight
    └── Temperature

TrackingSession Aggregate
├── TrackingSession (Root)
├── Breadcrumb (Entity)
└── Value Objects
    ├── GeoPoint
    └── Speed
```

### 3.2 Consistency Boundaries

- **Strong Consistency**: Within aggregate boundaries
- **Eventual Consistency**: Across aggregates via domain events

## 4. Domain Event Flow

### 4.1 Event Choreography

```
1. User creates shipment via API Gateway
   └── ShipmentContext publishes "ShipmentCreatedEvent"
       ├── LocationContext subscribes → Creates TrackingSession
       ├── EventContext subscribes → Initializes EventStream
       └── NotificationContext subscribes → Sends confirmation

2. GPS update received
   └── LocationContext publishes "LocationUpdatedEvent"
       ├── ShipmentContext subscribes → Updates ETA
       ├── EventContext subscribes → Records event
       └── GeofenceService evaluates → May trigger "GeofenceEnteredEvent"

3. Geofence entered at destination
   └── LocationContext publishes "GeofenceEnteredEvent"
       ├── ShipmentContext subscribes → Updates stop status
       ├── EventContext subscribes → Creates milestone
       └── NotificationContext subscribes → Sends arrival notification
```

## 5. Anti-Corruption Layer

### 5.1 External System Integration

```
External Systems          ACL                    Domain
┌──────────────┐    ┌────────────┐    ┌──────────────┐
│   ERP System │───►│  Adapter   │───►│   Shipment   │
└──────────────┘    └────────────┘    │   Context    │
┌──────────────┐    ┌────────────┐    └──────────────┘
│  GPS Provider│───►│ Translator │───►┌──────────────┐
└──────────────┘    └────────────┘    │   Location   │
┌──────────────┐    ┌────────────┐    │   Context    │
│   TMS System │───►│   Mapper   │───►└──────────────┘
└──────────────┘    └────────────┘
```

## 6. Repository Patterns

### 6.1 Aggregate Repository Interface

```java
public interface ShipmentRepository {
    // Aggregate persistence
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(UUID id);
    Optional<Shipment> findByShipmentNumber(String number);
    
    // Query methods
    Page<Shipment> findByCustomerId(String customerId, Pageable pageable);
    List<Shipment> findActiveShipments();
    
    // Specification pattern
    List<Shipment> findAll(Specification<Shipment> spec);
}
```

## 7. Domain Service Examples

### 7.1 Route Optimization Service

```java
@DomainService
public class RouteOptimizationService {
    
    public OptimizedRoute optimize(Shipment shipment, 
                                   List<Stop> proposedStops,
                                   OptimizationCriteria criteria) {
        // Complex domain logic for route optimization
        // May involve:
        // - Distance calculations
        // - Time window constraints
        // - Vehicle capacity constraints
        // - Traffic predictions
        return new OptimizedRoute(orderedStops, totalDistance, estimatedTime);
    }
}
```

### 7.2 ETA Calculation Service

```java
@DomainService
public class ETACalculationService {
    
    public LocalDateTime calculateETA(
            GeoPoint currentLocation,
            GeoPoint destination,
            Double averageSpeed,
            List<Stop> remainingStops,
            TrafficConditions traffic) {
        
        // Complex ETA calculation considering:
        // - Current location and speed
        // - Historical data
        // - Traffic conditions
        // - Planned stops
        // - Driver hours regulations
        
        return estimatedArrival;
    }
}
```

## 8. Saga Patterns

### 8.1 Shipment Cancellation Saga

```
CancellationSaga:
1. Receive CancelShipmentCommand
2. ShipmentContext: Update status to CANCELLING
3. LocationContext: Stop tracking session
4. NotificationContext: Send cancellation notifications
5. EventContext: Record cancellation event
6. If all successful:
   - ShipmentContext: Update status to CANCELLED
7. If any failure:
   - Compensate: Revert to previous status
```

## 9. CQRS Implementation

### 9.1 Command Side

```java
// Commands
CreateShipmentCommand
UpdateShipmentCommand
CancelShipmentCommand
AddStopCommand

// Command Handlers
@CommandHandler
public class ShipmentCommandHandler {
    public UUID handle(CreateShipmentCommand cmd) {
        // Business logic
        // Persist aggregate
        // Publish events
    }
}
```

### 9.2 Query Side

```java
// Queries
GetShipmentByIdQuery
GetShipmentsByCustomerQuery
GetNearbyShipmentsQuery

// Query Handlers
@QueryHandler
public class ShipmentQueryHandler {
    public ShipmentDTO handle(GetShipmentByIdQuery query) {
        // Read from read model
        // May use different database optimized for queries
    }
}
```

## 10. Invariant Rules

### 10.1 Shipment Invariants

1. **Unique Shipment Number**: No two shipments can have the same number
2. **Valid Status Transitions**: 
   - CREATED → CONFIRMED → DISPATCHED → IN_TRANSIT → DELIVERED
   - Any status → CANCELLED (except DELIVERED)
3. **Stop Sequence**: Stops must maintain sequential order
4. **Time Constraints**: Planned delivery > Planned pickup
5. **Weight/Volume Limits**: Cannot exceed vehicle capacity

### 10.2 Location Invariants

1. **Valid Coordinates**: Latitude ∈ [-90, 90], Longitude ∈ [-180, 180]
2. **Chronological Breadcrumbs**: Timestamps must be sequential
3. **Speed Limits**: Speed cannot exceed reasonable limits
4. **Geofence Boundaries**: Must form valid polygon

## 11. Event Sourcing Considerations

### 11.1 Event Store Structure

```java
EventStore {
    - aggregateId: UUID
    - eventType: String
    - eventData: JSON
    - eventTimestamp: LocalDateTime
    - eventVersion: Long
    - metadata: JSON
}
```

### 11.2 Event Replay

```java
public Shipment rebuild(UUID shipmentId) {
    List<DomainEvent> events = eventStore.getEvents(shipmentId);
    Shipment shipment = new Shipment();
    
    for (DomainEvent event : events) {
        shipment.apply(event);
    }
    
    return shipment;
}
```

## 12. Integration Patterns

### 12.1 Published Language

External APIs use DTOs that translate domain concepts:

```java
// External API DTO
ShipmentDTO {
    - trackingNumber (maps to shipmentNumber)
    - status (simplified from domain status)
    - estimatedDelivery (computed from domain)
}

// Translation Layer
ShipmentDTO toDTO(Shipment domainModel) {
    // Translation logic
}
```

## 13. Testing Strategy

### 13.1 Domain Model Testing

```java
@Test
void shipment_cannot_be_delivered_before_pickup() {
    // Given
    Shipment shipment = createTestShipment();
    
    // When/Then
    assertThrows(
        DomainException.class,
        () -> shipment.markAsDelivered(beforePickupTime)
    );
}
```

### 13.2 Domain Service Testing

```java
@Test
void route_optimizer_minimizes_total_distance() {
    // Given
    List<Stop> stops = createTestStops();
    
    // When
    OptimizedRoute route = routeOptimizer.optimize(stops);
    
    // Then
    assertThat(route.getTotalDistance()).isLessThan(unoptimizedDistance);
}
```

## 14. Domain Primitives

### 14.1 Custom Types

```java
public class ShipmentNumber {
    private final String value;
    
    public ShipmentNumber(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid shipment number");
        }
        this.value = value;
    }
    
    private boolean isValid(String value) {
        return value.matches("^[A-Z]{3}-\\d{6}$");
    }
}
```

## 15. Tactical Patterns Summary

| Pattern | Usage in System |
|---------|----------------|
| **Aggregate** | Shipment, TrackingSession, Geofence, EventStream |
| **Entity** | Stop, Breadcrumb, ShipmentEvent |
| **Value Object** | Address, GeoPoint, Weight, Temperature |
| **Domain Service** | RouteOptimizer, ETACalculator, GeofenceCalculator |
| **Domain Event** | All state changes published as events |
| **Repository** | One per aggregate root |
| **Factory** | ShipmentFactory for complex creation |
| **Specification** | For complex queries |

---

## Conclusion

This Domain-Driven Design provides:
- **Clear boundaries** between different business capabilities
- **Rich domain models** that encapsulate business logic
- **Event-driven architecture** for loose coupling
- **Consistency guarantees** within and across aggregates
- **Scalability** through bounded context separation
- **Maintainability** through ubiquitous language and clear patterns