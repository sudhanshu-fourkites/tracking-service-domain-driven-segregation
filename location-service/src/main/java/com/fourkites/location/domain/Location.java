package com.fourkites.location.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Document(collection = "locations")
@CompoundIndex(name = "shipment_timestamp", def = "{'shipmentId': 1, 'timestamp': -1}")
@CompoundIndex(name = "device_timestamp", def = "{'deviceId': 1, 'timestamp': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Location {
    
    @Id
    private String id;
    
    @Indexed
    private UUID shipmentId;
    
    @Indexed
    private String deviceId;
    
    @Indexed
    private String carrierId;
    
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint position;
    
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    
    @Indexed
    private Instant timestamp;
    
    private Instant receivedAt;
    
    private LocationSource source;
    
    private LocationQuality quality;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private Address address;
    
    private String geofenceId;
    private GeofenceEvent geofenceEvent;
    
    private Double batteryLevel;
    private Double signalStrength;
    private String networkType;
    
    @Indexed
    private Boolean isMoving;
    
    private String stopId;
    private Double distanceFromStop;
    
    @Version
    private Long version;
    
    public static Location create(UUID shipmentId, String deviceId, double latitude, 
                                 double longitude, Instant timestamp) {
        validateCoordinates(latitude, longitude);
        
        Location location = new Location();
        location.id = UUID.randomUUID().toString();
        location.shipmentId = shipmentId;
        location.deviceId = deviceId;
        location.latitude = latitude;
        location.longitude = longitude;
        location.position = new GeoJsonPoint(longitude, latitude);
        location.timestamp = timestamp;
        location.receivedAt = Instant.now();
        location.quality = LocationQuality.STANDARD;
        location.source = LocationSource.GPS;
        location.isMoving = false;
        
        return location;
    }
    
    public void updatePosition(double latitude, double longitude, Instant timestamp) {
        validateCoordinates(latitude, longitude);
        
        if (timestamp.isBefore(this.timestamp)) {
            throw new IllegalArgumentException("Cannot update with older timestamp");
        }
        
        double distance = calculateDistance(this.latitude, this.longitude, latitude, longitude);
        
        this.latitude = latitude;
        this.longitude = longitude;
        this.position = new GeoJsonPoint(longitude, latitude);
        this.timestamp = timestamp;
        
        this.isMoving = distance > 0.01 && this.speed != null && this.speed > 0.5;
    }
    
    public void enterGeofence(String geofenceId) {
        this.geofenceId = geofenceId;
        this.geofenceEvent = GeofenceEvent.ENTER;
    }
    
    public void exitGeofence(String geofenceId) {
        if (!geofenceId.equals(this.geofenceId)) {
            throw new IllegalArgumentException("Cannot exit different geofence");
        }
        this.geofenceEvent = GeofenceEvent.EXIT;
    }
    
    public boolean isStale(int thresholdMinutes) {
        return timestamp.isBefore(Instant.now().minusSeconds(thresholdMinutes * 60L));
    }
    
    public boolean isHighQuality() {
        return quality == LocationQuality.HIGH && 
               accuracy != null && accuracy < 50;
    }
    
    public boolean isAtStop(double stopLat, double stopLon, double thresholdMeters) {
        double distance = calculateDistance(latitude, longitude, stopLat, stopLon);
        return distance * 1000 <= thresholdMeters;
    }
    
    private static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude: " + longitude);
        }
    }
    
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

enum LocationSource {
    GPS,
    CELL_TOWER,
    WIFI,
    MANUAL,
    CALCULATED,
    MIXED
}

enum LocationQuality {
    HIGH,
    STANDARD,
    LOW,
    UNKNOWN
}

enum GeofenceEvent {
    ENTER,
    EXIT,
    DWELL
}