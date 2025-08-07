# Tracking Service Microservices Architecture

## Domain-Driven Design Microservices

### 1. **Shipment Service**
- **Domain**: Shipment Management
- **Responsibilities**: 
  - Create, update, delete shipments
  - Manage shipment lifecycle
  - Track shipment status
- **Kafka Topics**:
  - Produces: `shipment-created`, `shipment-updated`, `shipment-cancelled`
  - Consumes: `location-updates`, `event-occurred`

### 2. **Location Service**
- **Domain**: Location Tracking
- **Responsibilities**:
  - Real-time location updates
  - Geofencing and zone management
  - Route tracking and deviation detection
- **Kafka Topics**:
  - Produces: `location-updates`, `geofence-events`, `route-deviation`
  - Consumes: `shipment-created`

### 3. **Event Service**
- **Domain**: Event Management
- **Responsibilities**:
  - Capture tracking events
  - Event aggregation and processing
  - Event history management
- **Kafka Topics**:
  - Produces: `event-occurred`, `milestone-reached`
  - Consumes: `location-updates`, `shipment-updated`

### 4. **Notification Service**
- **Domain**: Notifications
- **Responsibilities**:
  - Send alerts and notifications
  - Manage notification preferences
  - Multi-channel delivery (email, SMS, push)
- **Kafka Topics**:
  - Consumes: `event-occurred`, `milestone-reached`, `route-deviation`

### 5. **Analytics Service**
- **Domain**: Analytics and Reporting
- **Responsibilities**:
  - Performance metrics calculation
  - Real-time dashboards
  - Historical analysis
- **Kafka Topics**:
  - Consumes: All events for analytics

### 6. **API Gateway**
- **Domain**: External API Management
- **Responsibilities**:
  - Route requests to appropriate services
  - Authentication and authorization
  - Rate limiting and caching

## Technology Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Messaging**: Apache Kafka
- **Database**: PostgreSQL (per service)
- **Cache**: Redis
- **API**: REST + GraphQL
- **Testing**: JUnit 5, Mockito, TestContainers