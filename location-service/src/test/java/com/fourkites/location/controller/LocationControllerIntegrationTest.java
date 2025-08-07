package com.fourkites.location.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourkites.location.dto.LocationDTO;
import com.fourkites.location.dto.LocationUpdateRequest;
import com.fourkites.location.dto.NearbySearchRequest;
import com.fourkites.location.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
class LocationControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private LocationService locationService;
    
    private UUID shipmentId;
    private LocationDTO testLocationDTO;
    private LocationUpdateRequest updateRequest;
    
    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        
        testLocationDTO = LocationDTO.builder()
            .id("LOC123")
            .shipmentId(shipmentId)
            .deviceId("DEVICE123")
            .latitude(40.7128)
            .longitude(-74.0060)
            .timestamp(Instant.now())
            .speed(45.5)
            .heading(180.0)
            .isMoving(true)
            .build();
        
        updateRequest = LocationUpdateRequest.builder()
            .shipmentId(shipmentId)
            .deviceId("DEVICE123")
            .latitude(40.7128)
            .longitude(-74.0060)
            .timestamp(Instant.now())
            .speed(45.5)
            .heading(180.0)
            .accuracy(10.0)
            .build();
    }
    
    @Test
    void updateLocation_Success() throws Exception {
        when(locationService.updateLocation(any(LocationUpdateRequest.class)))
            .thenReturn(testLocationDTO);
        
        mockMvc.perform(post("/api/v1/locations/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("LOC123"))
            .andExpect(jsonPath("$.shipmentId").value(shipmentId.toString()))
            .andExpect(jsonPath("$.latitude").value(40.7128))
            .andExpect(jsonPath("$.longitude").value(-74.0060));
        
        verify(locationService).updateLocation(any(LocationUpdateRequest.class));
    }
    
    @Test
    void updateLocation_InvalidData_ReturnsBadRequest() throws Exception {
        updateRequest.setLatitude(null);
        
        mockMvc.perform(post("/api/v1/locations/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void getLatestLocation_Success() throws Exception {
        when(locationService.getLatestLocation(shipmentId))
            .thenReturn(testLocationDTO);
        
        mockMvc.perform(get("/api/v1/locations/shipment/{shipmentId}/latest", shipmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("LOC123"))
            .andExpect(jsonPath("$.shipmentId").value(shipmentId.toString()));
        
        verify(locationService).getLatestLocation(shipmentId);
    }
    
    @Test
    void getLocationHistory_Success() throws Exception {
        Instant startTime = Instant.parse("2024-01-01T00:00:00Z");
        Instant endTime = Instant.parse("2024-01-02T00:00:00Z");
        
        when(locationService.getLocationHistory(eq(shipmentId), any(Instant.class), any(Instant.class)))
            .thenReturn(Arrays.asList(testLocationDTO));
        
        mockMvc.perform(get("/api/v1/locations/shipment/{shipmentId}/history", shipmentId)
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("LOC123"));
        
        verify(locationService).getLocationHistory(eq(shipmentId), any(Instant.class), any(Instant.class));
    }
    
    @Test
    void getLocationsPaginated_Success() throws Exception {
        PageRequest pageable = PageRequest.of(0, 50);
        PageImpl<LocationDTO> page = new PageImpl<>(Arrays.asList(testLocationDTO), pageable, 1);
        
        when(locationService.getLocationsPaginated(eq(shipmentId), any()))
            .thenReturn(page);
        
        mockMvc.perform(get("/api/v1/locations/shipment/{shipmentId}", shipmentId)
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("LOC123"))
            .andExpect(jsonPath("$.totalElements").value(1));
        
        verify(locationService).getLocationsPaginated(eq(shipmentId), any());
    }
    
    @Test
    void findNearbyLocations_Success() throws Exception {
        NearbySearchRequest request = NearbySearchRequest.builder()
            .latitude(40.7128)
            .longitude(-74.0060)
            .radiusMeters(5000.0)
            .maxResults(10)
            .build();
        
        when(locationService.findNearbyLocations(any(NearbySearchRequest.class)))
            .thenReturn(Arrays.asList(testLocationDTO));
        
        mockMvc.perform(post("/api/v1/locations/search/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("LOC123"));
        
        verify(locationService).findNearbyLocations(any(NearbySearchRequest.class));
    }
    
    @Test
    void processGeofenceEvent_Success() throws Exception {
        String geofenceId = "GEO123";
        String eventType = "ENTER";
        
        doNothing().when(locationService).processGeofenceEvent(shipmentId, geofenceId, eventType);
        
        mockMvc.perform(post("/api/v1/locations/shipment/{shipmentId}/geofence", shipmentId)
                .param("geofenceId", geofenceId)
                .param("eventType", eventType))
            .andExpect(status().isNoContent());
        
        verify(locationService).processGeofenceEvent(shipmentId, geofenceId, eventType);
    }
    
    @Test
    void getMovingVehicles_Success() throws Exception {
        when(locationService.getMovingVehicles(30))
            .thenReturn(Arrays.asList(testLocationDTO));
        
        mockMvc.perform(get("/api/v1/locations/moving")
                .param("lastMinutes", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("LOC123"))
            .andExpect(jsonPath("$[0].isMoving").value(true));
        
        verify(locationService).getMovingVehicles(30);
    }
    
    @Test
    void enrichLocation_Success() throws Exception {
        String locationId = "LOC123";
        
        when(locationService.enrichLocationWithAddress(locationId))
            .thenReturn(testLocationDTO);
        
        mockMvc.perform(put("/api/v1/locations/{locationId}/enrich", locationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("LOC123"));
        
        verify(locationService).enrichLocationWithAddress(locationId);
    }
    
    @Test
    void deleteLocationHistory_Success() throws Exception {
        doNothing().when(locationService).deleteLocationHistory(shipmentId);
        
        mockMvc.perform(delete("/api/v1/locations/shipment/{shipmentId}", shipmentId))
            .andExpect(status().isNoContent());
        
        verify(locationService).deleteLocationHistory(shipmentId);
    }
    
    @Test
    void cleanupStaleLocations_Success() throws Exception {
        long deletedCount = 100L;
        int staleThresholdHours = 72;
        
        when(locationService.cleanupStaleLocations(staleThresholdHours))
            .thenReturn(deletedCount);
        
        mockMvc.perform(post("/api/v1/locations/cleanup")
                .param("staleThresholdHours", String.valueOf(staleThresholdHours)))
            .andExpect(status().isOk())
            .andExpect(content().string("100"));
        
        verify(locationService).cleanupStaleLocations(staleThresholdHours);
    }
    
    @Test
    void archiveOldData_Success() throws Exception {
        int daysToKeep = 30;
        
        doNothing().when(locationService).archiveOldLocationData(daysToKeep);
        
        mockMvc.perform(post("/api/v1/locations/archive")
                .param("daysToKeep", String.valueOf(daysToKeep)))
            .andExpect(status().isNoContent());
        
        verify(locationService).archiveOldLocationData(daysToKeep);
    }
}