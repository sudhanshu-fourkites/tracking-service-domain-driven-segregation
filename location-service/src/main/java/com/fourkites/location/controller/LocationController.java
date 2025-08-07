package com.fourkites.location.controller;

import com.fourkites.location.dto.*;
import com.fourkites.location.service.LocationService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Location Management", description = "APIs for managing location tracking")
public class LocationController {
    
    private final LocationService locationService;
    
    @PostMapping("/update")
    @Operation(summary = "Update location for a shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Location updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid location data")
    })
    public ResponseEntity<LocationDTO> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        log.info("Updating location for shipment: {}", request.getShipmentId());
        LocationDTO updated = locationService.updateLocation(request);
        return ResponseEntity.ok(updated);
    }
    
    @GetMapping("/shipment/{shipmentId}/latest")
    @Operation(summary = "Get latest location for a shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Location found"),
        @ApiResponse(responseCode = "404", description = "No location found for shipment")
    })
    public ResponseEntity<LocationDTO> getLatestLocation(@PathVariable UUID shipmentId) {
        log.debug("Getting latest location for shipment: {}", shipmentId);
        LocationDTO location = locationService.getLatestLocation(shipmentId);
        return ResponseEntity.ok(location);
    }
    
    @GetMapping("/shipment/{shipmentId}/history")
    @Operation(summary = "Get location history for a shipment")
    @ApiResponse(responseCode = "200", description = "Location history retrieved")
    public ResponseEntity<List<LocationDTO>> getLocationHistory(
            @PathVariable UUID shipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        log.debug("Getting location history for shipment {} between {} and {}", 
                 shipmentId, startTime, endTime);
        List<LocationDTO> history = locationService.getLocationHistory(shipmentId, startTime, endTime);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "Get paginated locations for a shipment")
    @ApiResponse(responseCode = "200", description = "Locations retrieved")
    public ResponseEntity<Page<LocationDTO>> getLocationsPaginated(
            @PathVariable UUID shipmentId,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Getting paginated locations for shipment: {}", shipmentId);
        Page<LocationDTO> locations = locationService.getLocationsPaginated(shipmentId, pageable);
        return ResponseEntity.ok(locations);
    }
    
    @PostMapping("/search/nearby")
    @Operation(summary = "Find locations near a point")
    @ApiResponse(responseCode = "200", description = "Nearby locations found")
    public ResponseEntity<List<LocationDTO>> findNearbyLocations(
            @Valid @RequestBody NearbySearchRequest request) {
        log.debug("Searching for locations near {}, {}", request.getLatitude(), request.getLongitude());
        List<LocationDTO> locations = locationService.findNearbyLocations(request);
        return ResponseEntity.ok(locations);
    }
    
    @GetMapping("/shipment/{shipmentId}/history/daily")
    @Operation(summary = "Get daily location history")
    @ApiResponse(responseCode = "200", description = "Daily history retrieved")
    public ResponseEntity<LocationHistoryDTO> getDailyHistory(
            @PathVariable UUID shipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String dateStr = date.toString();
        LocationHistoryDTO history = locationService.getLocationHistoryByDate(shipmentId, dateStr);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/shipment/{shipmentId}/history/range")
    @Operation(summary = "Get location history for date range")
    @ApiResponse(responseCode = "200", description = "History range retrieved")
    public ResponseEntity<List<LocationHistoryDTO>> getHistoryRange(
            @PathVariable UUID shipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<LocationHistoryDTO> history = locationService.getLocationHistoryRange(
            shipmentId, startDate.toString(), endDate.toString());
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/shipment/{shipmentId}/geofence")
    @Operation(summary = "Process geofence event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Geofence event processed"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<Void> processGeofenceEvent(
            @PathVariable UUID shipmentId,
            @RequestParam String geofenceId,
            @RequestParam String eventType) {
        log.info("Processing geofence event for shipment {}: {} - {}", 
                shipmentId, geofenceId, eventType);
        locationService.processGeofenceEvent(shipmentId, geofenceId, eventType);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/moving")
    @Operation(summary = "Get currently moving vehicles")
    @ApiResponse(responseCode = "200", description = "Moving vehicles retrieved")
    public ResponseEntity<List<LocationDTO>> getMovingVehicles(
            @RequestParam(defaultValue = "30") int lastMinutes) {
        log.debug("Getting vehicles moving in last {} minutes", lastMinutes);
        List<LocationDTO> movingVehicles = locationService.getMovingVehicles(lastMinutes);
        return ResponseEntity.ok(movingVehicles);
    }
    
    @PutMapping("/{locationId}/enrich")
    @Operation(summary = "Enrich location with address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Location enriched"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<LocationDTO> enrichLocation(@PathVariable String locationId) {
        log.debug("Enriching location: {}", locationId);
        LocationDTO enriched = locationService.enrichLocationWithAddress(locationId);
        return ResponseEntity.ok(enriched);
    }
    
    @DeleteMapping("/shipment/{shipmentId}")
    @Operation(summary = "Delete location history for a shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "History deleted"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> deleteLocationHistory(@PathVariable UUID shipmentId) {
        log.info("Deleting location history for shipment: {}", shipmentId);
        locationService.deleteLocationHistory(shipmentId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "Clean up stale locations")
    @ApiResponse(responseCode = "200", description = "Cleanup completed")
    public ResponseEntity<Long> cleanupStaleLocations(
            @RequestParam(defaultValue = "72") int staleThresholdHours) {
        log.info("Cleaning up locations older than {} hours", staleThresholdHours);
        long deletedCount = locationService.cleanupStaleLocations(staleThresholdHours);
        return ResponseEntity.ok(deletedCount);
    }
    
    @PostMapping("/archive")
    @Operation(summary = "Archive old location data")
    @ApiResponse(responseCode = "204", description = "Archive completed")
    public ResponseEntity<Void> archiveOldData(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        log.info("Archiving location data older than {} days", daysToKeep);
        locationService.archiveOldLocationData(daysToKeep);
        return ResponseEntity.noContent().build();
    }
}