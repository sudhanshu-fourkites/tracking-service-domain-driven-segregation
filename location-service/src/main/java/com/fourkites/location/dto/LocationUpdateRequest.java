package com.fourkites.location.dto;

import jakarta.validation.constraints.*;
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
public class LocationUpdateRequest {
    
    @NotNull(message = "Shipment ID is required")
    private UUID shipmentId;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    private String carrierId;
    
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;
    
    private Double altitude;
    
    @PositiveOrZero(message = "Accuracy must be positive")
    private Double accuracy;
    
    @PositiveOrZero(message = "Speed must be positive")
    private Double speed;
    
    @Min(value = 0, message = "Heading must be >= 0")
    @Max(value = 360, message = "Heading must be <= 360")
    private Double heading;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
    
    private String source;
    
    private Map<String, Object> metadata;
    
    @Min(value = 0, message = "Battery level must be >= 0")
    @Max(value = 100, message = "Battery level must be <= 100")
    private Double batteryLevel;
    
    private Double signalStrength;
    
    private String networkType;
    
    private String stopId;
}