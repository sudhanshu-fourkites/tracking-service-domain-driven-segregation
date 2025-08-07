package com.fourkites.location.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "location_history")
@CompoundIndex(name = "shipment_date", def = "{'shipmentId': 1, 'date': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationHistory {
    
    @Id
    private String id;
    
    @Indexed
    private UUID shipmentId;
    
    @Indexed
    private String date;
    
    @Builder.Default
    private List<LocationPoint> locations = new ArrayList<>();
    
    private LocationStatistics statistics;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    public static LocationHistory createDaily(UUID shipmentId, String date) {
        LocationHistory history = new LocationHistory();
        history.id = shipmentId + "_" + date;
        history.shipmentId = shipmentId;
        history.date = date;
        history.createdAt = Instant.now();
        history.updatedAt = Instant.now();
        history.statistics = new LocationStatistics();
        return history;
    }
    
    public void addLocation(LocationPoint point) {
        locations.add(point);
        updateStatistics(point);
        this.updatedAt = Instant.now();
    }
    
    public void compressOldData(int keepLastNPoints) {
        if (locations.size() > keepLastNPoints) {
            List<LocationPoint> compressed = new ArrayList<>();
            
            int skipFactor = locations.size() / keepLastNPoints;
            for (int i = 0; i < locations.size(); i += skipFactor) {
                compressed.add(locations.get(i));
            }
            
            if (!compressed.contains(locations.get(locations.size() - 1))) {
                compressed.add(locations.get(locations.size() - 1));
            }
            
            locations = compressed;
            this.updatedAt = Instant.now();
        }
    }
    
    private void updateStatistics(LocationPoint point) {
        if (statistics == null) {
            statistics = new LocationStatistics();
        }
        
        statistics.totalPoints++;
        statistics.lastUpdate = point.timestamp;
        
        if (statistics.minLatitude == null || point.latitude < statistics.minLatitude) {
            statistics.minLatitude = point.latitude;
        }
        if (statistics.maxLatitude == null || point.latitude > statistics.maxLatitude) {
            statistics.maxLatitude = point.latitude;
        }
        if (statistics.minLongitude == null || point.longitude < statistics.minLongitude) {
            statistics.minLongitude = point.longitude;
        }
        if (statistics.maxLongitude == null || point.longitude > statistics.maxLongitude) {
            statistics.maxLongitude = point.longitude;
        }
        
        if (point.speed != null) {
            if (statistics.maxSpeed == null || point.speed > statistics.maxSpeed) {
                statistics.maxSpeed = point.speed;
            }
            statistics.avgSpeed = ((statistics.avgSpeed == null ? 0 : statistics.avgSpeed) 
                * (statistics.totalPoints - 1) + point.speed) / statistics.totalPoints;
        }
        
        if (locations.size() > 1) {
            LocationPoint lastPoint = locations.get(locations.size() - 2);
            double distance = calculateDistance(lastPoint.latitude, lastPoint.longitude,
                                              point.latitude, point.longitude);
            statistics.totalDistance = (statistics.totalDistance == null ? 0 : statistics.totalDistance) 
                + distance;
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
class LocationPoint {
    Double latitude;
    Double longitude;
    Double altitude;
    Double speed;
    Double heading;
    Instant timestamp;
    String source;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class LocationStatistics {
    Integer totalPoints = 0;
    Double totalDistance;
    Double maxSpeed;
    Double avgSpeed;
    Double minLatitude;
    Double maxLatitude;
    Double minLongitude;
    Double maxLongitude;
    Instant lastUpdate;
}