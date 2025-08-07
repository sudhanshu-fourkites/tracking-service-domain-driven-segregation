# Domain Model Visual Diagrams

## 1. Core Domain Model - Shipment Context

```mermaid
classDiagram
    class Shipment {
        <<Aggregate Root>>
        +UUID id
        +String shipmentNumber
        +String customerId
        +String carrierId
        +ShipmentStatus status
        +ShipmentMode mode
        +Address origin
        +Address destination
        +LocalDateTime plannedPickupTime
        +LocalDateTime plannedDeliveryTime
        +LocalDateTime actualPickupTime
        +LocalDateTime actualDeliveryTime
        +BigDecimal weight
        +BigDecimal volume
        +List~Stop~ stops
        +List~ShipmentEvent~ events
        +createShipment()
        +addStop()
        +updateStatus()
        +cancel()
        +markAsDelivered()
    }

    class Stop {
        <<Entity>>
        +UUID id
        +Integer sequenceNumber
        +StopType type
        +Address location
        +LocalDateTime plannedArrival
        +LocalDateTime actualArrival
        +StopStatus status
        +complete()
        +skip()
    }

    class ShipmentEvent {
        <<Entity>>
        +UUID id
        +String eventType
        +LocalDateTime timestamp
        +String description
        +String location
    }

    class Address {
        <<Value Object>>
        +String addressLine1
        +String addressLine2
        +String city
        +String state
        +String zipCode
        +String country
        +BigDecimal latitude
        +BigDecimal longitude
    }

    class ShipmentStatus {
        <<Enumeration>>
        CREATED
        CONFIRMED
        DISPATCHED
        IN_TRANSIT
        DELIVERED
        CANCELLED
    }

    class ShipmentMode {
        <<Enumeration>>
        TRUCK_FTL
        TRUCK_LTL
        RAIL
        OCEAN
        AIR
        PARCEL
    }

    Shipment "1" *-- "0..*" Stop : contains
    Shipment "1" *-- "0..*" ShipmentEvent : records
    Shipment ..> Address : uses
    Stop ..> Address : uses
    Shipment ..> ShipmentStatus : has
    Shipment ..> ShipmentMode : has
```

## 2. Location Context Domain Model

```mermaid
classDiagram
    class TrackingSession {
        <<Aggregate Root>>
        +UUID id
        +String shipmentId
        +String deviceId
        +LocalDateTime startTime
        +LocalDateTime endTime
        +GeoPoint currentLocation
        +TrackingStatus status
        +List~Breadcrumb~ breadcrumbs
        +startTracking()
        +updateLocation()
        +stopTracking()
        +detectStop()
    }

    class Breadcrumb {
        <<Entity>>
        +UUID id
        +GeoPoint location
        +LocalDateTime timestamp
        +Double speed
        +Double heading
        +Double accuracy
    }

    class Geofence {
        <<Aggregate Root>>
        +UUID id
        +String name
        +GeofenceType type
        +Polygon boundary
        +List~GeofenceTrigger~ triggers
        +contains(GeoPoint)
        +addTrigger()
    }

    class GeoPoint {
        <<Value Object>>
        +Double latitude
        +Double longitude
        +Double altitude
        +distanceTo(GeoPoint)
        +isValid()
    }

    class Polygon {
        <<Value Object>>
        +List~GeoPoint~ vertices
        +contains(GeoPoint)
        +area()
        +perimeter()
    }

    class GeofenceTrigger {
        <<Value Object>>
        +TriggerType type
        +String eventType
        +Map metadata
    }

    TrackingSession "1" *-- "0..*" Breadcrumb : tracks
    TrackingSession ..> GeoPoint : uses
    Breadcrumb ..> GeoPoint : has
    Geofence ..> Polygon : defines
    Polygon "1" *-- "3..*" GeoPoint : vertices
    Geofence "1" *-- "0..*" GeofenceTrigger : has
```

## 3. Event Context Domain Model

```mermaid
classDiagram
    class EventStream {
        <<Aggregate Root>>
        +UUID id
        +String shipmentId
        +List~TrackingEvent~ events
        +List~Milestone~ milestones
        +List~Exception~ exceptions
        +addEvent()
        +addMilestone()
        +raiseException()
        +resolveException()
    }

    class TrackingEvent {
        <<Entity>>
        +UUID id
        +EventType type
        +EventSeverity severity
        +LocalDateTime timestamp
        +String description
        +Map metadata
        +EventSource source
    }

    class Milestone {
        <<Entity>>
        +UUID id
        +MilestoneType type
        +LocalDateTime plannedTime
        +LocalDateTime achievedTime
        +MilestoneStatus status
        +complete()
        +miss()
    }

    class Exception {
        <<Entity>>
        +UUID id
        +ExceptionType type
        +String description
        +LocalDateTime raisedAt
        +LocalDateTime resolvedAt
        +ResolutionStatus status
        +resolve()
        +escalate()
    }

    class EventType {
        <<Enumeration>>
        PICKUP
        DELIVERY
        DEPARTURE
        ARRIVAL
        DELAY
        DAMAGE
        CUSTOM
    }

    class MilestoneType {
        <<Enumeration>>
        PICKUP_SCHEDULED
        PICKED_UP
        IN_TRANSIT
        AT_DESTINATION
        DELIVERED
    }

    EventStream "1" *-- "0..*" TrackingEvent : contains
    EventStream "1" *-- "0..*" Milestone : tracks
    EventStream "1" *-- "0..*" Exception : manages
    TrackingEvent ..> EventType : has
    Milestone ..> MilestoneType : has
```

## 4. Domain Event Flow Diagram

```mermaid
sequenceDiagram
    participant UI as User Interface
    participant GW as API Gateway
    participant SC as Shipment Context
    participant LC as Location Context
    participant EC as Event Context
    participant NC as Notification Context
    participant K as Kafka

    UI->>GW: Create Shipment
    GW->>SC: CreateShipmentCommand
    SC->>SC: Validate & Create Aggregate
    SC->>K: Publish ShipmentCreatedEvent
    
    K-->>LC: ShipmentCreatedEvent
    LC->>LC: Initialize TrackingSession
    
    K-->>EC: ShipmentCreatedEvent
    EC->>EC: Create EventStream
    
    K-->>NC: ShipmentCreatedEvent
    NC->>NC: Send Confirmation
    
    Note over LC: GPS Updates Start
    
    LC->>K: Publish LocationUpdatedEvent
    K-->>SC: LocationUpdatedEvent
    SC->>SC: Update ETA
    
    K-->>EC: LocationUpdatedEvent
    EC->>EC: Record Event
    
    LC->>LC: Check Geofences
    LC->>K: Publish GeofenceEnteredEvent
    
    K-->>SC: GeofenceEnteredEvent
    SC->>SC: Update Stop Status
    
    K-->>NC: GeofenceEnteredEvent
    NC->>NC: Send Arrival Alert
```

## 5. Aggregate Boundaries and Transactions

```mermaid
graph TB
    subgraph "Shipment Aggregate"
        S[Shipment Root]
        S1[Stop 1]
        S2[Stop 2]
        SE1[Event 1]
        SE2[Event 2]
        S --> S1
        S --> S2
        S --> SE1
        S --> SE2
    end
    
    subgraph "TrackingSession Aggregate"
        T[TrackingSession Root]
        B1[Breadcrumb 1]
        B2[Breadcrumb 2]
        B3[Breadcrumb 3]
        T --> B1
        T --> B2
        T --> B3
    end
    
    subgraph "Geofence Aggregate"
        G[Geofence Root]
        GT1[Trigger 1]
        GT2[Trigger 2]
        G --> GT1
        G --> GT2
    end
    
    subgraph "EventStream Aggregate"
        E[EventStream Root]
        TE1[Tracking Event 1]
        M1[Milestone 1]
        EX1[Exception 1]
        E --> TE1
        E --> M1
        E --> EX1
    end
    
    S -.->|Domain Event| T
    T -.->|Domain Event| G
    G -.->|Domain Event| E
    E -.->|Domain Event| S
```

## 6. State Machine - Shipment Status

```mermaid
stateDiagram-v2
    [*] --> CREATED: Create Shipment
    CREATED --> CONFIRMED: Confirm
    CONFIRMED --> DISPATCHED: Dispatch
    DISPATCHED --> IN_TRANSIT: Start Journey
    IN_TRANSIT --> IN_TRANSIT: Update Location
    IN_TRANSIT --> DELIVERED: Complete Delivery
    
    CREATED --> CANCELLED: Cancel
    CONFIRMED --> CANCELLED: Cancel
    DISPATCHED --> CANCELLED: Cancel
    IN_TRANSIT --> CANCELLED: Cancel
    
    IN_TRANSIT --> EXCEPTION: Exception Occurs
    EXCEPTION --> IN_TRANSIT: Resolve
    EXCEPTION --> CANCELLED: Cannot Resolve
    
    DELIVERED --> [*]: End
    CANCELLED --> [*]: End
```

## 7. Context Mapping

```mermaid
graph LR
    subgraph "Core Domain"
        SC[Shipment Context]
    end
    
    subgraph "Supporting Domain"
        LC[Location Context]
        EC[Event Context]
        NC[Notification Context]
    end
    
    subgraph "Generic Domain"
        AC[Analytics Context]
    end
    
    subgraph "External Systems"
        ERP[ERP System]
        GPS[GPS Provider]
        SMS[SMS Gateway]
        EMAIL[Email Service]
    end
    
    SC <-->|Shared Kernel| LC
    SC -->|Downstream| EC
    SC -->|Downstream| NC
    LC -->|Upstream| EC
    EC -->|Upstream| NC
    EC -->|Published Language| AC
    
    ERP -->|ACL| SC
    GPS -->|ACL| LC
    SMS -->|ACL| NC
    EMAIL -->|ACL| NC
```

## 8. Command and Query Separation (CQRS)

```mermaid
graph TB
    subgraph "Command Side"
        CMD[Commands]
        CMD --> CH[Command Handlers]
        CH --> AGG[Aggregates]
        AGG --> ES[Event Store]
        AGG --> WM[Write Model DB]
    end
    
    subgraph "Event Processing"
        ES --> EP[Event Processor]
        EP --> PROJ[Projections]
    end
    
    subgraph "Query Side"
        QRY[Queries]
        QRY --> QH[Query Handlers]
        QH --> RM[Read Model DB]
        PROJ --> RM
    end
    
    subgraph "External Events"
        KAFKA[Kafka Events]
        ES --> KAFKA
        KAFKA --> EP
    end
```

## 9. Layered Architecture per Bounded Context

```mermaid
graph TB
    subgraph "Shipment Context Layers"
        UI[UI Layer - REST Controllers]
        APP[Application Layer - Services]
        DOM[Domain Layer - Entities/VOs/Services]
        INF[Infrastructure Layer - Repositories/Kafka]
        
        UI --> APP
        APP --> DOM
        APP --> INF
        INF --> DOM
    end
    
    subgraph "Cross-Cutting"
        LOG[Logging]
        SEC[Security]
        MON[Monitoring]
        
        UI -.-> LOG
        APP -.-> SEC
        DOM -.-> LOG
        INF -.-> MON
    end
```

## 10. Event Storming Result

```
Legend:
🟧 = Domain Event
🟦 = Command
🟨 = Aggregate
🟪 = Policy
🟩 = Read Model
🟥 = External System
👤 = Actor

Timeline →

👤 Customer
    🟦 Request Shipment
        🟨 Shipment
            🟧 Shipment Requested
                🟪 Validate Business Rules
                    🟧 Shipment Created
                        🟩 Shipment List Updated

👤 Dispatcher
    🟦 Assign Carrier
        🟨 Shipment
            🟧 Carrier Assigned
                🟪 Notify Carrier
                    🟥 Send to TMS

👤 Driver
    🟦 Start Tracking
        🟨 TrackingSession
            🟧 Tracking Started
                🟪 Initialize GPS
                    🟥 GPS Provider

👤 System
    🟦 Update Location
        🟨 TrackingSession
            🟧 Location Updated
                🟪 Check Geofences
                    🟧 Geofence Entered
                        🟪 Update Stop Status
                            🟨 Shipment
                                🟧 Stop Arrived
                                    🟩 Real-time Dashboard

👤 Receiver
    🟦 Confirm Delivery
        🟨 Shipment
            🟧 Delivery Confirmed
                🟪 Calculate Metrics
                    🟨 Analytics
                        🟧 Metrics Updated
                            🟩 Performance Report
```

---

These diagrams provide a comprehensive visual representation of the Domain-Driven Design, showing:
- Class relationships and hierarchies
- Aggregate boundaries
- Event flows between contexts
- State transitions
- System architecture layers
- CQRS implementation
- Context mapping relationships

The visual models complement the textual DDD documentation and provide quick reference for developers implementing the system.