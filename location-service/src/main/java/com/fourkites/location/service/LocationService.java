package com.fourkites.location.service;

import com.fourkites.location.dto.LocationDTO;
import com.fourkites.location.dto.LocationHistoryDTO;
import com.fourkites.location.dto.LocationUpdateRequest;
import com.fourkites.location.dto.NearbySearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LocationService {
    
    LocationDTO updateLocation(LocationUpdateRequest request);
    
    LocationDTO getLatestLocation(UUID shipmentId);
    
    List<LocationDTO> getLocationHistory(UUID shipmentId, Instant startTime, Instant endTime);
    
    Page<LocationDTO> getLocationsPaginated(UUID shipmentId, Pageable pageable);
    
    List<LocationDTO> findNearbyLocations(NearbySearchRequest request);
    
    LocationHistoryDTO getLocationHistoryByDate(UUID shipmentId, String date);
    
    List<LocationHistoryDTO> getLocationHistoryRange(UUID shipmentId, String startDate, String endDate);
    
    void processGeofenceEvent(UUID shipmentId, String geofenceId, String eventType);
    
    void deleteLocationHistory(UUID shipmentId);
    
    long cleanupStaleLocations(int staleThresholdHours);
    
    List<LocationDTO> getMovingVehicles(int lastMinutes);
    
    LocationDTO enrichLocationWithAddress(String locationId);
    
    void archiveOldLocationData(int daysToKeep);
}