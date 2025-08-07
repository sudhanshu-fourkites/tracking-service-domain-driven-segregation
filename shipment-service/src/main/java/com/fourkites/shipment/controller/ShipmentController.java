package com.fourkites.shipment.controller;

import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.dto.ShipmentDTO;
import com.fourkites.shipment.dto.StopDTO;
import com.fourkites.shipment.service.ShipmentService;
import com.fourkites.shipment.validation.PaginationValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipment Management", description = "APIs for managing shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final PaginationValidator paginationValidator;

    @PostMapping
    @Operation(summary = "Create a new shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Shipment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Shipment already exists")
    })
    public ResponseEntity<ShipmentDTO> createShipment(@Valid @RequestBody ShipmentDTO shipmentDTO) {
        log.info("Creating new shipment: {}", shipmentDTO.getShipmentNumber());
        ShipmentDTO created = shipmentService.createShipment(shipmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentDTO> updateShipment(
            @PathVariable UUID id,
            @Valid @RequestBody ShipmentDTO shipmentDTO) {
        log.info("Updating shipment: {}", id);
        ShipmentDTO updated = shipmentService.updateShipment(id, shipmentDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shipment by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment found"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentDTO> getShipment(@PathVariable UUID id) {
        log.info("Fetching shipment: {}", id);
        ShipmentDTO shipment = shipmentService.getShipment(id);
        return ResponseEntity.ok(shipment);
    }

    @GetMapping("/by-number/{shipmentNumber}")
    @Operation(summary = "Get shipment by shipment number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment found"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentDTO> getShipmentByNumber(@PathVariable String shipmentNumber) {
        log.info("Fetching shipment by number: {}", shipmentNumber);
        ShipmentDTO shipment = shipmentService.getShipmentByNumber(shipmentNumber);
        return ResponseEntity.ok(shipment);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get shipments by customer ID")
    @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully")
    public ResponseEntity<Page<ShipmentDTO>> getShipmentsByCustomer(
            @PathVariable String customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching shipments for customer: {}", customerId);
        paginationValidator.validate(pageable);
        Pageable sanitizedPageable = paginationValidator.sanitize(pageable);
        Page<ShipmentDTO> shipments = shipmentService.getShipmentsByCustomer(customerId, sanitizedPageable);
        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/carrier/{carrierId}")
    @Operation(summary = "Get shipments by carrier ID")
    @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully")
    public ResponseEntity<Page<ShipmentDTO>> getShipmentsByCarrier(
            @PathVariable String carrierId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching shipments for carrier: {}", carrierId);
        paginationValidator.validate(pageable);
        Pageable sanitizedPageable = paginationValidator.sanitize(pageable);
        Page<ShipmentDTO> shipments = shipmentService.getShipmentsByCarrier(carrierId, sanitizedPageable);
        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get shipments by status")
    @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully")
    public ResponseEntity<Page<ShipmentDTO>> getShipmentsByStatus(
            @PathVariable ShipmentStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching shipments with status: {}", status);
        paginationValidator.validate(pageable);
        Pageable sanitizedPageable = paginationValidator.sanitize(pageable);
        Page<ShipmentDTO> shipments = shipmentService.getShipmentsByStatus(status, sanitizedPageable);
        return ResponseEntity.ok(shipments);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update shipment status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> updateShipmentStatus(
            @PathVariable UUID id,
            @RequestParam ShipmentStatus status) {
        log.info("Updating status for shipment {} to {}", id, status);
        shipmentService.updateShipmentStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Shipment cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel shipment"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> cancelShipment(
            @PathVariable UUID id,
            @RequestParam String reason) {
        log.info("Cancelling shipment: {}", id);
        shipmentService.cancelShipment(id, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stops")
    @Operation(summary = "Add a stop to shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stop added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentDTO> addStop(
            @PathVariable UUID id,
            @Valid @RequestBody StopDTO stopDTO) {
        log.info("Adding stop to shipment: {}", id);
        ShipmentDTO updated = shipmentService.addStop(id, stopDTO);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{shipmentId}/stops/{stopId}")
    @Operation(summary = "Update a stop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Stop updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Shipment or stop not found")
    })
    public ResponseEntity<Void> updateStop(
            @PathVariable UUID shipmentId,
            @PathVariable UUID stopId,
            @Valid @RequestBody StopDTO stopDTO) {
        log.info("Updating stop {} for shipment: {}", stopId, shipmentId);
        shipmentService.updateStop(shipmentId, stopId, stopDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shipmentId}/stops/{stopId}")
    @Operation(summary = "Remove a stop from shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Stop removed successfully"),
        @ApiResponse(responseCode = "404", description = "Shipment or stop not found")
    })
    public ResponseEntity<Void> removeStop(
            @PathVariable UUID shipmentId,
            @PathVariable UUID stopId) {
        log.info("Removing stop {} from shipment: {}", stopId, shipmentId);
        shipmentService.removeStop(shipmentId, stopId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/delivery-window")
    @Operation(summary = "Get shipments for delivery within time window")
    @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully")
    public ResponseEntity<List<ShipmentDTO>> getShipmentsForDelivery(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("Fetching shipments for delivery between {} and {}", start, end);
        List<ShipmentDTO> shipments = shipmentService.getShipmentsForDelivery(start, end);
        return ResponseEntity.ok(shipments);
    }

    @PatchMapping("/{id}/estimated-delivery")
    @Operation(summary = "Update estimated delivery time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Estimated delivery updated"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> updateEstimatedDelivery(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime estimatedDelivery) {
        log.info("Updating estimated delivery for shipment: {}", id);
        shipmentService.updateEstimatedDelivery(id, estimatedDelivery);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Mark shipment as delivered")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Shipment marked as delivered"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> markAsDelivered(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deliveryTime) {
        log.info("Marking shipment {} as delivered", id);
        shipmentService.markAsDelivered(id, deliveryTime);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/location")
    @Operation(summary = "Update shipment location")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Location updated"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> updateLocation(
            @PathVariable UUID id,
            @RequestBody Map<String, Double> location) {
        log.info("Updating location for shipment: {}", id);
        shipmentService.processLocationUpdate(id, location.get("latitude"), location.get("longitude"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find shipments near location")
    @ApiResponse(responseCode = "200", description = "Nearby shipments retrieved")
    public ResponseEntity<List<ShipmentDTO>> findNearbyShipments(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50") Double radiusKm) {
        log.info("Finding shipments near {}, {} within {}km", latitude, longitude, radiusKm);
        List<ShipmentDTO> shipments = shipmentService.findNearbyShipments(latitude, longitude, radiusKm);
        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/search")
    @Operation(summary = "Search shipments")
    @ApiResponse(responseCode = "200", description = "Search results retrieved")
    public ResponseEntity<Page<ShipmentDTO>> searchShipments(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching shipments with query: {}", query);
        Page<ShipmentDTO> results = shipmentService.searchShipments(query, pageable);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Shipment deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> deleteShipment(@PathVariable UUID id) {
        log.info("Deleting shipment: {}", id);
        shipmentService.deleteShipment(id);
        return ResponseEntity.noContent().build();
    }
}