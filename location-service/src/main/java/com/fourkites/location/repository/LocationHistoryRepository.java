package com.fourkites.location.repository;

import com.fourkites.location.domain.LocationHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationHistoryRepository extends MongoRepository<LocationHistory, String> {
    
    Optional<LocationHistory> findByShipmentIdAndDate(UUID shipmentId, String date);
    
    List<LocationHistory> findByShipmentIdAndDateBetween(UUID shipmentId, String startDate, String endDate);
    
    List<LocationHistory> findByShipmentIdOrderByDateDesc(UUID shipmentId);
    
    @Query(value = "{ 'shipmentId': ?0 }", delete = true)
    void deleteByShipmentId(UUID shipmentId);
    
    @Query("{ 'date': { $lt: ?0 } }")
    List<LocationHistory> findOlderThan(String date);
    
    @Query(value = "{ 'date': { $lt: ?0 } }", delete = true)
    long deleteOlderThan(String date);
    
    @Query("{ 'shipmentId': ?0, 'statistics.totalDistance': { $gte: ?1 } }")
    List<LocationHistory> findByShipmentIdAndMinDistance(UUID shipmentId, double minDistance);
    
    long countByShipmentId(UUID shipmentId);
}