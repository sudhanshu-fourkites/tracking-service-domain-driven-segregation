package com.fourkites.location.service.impl;

import com.fourkites.location.domain.*;
import com.fourkites.location.dto.*;
import com.fourkites.location.event.LocationEventPublisher;
import com.fourkites.location.exception.LocationNotFoundException;
import com.fourkites.location.exception.InvalidLocationDataException;
import com.fourkites.location.repository.LocationHistoryRepository;
import com.fourkites.location.repository.LocationRepository;
import com.fourkites.location.service.GeocodeService;
import com.fourkites.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LocationServiceImpl implements LocationService {
    
    private final LocationRepository locationRepository;
    private final LocationHistoryRepository historyRepository;
    private final LocationEventPublisher eventPublisher;
    private final GeocodeService geocodeService;
    private final LocationMapper locationMapper;
    
    private static final int MAX_HISTORY_POINTS_PER_DAY = 1000;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    @CacheEvict(value = "latest-locations", key = "#request.shipmentId")
    public LocationDTO updateLocation(LocationUpdateRequest request) {
        log.debug("Updating location for shipment: {}", request.getShipmentId());
        
        validateLocationRequest(request);
        
        Location location = Location.create(
            request.getShipmentId(),
            request.getDeviceId(),
            request.getLatitude(),
            request.getLongitude(),
            request.getTimestamp()
        );
        
        enrichLocationData(location, request);
        
        Location saved = locationRepository.save(location);
        
        updateLocationHistory(saved);
        
        eventPublisher.publishLocationUpdate(saved);
        
        return locationMapper.toDTO(saved);
    }
    
    @Override
    @Cacheable(value = "latest-locations", key = "#shipmentId")
    public LocationDTO getLatestLocation(UUID shipmentId) {
        log.debug("Getting latest location for shipment: {}", shipmentId);
        
        return locationRepository.findTopByShipmentIdOrderByTimestampDesc(shipmentId)
            .map(locationMapper::toDTO)
            .orElseThrow(() -> new LocationNotFoundException("No location found for shipment: " + shipmentId));
    }
    
    @Override
    public List<LocationDTO> getLocationHistory(UUID shipmentId, Instant startTime, Instant endTime) {
        log.debug("Getting location history for shipment {} between {} and {}", 
                 shipmentId, startTime, endTime);
        
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        List<Location> locations = locationRepository
            .findByShipmentIdAndTimestampBetweenOrderByTimestampAsc(shipmentId, startTime, endTime);
        
        return locations.stream()
            .map(locationMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<LocationDTO> getLocationsPaginated(UUID shipmentId, Pageable pageable) {
        Page<Location> locations = locationRepository.findByShipmentId(shipmentId, pageable);
        return locations.map(locationMapper::toDTO);
    }
    
    @Override
    public List<LocationDTO> findNearbyLocations(NearbySearchRequest request) {
        log.debug("Finding locations near {}, {} within {} meters", 
                 request.getLatitude(), request.getLongitude(), request.getRadiusMeters());
        
        Point searchPoint = new Point(request.getLongitude(), request.getLatitude());
        Distance distance = new Distance(request.getRadiusMeters(), Metrics.METERS);
        
        List<Location> nearbyLocations = locationRepository.findByPositionNear(searchPoint, distance);
        
        return nearbyLocations.stream()
            .filter(loc -> {
                if (request.getMinTimestamp() != null) {
                    return !loc.getTimestamp().isBefore(request.getMinTimestamp());
                }
                return true;
            })
            .limit(request.getMaxResults() != null ? request.getMaxResults() : 100)
            .map(locationMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public LocationHistoryDTO getLocationHistoryByDate(UUID shipmentId, String date) {
        return historyRepository.findByShipmentIdAndDate(shipmentId, date)
            .map(this::convertToHistoryDTO)
            .orElse(null);
    }
    
    @Override
    public List<LocationHistoryDTO> getLocationHistoryRange(UUID shipmentId, String startDate, String endDate) {
        List<LocationHistory> histories = historyRepository
            .findByShipmentIdAndDateBetween(shipmentId, startDate, endDate);
        
        return histories.stream()
            .map(this::convertToHistoryDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public void processGeofenceEvent(UUID shipmentId, String geofenceId, String eventType) {
        log.info("Processing geofence event: shipment={}, geofence={}, event={}", 
                shipmentId, geofenceId, eventType);
        
        LocationDTO latest = getLatestLocation(shipmentId);
        
        Location location = locationRepository.findById(latest.getId())
            .orElseThrow(() -> new LocationNotFoundException("Location not found"));
        
        switch (eventType.toUpperCase()) {
            case "ENTER":
                location.enterGeofence(geofenceId);
                break;
            case "EXIT":
                location.exitGeofence(geofenceId);
                break;
            default:
                log.warn("Unknown geofence event type: {}", eventType);
        }
        
        locationRepository.save(location);
        
        eventPublisher.publishGeofenceEvent(shipmentId, geofenceId, eventType);
    }
    
    @Override
    @CacheEvict(value = "latest-locations", key = "#shipmentId")
    public void deleteLocationHistory(UUID shipmentId) {
        log.info("Deleting location history for shipment: {}", shipmentId);
        
        locationRepository.deleteByShipmentId(shipmentId);
        historyRepository.deleteByShipmentId(shipmentId);
    }
    
    @Override
    public long cleanupStaleLocations(int staleThresholdHours) {
        log.info("Cleaning up locations older than {} hours", staleThresholdHours);
        
        Instant threshold = Instant.now().minus(staleThresholdHours, ChronoUnit.HOURS);
        return locationRepository.deleteStaleLocations(threshold);
    }
    
    @Override
    public List<LocationDTO> getMovingVehicles(int lastMinutes) {
        Instant since = Instant.now().minus(lastMinutes, ChronoUnit.MINUTES);
        
        List<Location> movingVehicles = locationRepository.findMovingVehicles(since);
        
        return movingVehicles.stream()
            .map(locationMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public LocationDTO enrichLocationWithAddress(String locationId) {
        Location location = locationRepository.findById(locationId)
            .orElseThrow(() -> new LocationNotFoundException("Location not found: " + locationId));
        
        if (location.getAddress() == null) {
            Address address = geocodeService.reverseGeocode(
                location.getLatitude(), 
                location.getLongitude()
            );
            location.setAddress(address);
            locationRepository.save(location);
        }
        
        return locationMapper.toDTO(location);
    }
    
    @Override
    public void archiveOldLocationData(int daysToKeep) {
        log.info("Archiving location data older than {} days", daysToKeep);
        
        String cutoffDate = LocalDate.now()
            .minusDays(daysToKeep)
            .format(DATE_FORMAT);
        
        List<LocationHistory> oldHistories = historyRepository.findOlderThan(cutoffDate);
        
        for (LocationHistory history : oldHistories) {
            history.compressOldData(100);
            historyRepository.save(history);
        }
        
        Instant locationCutoff = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
        long deletedCount = locationRepository.deleteStaleLocations(locationCutoff);
        
        log.info("Archived {} old location records", deletedCount);
    }
    
    private void validateLocationRequest(LocationUpdateRequest request) {
        if (request.getShipmentId() == null) {
            throw new InvalidLocationDataException("Shipment ID is required");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new InvalidLocationDataException("Latitude and longitude are required");
        }
        if (request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new InvalidLocationDataException("Invalid latitude: " + request.getLatitude());
        }
        if (request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new InvalidLocationDataException("Invalid longitude: " + request.getLongitude());
        }
    }
    
    private void enrichLocationData(Location location, LocationUpdateRequest request) {
        if (request.getSpeed() != null) {
            location.setSpeed(request.getSpeed());
        }
        if (request.getHeading() != null) {
            location.setHeading(request.getHeading());
        }
        if (request.getAccuracy() != null) {
            location.setAccuracy(request.getAccuracy());
            location.setQuality(determineQuality(request.getAccuracy()));
        }
        if (request.getAltitude() != null) {
            location.setAltitude(request.getAltitude());
        }
        if (request.getMetadata() != null) {
            location.setMetadata(request.getMetadata());
        }
        
        location.setIsMoving(request.getSpeed() != null && request.getSpeed() > 0.5);
    }
    
    private LocationQuality determineQuality(Double accuracy) {
        if (accuracy == null) return LocationQuality.UNKNOWN;
        if (accuracy < 10) return LocationQuality.HIGH;
        if (accuracy < 50) return LocationQuality.STANDARD;
        return LocationQuality.LOW;
    }
    
    private void updateLocationHistory(Location location) {
        String date = location.getTimestamp()
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DATE_FORMAT);
        
        LocationHistory history = historyRepository
            .findByShipmentIdAndDate(location.getShipmentId(), date)
            .orElseGet(() -> LocationHistory.createDaily(location.getShipmentId(), date));
        
        LocationPoint point = LocationPoint.builder()
            .latitude(location.getLatitude())
            .longitude(location.getLongitude())
            .altitude(location.getAltitude())
            .speed(location.getSpeed())
            .heading(location.getHeading())
            .timestamp(location.getTimestamp())
            .source(location.getSource().name())
            .build();
        
        history.addLocation(point);
        
        if (history.getLocations().size() > MAX_HISTORY_POINTS_PER_DAY) {
            history.compressOldData(MAX_HISTORY_POINTS_PER_DAY / 2);
        }
        
        historyRepository.save(history);
    }
    
    private LocationHistoryDTO convertToHistoryDTO(LocationHistory history) {
        return LocationHistoryDTO.builder()
            .id(history.getId())
            .shipmentId(history.getShipmentId())
            .date(history.getDate())
            .locationCount(history.getLocations().size())
            .statistics(history.getStatistics())
            .build();
    }
}