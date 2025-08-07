package com.fourkites.location.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbySearchRequest {
    
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;
    
    @NotNull(message = "Radius is required")
    @Positive(message = "Radius must be positive")
    @Max(value = 100000, message = "Radius cannot exceed 100km")
    private Double radiusMeters;
    
    private Instant minTimestamp;
    
    @Min(value = 1)
    @Max(value = 1000)
    private Integer maxResults;
    
    private String carrierId;
    
    private Boolean movingOnly;
}