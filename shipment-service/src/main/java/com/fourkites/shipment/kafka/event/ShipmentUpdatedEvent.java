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
public class ShipmentUpdatedEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private String shipmentId;
    private String shipmentNumber;
    private String status;
    private LocalDateTime estimatedDeliveryTime;
}