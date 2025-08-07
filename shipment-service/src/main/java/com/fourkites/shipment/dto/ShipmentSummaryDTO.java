package com.fourkites.shipment.dto;

import com.fourkites.shipment.domain.ShipmentMode;
import com.fourkites.shipment.domain.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentSummaryDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String shipmentNumber;
    private String customerId;
    private String carrierId;
    private ShipmentStatus status;
    private ShipmentMode mode;
    private String originCity;
    private String originState;
    private String destinationCity;
    private String destinationState;
    private LocalDateTime plannedPickupTime;
    private LocalDateTime plannedDeliveryTime;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private Integer stopCount;
    private LocalDateTime lastUpdated;
}