package com.fourkites.shipment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private String shipmentId;
    private String shipmentNumber;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private String address;
}