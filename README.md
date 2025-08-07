# Tracking Service Microservices Architecture

## Overview
This is a comprehensive domain-driven microservices architecture for a tracking service system, built with Java Spring Boot and Apache Kafka for inter-service communication.

## Architecture Components

### 1. Shipment Service (Port: 8081)
**Domain**: Shipment Management
- Complete CRUD operations for shipments
- Shipment lifecycle management
- Stop management
- Status tracking
- Event publishing for all shipment changes

**Key Features**:
- RESTful APIs with OpenAPI documentation
- Redis caching for performance
- PostgreSQL for persistence
- Comprehensive unit and integration tests
- Kafka event publishing

### 2. Location Service (Port: 8082)
**Domain**: Real-time Location Tracking
- GPS location updates
- Geofencing capabilities
- Route tracking and deviation detection
- Location history management

### 3. Event Service (Port: 8083)
**Domain**: Event Management
- Event capture and aggregation
- Milestone tracking
- Event history
- Complex event processing

### 4. Notification Service (Port: 8084)
**Domain**: Multi-channel Notifications
- Email notifications
- SMS alerts
- Push notifications
- Notification preferences management

### 5. Analytics Service (Port: 8085)
**Domain**: Analytics and Reporting
- Real-time metrics
- Historical analysis
- Performance dashboards
- KPI calculations

### 6. API Gateway (Port: 8080)
**Domain**: External API Management
- Request routing
- Authentication/Authorization
- Rate limiting
- API aggregation

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Messaging**: Apache Kafka
- **Database**: PostgreSQL
- **Cache**: Redis
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **Containerization**: Docker
- **Orchestration**: Kubernetes

## Kafka Topics and Event Flow

### Published Events:
- `shipment-created`: When a new shipment is created
- `shipment-updated`: When shipment details are updated
- `shipment-cancelled`: When a shipment is cancelled
- `shipment-delivered`: When a shipment is delivered
- `shipment-status-changed`: When shipment status changes
- `location-updates`: Real-time GPS updates
- `event-occurred`: General tracking events
- `milestone-reached`: When shipment reaches milestones
- `geofence-events`: Geofence entry/exit events
- `route-deviation`: When shipment deviates from route

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.x

### Local Development Setup

1. **Start Infrastructure Services**:
```bash
docker-compose up -d postgres redis kafka zookeeper
```

2. **Build All Services**:
```bash
mvn clean install -f shipment-service/pom.xml
mvn clean install -f location-service/pom.xml
mvn clean install -f event-service/pom.xml
mvn clean install -f notification-service/pom.xml
mvn clean install -f analytics-service/pom.xml
mvn clean install -f api-gateway/pom.xml
```

3. **Run Services**:
```bash
# Terminal 1 - Shipment Service
cd shipment-service && mvn spring-boot:run

# Terminal 2 - Location Service
cd location-service && mvn spring-boot:run

# Terminal 3 - Event Service
cd event-service && mvn spring-boot:run

# Terminal 4 - Notification Service
cd notification-service && mvn spring-boot:run

# Terminal 5 - Analytics Service
cd analytics-service && mvn spring-boot:run

# Terminal 6 - API Gateway
cd api-gateway && mvn spring-boot:run
```

### Running Tests

**Unit Tests**:
```bash
mvn test
```

**Integration Tests**:
```bash
mvn verify
```

**Test Coverage Report**:
```bash
mvn jacoco:report
```

## API Documentation

Each service exposes Swagger UI for API documentation:

- Shipment Service: http://localhost:8081/shipment-service/swagger-ui.html
- Location Service: http://localhost:8082/location-service/swagger-ui.html
- Event Service: http://localhost:8083/event-service/swagger-ui.html
- Notification Service: http://localhost:8084/notification-service/swagger-ui.html
- Analytics Service: http://localhost:8085/analytics-service/swagger-ui.html
- API Gateway: http://localhost:8080/swagger-ui.html

## Sample API Calls

### Create a Shipment
```bash
curl -X POST http://localhost:8081/shipment-service/api/v1/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentNumber": "SHP-2024-001",
    "customerId": "CUST-001",
    "carrierId": "CARR-001",
    "mode": "TRUCK_FTL",
    "origin": {
      "addressLine1": "123 Origin St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA",
      "latitude": 40.7128,
      "longitude": -74.0060
    },
    "destination": {
      "addressLine1": "456 Dest Ave",
      "city": "Los Angeles",
      "state": "CA",
      "zipCode": "90001",
      "country": "USA",
      "latitude": 34.0522,
      "longitude": -118.2437
    },
    "plannedPickupTime": "2024-12-20T10:00:00",
    "plannedDeliveryTime": "2024-12-22T18:00:00",
    "weight": 1000.00,
    "pieceCount": 10
  }'
```

### Update Shipment Status
```bash
curl -X PATCH http://localhost:8081/shipment-service/api/v1/shipments/{id}/status?status=IN_TRANSIT
```

### Get Shipment Details
```bash
curl -X GET http://localhost:8081/shipment-service/api/v1/shipments/{id}
```

## Monitoring & Observability

### Health Checks
- Each service: `http://localhost:{port}/{service}/actuator/health`

### Metrics (Prometheus)
- Each service: `http://localhost:{port}/{service}/actuator/prometheus`

### Distributed Tracing
- Integrated with Spring Cloud Sleuth
- Zipkin integration for trace visualization

## Security

- JWT-based authentication (implemented in API Gateway)
- Service-to-service communication secured with mTLS
- API rate limiting
- Input validation and sanitization
- SQL injection prevention through JPA

## Deployment

### Docker Build
```bash
docker build -t fourkites/shipment-service:latest ./shipment-service
docker build -t fourkites/location-service:latest ./location-service
# ... repeat for other services
```

### Kubernetes Deployment
```bash
kubectl apply -f infrastructure/kubernetes/
```

## Performance Optimizations

1. **Caching**: Redis caching for frequently accessed data
2. **Database**: Connection pooling, query optimization, indexes
3. **Kafka**: Batch processing, compression, partitioning
4. **API**: Pagination, field filtering, async processing
5. **JVM**: Optimized heap sizes, G1GC collector

## Development Best Practices

1. **Domain-Driven Design**: Clear bounded contexts
2. **Event-Driven Architecture**: Loose coupling via Kafka
3. **Test Coverage**: Minimum 80% coverage
4. **API Versioning**: URI versioning (v1, v2)
5. **Error Handling**: Consistent error responses
6. **Logging**: Structured logging with correlation IDs
7. **Documentation**: OpenAPI specs, README files

## Troubleshooting

### Common Issues

1. **Kafka Connection Issues**:
```bash
# Check Kafka is running
docker ps | grep kafka
# Check topic list
kafka-topics --list --bootstrap-server localhost:9092
```

2. **Database Connection Issues**:
```bash
# Check PostgreSQL is running
docker ps | grep postgres
# Test connection
psql -h localhost -U shipment_user -d shipment_db
```

3. **Service Discovery Issues**:
```bash
# Check service registration
curl http://localhost:8761/eureka/apps
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit changes with descriptive messages
4. Write/update tests
5. Submit a pull request

## License

Copyright © 2024 FourKites

## Contact

For questions or support, contact the development team.