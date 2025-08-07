package com.fourkites.location.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

@Document(collection = "geofences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Geofence {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    @Indexed
    private String type;
    
    @Indexed
    private String customerId;
    
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPolygon boundary;
    
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusMeters;
    
    @Indexed
    private Boolean active;
    
    @Builder.Default
    private Set<String> tags = new HashSet<>();
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private GeofenceNotificationSettings notificationSettings;
    
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    
    @Version
    private Long version;
    
    public static Geofence createCircular(String name, String customerId, 
                                         double centerLat, double centerLon, 
                                         double radiusMeters) {
        validateRadius(radiusMeters);
        
        Geofence geofence = new Geofence();
        geofence.id = UUID.randomUUID().toString();
        geofence.name = name;
        geofence.customerId = customerId;
        geofence.centerLatitude = centerLat;
        geofence.centerLongitude = centerLon;
        geofence.radiusMeters = radiusMeters;
        geofence.type = "CIRCULAR";
        geofence.active = true;
        geofence.createdAt = Instant.now();
        geofence.updatedAt = Instant.now();
        
        return geofence;
    }
    
    public boolean containsLocation(double latitude, double longitude) {
        if ("CIRCULAR".equals(type)) {
            double distance = calculateDistance(centerLatitude, centerLongitude, 
                                              latitude, longitude);
            return distance * 1000 <= radiusMeters;
        }
        return false;
    }
    
    public void activate() {
        if (this.active) {
            throw new IllegalStateException("Geofence is already active");
        }
        this.active = true;
        this.updatedAt = Instant.now();
    }
    
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("Geofence is already inactive");
        }
        this.active = false;
        this.updatedAt = Instant.now();
    }
    
    public void updateRadius(double newRadiusMeters) {
        validateRadius(newRadiusMeters);
        if (!"CIRCULAR".equals(type)) {
            throw new IllegalStateException("Can only update radius for circular geofences");
        }
        this.radiusMeters = newRadiusMeters;
        this.updatedAt = Instant.now();
    }
    
    private static void validateRadius(double radiusMeters) {
        if (radiusMeters <= 0 || radiusMeters > 50000) {
            throw new IllegalArgumentException("Radius must be between 0 and 50000 meters");
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GeofenceNotificationSettings {
    private boolean notifyOnEntry;
    private boolean notifyOnExit;
    private boolean notifyOnDwell;
    private int dwellTimeMinutes;
    private List<String> notificationChannels;
    private Map<String, String> emailRecipients;
}