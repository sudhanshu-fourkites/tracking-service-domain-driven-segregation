package com.fourkites.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Address record using Java 21 features
 * Immutable data transfer object for address information
 */
public record AddressRecord(
    String addressLine1,
    String addressLine2,
    @NotBlank(message = "City is required")
    String city,
    String state,
    String zipCode,
    @NotBlank(message = "Country is required") 
    String country,
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    BigDecimal latitude,
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    BigDecimal longitude
) {
    
    // Compact constructor for validation
    public AddressRecord {
        Objects.requireNonNull(city, "City is required");
        Objects.requireNonNull(country, "Country is required");
        
        // Validate coordinates if provided
        if (latitude != null && (latitude.doubleValue() < -90 || latitude.doubleValue() > 90)) {
            throw new IllegalArgumentException("Invalid latitude: " + latitude);
        }
        if (longitude != null && (longitude.doubleValue() < -180 || longitude.doubleValue() > 180)) {
            throw new IllegalArgumentException("Invalid longitude: " + longitude);
        }
        
        // Normalize country code to uppercase
        country = country != null ? country.toUpperCase() : null;
    }
    
    // Static factory method for creating from coordinates only
    public static AddressRecord fromCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return new AddressRecord(
            null, null, "Unknown", null, null, "Unknown",
            latitude, longitude
        );
    }
    
    // Helper method to check if address has coordinates
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
    
    // Calculate distance to another address (in kilometers)
    public double distanceTo(AddressRecord other) {
        if (!this.hasCoordinates() || !other.hasCoordinates()) {
            throw new IllegalStateException("Both addresses must have coordinates");
        }
        
        final int R = 6371; // Earth's radius in kilometers
        double lat1Rad = Math.toRadians(this.latitude.doubleValue());
        double lat2Rad = Math.toRadians(other.latitude.doubleValue());
        double deltaLat = Math.toRadians(other.latitude.doubleValue() - this.latitude.doubleValue());
        double deltaLon = Math.toRadians(other.longitude.doubleValue() - this.longitude.doubleValue());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    // Format address for display
    public String format() {
        StringBuilder sb = new StringBuilder();
        if (addressLine1 != null) sb.append(addressLine1).append(", ");
        if (addressLine2 != null) sb.append(addressLine2).append(", ");
        sb.append(city).append(", ");
        if (state != null) sb.append(state).append(" ");
        if (zipCode != null) sb.append(zipCode).append(", ");
        sb.append(country);
        return sb.toString();
    }
}