package com.fourkites.location.dto;

import com.fourkites.location.domain.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private String id;
    private UUID shipmentId;
    private String deviceId;
    private String carrierId;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private Instant timestamp;
    private Instant receivedAt;
    private String source;
    private String quality;
    private Map<String, Object> metadata;
    private Address address;
    private String geofenceId;
    private String geofenceEvent;
    private Double batteryLevel;
    private Double signalStrength;
    private String networkType;
    private Boolean isMoving;
    private String stopId;
    private Double distanceFromStop;
}