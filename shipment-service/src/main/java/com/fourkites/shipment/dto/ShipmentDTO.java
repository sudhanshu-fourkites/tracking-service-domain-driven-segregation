package com.fourkites.shipment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fourkites.shipment.domain.ShipmentMode;
import com.fourkites.shipment.domain.ShipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentDTO {

    private UUID id;

    @NotBlank(message = "Shipment number is required")
    @Size(max = 50, message = "Shipment number must not exceed 50 characters")
    private String shipmentNumber;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Carrier ID is required")
    private String carrierId;

    @NotNull(message = "Shipment status is required")
    private ShipmentStatus status;

    @NotNull(message = "Shipment mode is required")
    private ShipmentMode mode;

    @Valid
    @NotNull(message = "Origin address is required")
    private AddressDTO origin;

    @Valid
    @NotNull(message = "Destination address is required")
    private AddressDTO destination;

    @NotNull(message = "Planned pickup time is required")
    @Future(message = "Planned pickup time must be in the future")
    private LocalDateTime plannedPickupTime;

    private LocalDateTime actualPickupTime;

    @NotNull(message = "Planned delivery time is required")
    @Future(message = "Planned delivery time must be in the future")
    private LocalDateTime plannedDeliveryTime;

    private LocalDateTime actualDeliveryTime;
    private LocalDateTime estimatedDeliveryTime;

    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Weight exceeds maximum allowed")
    private BigDecimal weight;

    @DecimalMin(value = "0.01", message = "Volume must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Volume exceeds maximum allowed")
    private BigDecimal volume;

    @Min(value = 1, message = "Piece count must be at least 1")
    @Max(value = 99999, message = "Piece count exceeds maximum allowed")
    private Integer pieceCount;

    @Size(max = 500, message = "Commodity description must not exceed 500 characters")
    private String commodityDescription;

    @Size(max = 50, message = "Reference number must not exceed 50 characters")
    private String referenceNumber;

    @Size(max = 50, message = "PO number must not exceed 50 characters")
    private String poNumber;

    @Size(max = 50, message = "Bill of lading must not exceed 50 characters")
    private String billOfLading;

    @DecimalMin(value = "0.00", message = "Declared value cannot be negative")
    private BigDecimal declaredValue;

    @Size(max = 1000, message = "Special instructions must not exceed 1000 characters")
    private String specialInstructions;

    private Boolean temperatureControlled;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
    private Boolean hazmat;
    private String hazmatClass;

    private List<StopDTO> stops;
    private List<String> tags;
    private String metadata;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}