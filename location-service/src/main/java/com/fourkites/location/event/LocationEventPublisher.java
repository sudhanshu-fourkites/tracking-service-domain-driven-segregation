package com.fourkites.location.event;

import com.fourkites.location.domain.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String LOCATION_UPDATE_TOPIC = "location.updates";
    private static final String GEOFENCE_EVENT_TOPIC = "location.geofence.events";
    private static final String LOCATION_ALERT_TOPIC = "location.alerts";
    
    public void publishLocationUpdate(Location location) {
        LocationUpdateEvent event = LocationUpdateEvent.builder()
            .shipmentId(location.getShipmentId())
            .deviceId(location.getDeviceId())
            .latitude(location.getLatitude())
            .longitude(location.getLongitude())
            .timestamp(location.getTimestamp())
            .speed(location.getSpeed())
            .heading(location.getHeading())
            .isMoving(location.getIsMoving())
            .build();
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(LOCATION_UPDATE_TOPIC, location.getShipmentId().toString(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish location update for shipment {}: {}", 
                         location.getShipmentId(), ex.getMessage());
            } else {
                log.debug("Published location update for shipment {}", location.getShipmentId());
            }
        });
    }
    
    public void publishGeofenceEvent(UUID shipmentId, String geofenceId, String eventType) {
        GeofenceEvent event = GeofenceEvent.builder()
            .shipmentId(shipmentId)
            .geofenceId(geofenceId)
            .eventType(eventType)
            .timestamp(java.time.Instant.now())
            .build();
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(GEOFENCE_EVENT_TOPIC, shipmentId.toString(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish geofence event for shipment {}: {}", 
                         shipmentId, ex.getMessage());
            } else {
                log.info("Published geofence {} event for shipment {}", eventType, shipmentId);
            }
        });
    }
    
    public void publishLocationAlert(UUID shipmentId, String alertType, String message) {
        LocationAlert alert = LocationAlert.builder()
            .shipmentId(shipmentId)
            .alertType(alertType)
            .message(message)
            .timestamp(java.time.Instant.now())
            .build();
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(LOCATION_ALERT_TOPIC, shipmentId.toString(), alert);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish location alert for shipment {}: {}", 
                         shipmentId, ex.getMessage());
            } else {
                log.warn("Published {} alert for shipment {}: {}", alertType, shipmentId, message);
            }
        });
    }
}