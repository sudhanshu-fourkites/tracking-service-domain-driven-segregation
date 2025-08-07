package com.fourkites.shipment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fourkites.shipment.domain.StopStatus;
import com.fourkites.shipment.domain.StopType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StopDTO {

    private UUID id;

    @NotNull(message = "Sequence number is required")
    @Min(value = 1, message = "Sequence number must be at least 1")
    private Integer sequenceNumber;

    @NotNull(message = "Stop type is required")
    private StopType type;

    @Valid
    @NotNull(message = "Location is required")
    private AddressDTO location;

    @NotNull(message = "Planned arrival is required")
    private LocalDateTime plannedArrival;

    private LocalDateTime actualArrival;

    @NotNull(message = "Planned departure is required")
    private LocalDateTime plannedDeparture;

    private LocalDateTime actualDeparture;

    @Size(max = 50, message = "Stop reference number must not exceed 50 characters")
    private String stopReferenceNumber;

    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String contactPhone;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Contact email must not exceed 100 characters")
    private String contactEmail;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private StopStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}