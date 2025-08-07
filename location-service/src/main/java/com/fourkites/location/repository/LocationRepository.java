package com.fourkites.location.repository;

import com.fourkites.location.domain.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends MongoRepository<Location, String> {
    
    Optional<Location> findTopByShipmentIdOrderByTimestampDesc(UUID shipmentId);
    
    List<Location> findByShipmentIdAndTimestampBetweenOrderByTimestampAsc(
        UUID shipmentId, Instant startTime, Instant endTime);
    
    Page<Location> findByShipmentId(UUID shipmentId, Pageable pageable);
    
    List<Location> findByDeviceIdAndTimestampAfterOrderByTimestampDesc(
        String deviceId, Instant after);
    
    @Query("{ 'position': { $near: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
    List<Location> findNearby(double longitude, double latitude, double maxDistanceMeters);
    
    List<Location> findByPositionNear(Point point, Distance distance);
    
    @Query("{ 'shipmentId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Location> findLocationHistory(UUID shipmentId, Instant startTime, Instant endTime);
    
    @Query("{ 'carrierId': ?0, 'timestamp': { $gte: ?1 } }")
    List<Location> findByCarrierIdAfter(String carrierId, Instant after);
    
    @Query("{ 'geofenceId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Location> findByGeofenceInTimeRange(String geofenceId, Instant startTime, Instant endTime);
    
    @Query(value = "{ 'shipmentId': ?0 }", delete = true)
    void deleteByShipmentId(UUID shipmentId);
    
    @Query("{ 'timestamp': { $lt: ?0 } }")
    List<Location> findStaleLocations(Instant before);
    
    @Query(value = "{ 'timestamp': { $lt: ?0 } }", delete = true)
    long deleteStaleLocations(Instant before);
    
    long countByShipmentIdAndTimestampAfter(UUID shipmentId, Instant after);
    
    @Query("{ 'shipmentId': { $in: ?0 }, 'timestamp': { $gte: ?1 } }")
    List<Location> findLatestForShipments(List<UUID> shipmentIds, Instant after);
    
    @Query("{ 'isMoving': true, 'timestamp': { $gte: ?0 } }")
    List<Location> findMovingVehicles(Instant since);
    
    @Query("{ 'stopId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Location> findByStopIdInTimeRange(String stopId, Instant startTime, Instant endTime);
}