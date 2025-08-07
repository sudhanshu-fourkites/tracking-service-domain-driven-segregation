# Multi-stage build for optimized image size
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/location-service-*.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# JVM optimization flags for Java 21
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseZGC -XX:ZCollectionInterval=30 \
               -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp \
               -Djava.security.egd=file:/dev/./urandom \
               --enable-preview"

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]