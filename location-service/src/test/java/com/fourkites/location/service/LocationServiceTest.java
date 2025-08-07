package com.fourkites.location.service;

import com.fourkites.location.domain.*;
import com.fourkites.location.dto.*;
import com.fourkites.location.event.LocationEventPublisher;
import com.fourkites.location.exception.InvalidLocationDataException;
import com.fourkites.location.exception.LocationNotFoundException;
import com.fourkites.location.repository.LocationHistoryRepository;
import com.fourkites.location.repository.LocationRepository;
import com.fourkites.location.service.impl.LocationMapper;
import com.fourkites.location.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    
    @Mock
    private LocationRepository locationRepository;
    
    @Mock
    private LocationHistoryRepository historyRepository;
    
    @Mock
    private LocationEventPublisher eventPublisher;
    
    @Mock
    private GeocodeService geocodeService;
    
    @Mock
    private LocationMapper locationMapper;
    
    @InjectMocks
    private LocationServiceImpl locationService;
    
    private UUID shipmentId;
    private Location testLocation;
    private LocationDTO testLocationDTO;
    private LocationUpdateRequest updateRequest;
    
    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        
        testLocation = Location.create(
            shipmentId,
            "DEVICE123",
            40.7128,
            -74.0060,
            Instant.now()
        );
        testLocation.setId("LOC123");
        
        testLocationDTO = LocationDTO.builder()
            .id("LOC123")
            .shipmentId(shipmentId)
            .deviceId("DEVICE123")
            .latitude(40.7128)
            .longitude(-74.0060)
            .timestamp(testLocation.getTimestamp())
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
    void updateLocation_Success() {
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);
        when(historyRepository.findByShipmentIdAndDate(any(), any()))
            .thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenReturn(null);
        when(locationMapper.toDTO(any())).thenReturn(testLocationDTO);
        
        LocationDTO result = locationService.updateLocation(updateRequest);
        
        assertThat(result).isNotNull();
        assertThat(result.getShipmentId()).isEqualTo(shipmentId);
        
        verify(locationRepository).save(any(Location.class));
        verify(eventPublisher).publishLocationUpdate(any(Location.class));
    }
    
    @Test
    void updateLocation_InvalidLatitude_ThrowsException() {
        updateRequest.setLatitude(91.0);
        
        assertThatThrownBy(() -> locationService.updateLocation(updateRequest))
            .isInstanceOf(InvalidLocationDataException.class)
            .hasMessageContaining("Invalid latitude");
    }
    
    @Test
    void updateLocation_InvalidLongitude_ThrowsException() {
        updateRequest.setLongitude(181.0);
        
        assertThatThrownBy(() -> locationService.updateLocation(updateRequest))
            .isInstanceOf(InvalidLocationDataException.class)
            .hasMessageContaining("Invalid longitude");
    }
    
    @Test
    void updateLocation_NullShipmentId_ThrowsException() {
        updateRequest.setShipmentId(null);
        
        assertThatThrownBy(() -> locationService.updateLocation(updateRequest))
            .isInstanceOf(InvalidLocationDataException.class)
            .hasMessageContaining("Shipment ID is required");
    }
    
    @Test
    void getLatestLocation_Found() {
        when(locationRepository.findTopByShipmentIdOrderByTimestampDesc(shipmentId))
            .thenReturn(Optional.of(testLocation));
        when(locationMapper.toDTO(testLocation)).thenReturn(testLocationDTO);
        
        LocationDTO result = locationService.getLatestLocation(shipmentId);
        
        assertThat(result).isNotNull();
        assertThat(result.getShipmentId()).isEqualTo(shipmentId);
        
        verify(locationRepository).findTopByShipmentIdOrderByTimestampDesc(shipmentId);
    }
    
    @Test
    void getLatestLocation_NotFound_ThrowsException() {
        when(locationRepository.findTopByShipmentIdOrderByTimestampDesc(shipmentId))
            .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> locationService.getLatestLocation(shipmentId))
            .isInstanceOf(LocationNotFoundException.class)
            .hasMessageContaining("No location found for shipment");
    }
    
    @Test
    void getLocationHistory_Success() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now();
        List<Location> locations = Arrays.asList(testLocation);
        
        when(locationRepository.findByShipmentIdAndTimestampBetweenOrderByTimestampAsc(
            shipmentId, startTime, endTime)).thenReturn(locations);
        when(locationMapper.toDTO(any())).thenReturn(testLocationDTO);
        
        List<LocationDTO> result = locationService.getLocationHistory(shipmentId, startTime, endTime);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShipmentId()).isEqualTo(shipmentId);
    }
    
    @Test
    void getLocationHistory_InvalidTimeRange_ThrowsException() {
        Instant startTime = Instant.now();
        Instant endTime = Instant.now().minus(1, ChronoUnit.HOURS);
        
        assertThatThrownBy(() -> locationService.getLocationHistory(shipmentId, startTime, endTime))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End time must be after start time");
    }
    
    @Test
    void getLocationsPaginated_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Location> locationPage = new PageImpl<>(Arrays.asList(testLocation));
        
        when(locationRepository.findByShipmentId(shipmentId, pageable))
            .thenReturn(locationPage);
        when(locationMapper.toDTO(any())).thenReturn(testLocationDTO);
        
        Page<LocationDTO> result = locationService.getLocationsPaginated(shipmentId, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getShipmentId()).isEqualTo(shipmentId);
    }
    
    @Test
    void findNearbyLocations_Success() {
        NearbySearchRequest request = NearbySearchRequest.builder()
            .latitude(40.7128)
            .longitude(-74.0060)
            .radiusMeters(5000.0)
            .maxResults(10)
            .build();
        
        Point searchPoint = new Point(-74.0060, 40.7128);
        Distance distance = new Distance(5000.0, Metrics.METERS);
        
        when(locationRepository.findByPositionNear(searchPoint, distance))
            .thenReturn(Arrays.asList(testLocation));
        when(locationMapper.toDTO(any())).thenReturn(testLocationDTO);
        
        List<LocationDTO> result = locationService.findNearbyLocations(request);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLatitude()).isEqualTo(40.7128);
    }
    
    @Test
    void processGeofenceEvent_Enter_Success() {
        String geofenceId = "GEO123";
        
        when(locationRepository.findTopByShipmentIdOrderByTimestampDesc(shipmentId))
            .thenReturn(Optional.of(testLocation));
        when(locationMapper.toDTO(testLocation)).thenReturn(testLocationDTO);
        when(locationRepository.findById(testLocationDTO.getId()))
            .thenReturn(Optional.of(testLocation));
        when(locationRepository.save(any())).thenReturn(testLocation);
        
        locationService.processGeofenceEvent(shipmentId, geofenceId, "ENTER");
        
        verify(locationRepository).save(testLocation);
        verify(eventPublisher).publishGeofenceEvent(shipmentId, geofenceId, "ENTER");
    }
    
    @Test
    void deleteLocationHistory_Success() {
        doNothing().when(locationRepository).deleteByShipmentId(shipmentId);
        doNothing().when(historyRepository).deleteByShipmentId(shipmentId);
        
        locationService.deleteLocationHistory(shipmentId);
        
        verify(locationRepository).deleteByShipmentId(shipmentId);
        verify(historyRepository).deleteByShipmentId(shipmentId);
    }
    
    @Test
    void cleanupStaleLocations_Success() {
        int staleThresholdHours = 72;
        long expectedDeleted = 100L;
        
        when(locationRepository.deleteStaleLocations(any(Instant.class)))
            .thenReturn(expectedDeleted);
        
        long result = locationService.cleanupStaleLocations(staleThresholdHours);
        
        assertThat(result).isEqualTo(expectedDeleted);
        verify(locationRepository).deleteStaleLocations(any(Instant.class));
    }
    
    @Test
    void getMovingVehicles_Success() {
        int lastMinutes = 30;
        
        when(locationRepository.findMovingVehicles(any(Instant.class)))
            .thenReturn(Arrays.asList(testLocation));
        when(locationMapper.toDTO(any())).thenReturn(testLocationDTO);
        
        List<LocationDTO> result = locationService.getMovingVehicles(lastMinutes);
        
        assertThat(result).hasSize(1);
        verify(locationRepository).findMovingVehicles(any(Instant.class));
    }
    
    @Test
    void enrichLocationWithAddress_Success() {
        String locationId = "LOC123";
        Address address = Address.builder()
            .formattedAddress("123 Main St, New York, NY")
            .city("New York")
            .state("NY")
            .country("USA")
            .build();
        
        when(locationRepository.findById(locationId))
            .thenReturn(Optional.of(testLocation));
        when(geocodeService.reverseGeocode(testLocation.getLatitude(), testLocation.getLongitude()))
            .thenReturn(address);
        when(locationRepository.save(any())).thenReturn(testLocation);
        when(locationMapper.toDTO(any())).thenReturn(testLocationDTO);
        
        LocationDTO result = locationService.enrichLocationWithAddress(locationId);
        
        assertThat(result).isNotNull();
        verify(geocodeService).reverseGeocode(testLocation.getLatitude(), testLocation.getLongitude());
        verify(locationRepository).save(testLocation);
    }
    
    @Test
    void enrichLocationWithAddress_LocationNotFound_ThrowsException() {
        String locationId = "INVALID";
        
        when(locationRepository.findById(locationId))
            .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> locationService.enrichLocationWithAddress(locationId))
            .isInstanceOf(LocationNotFoundException.class)
            .hasMessageContaining("Location not found");
    }
    
    @Test
    void archiveOldLocationData_Success() {
        int daysToKeep = 30;
        LocationHistory history = LocationHistory.createDaily(shipmentId, "2024-01-01");
        
        when(historyRepository.findOlderThan(any()))
            .thenReturn(Arrays.asList(history));
        when(historyRepository.save(any())).thenReturn(history);
        when(locationRepository.deleteStaleLocations(any()))
            .thenReturn(50L);
        
        locationService.archiveOldLocationData(daysToKeep);
        
        verify(historyRepository).save(history);
        verify(locationRepository).deleteStaleLocations(any(Instant.class));
    }
}