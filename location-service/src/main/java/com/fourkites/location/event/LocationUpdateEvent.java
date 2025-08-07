package com.fourkites.location.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateEvent {
    private UUID shipmentId;
    private String deviceId;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private Double speed;
    private Double heading;
    private Boolean isMoving;
}