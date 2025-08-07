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
public class GeofenceEvent {
    private UUID shipmentId;
    private String geofenceId;
    private String eventType;
    private Instant timestamp;
}