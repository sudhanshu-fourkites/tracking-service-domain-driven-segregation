package com.fourkites.location.repository;

import com.fourkites.location.domain.Geofence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends MongoRepository<Geofence, String> {
    
    Optional<Geofence> findByName(String name);
    
    List<Geofence> findByCustomerId(String customerId);
    
    Page<Geofence> findByCustomerId(String customerId, Pageable pageable);
    
    List<Geofence> findByActiveTrue();
    
    List<Geofence> findByType(String type);
    
    @Query("{ 'boundary': { $geoIntersects: { $geometry: { type: 'Point', coordinates: [?0, ?1] } } } }")
    List<Geofence> findContainingPoint(double longitude, double latitude);
    
    @Query("{ 'centerLongitude': { $gte: ?0, $lte: ?2 }, 'centerLatitude': { $gte: ?1, $lte: ?3 }, 'active': true }")
    List<Geofence> findActiveInBoundingBox(double minLon, double minLat, double maxLon, double maxLat);
    
    @Query("{ 'tags': { $in: ?0 }, 'active': true }")
    List<Geofence> findActiveByTags(List<String> tags);
    
    List<Geofence> findByCustomerIdAndActiveTrue(String customerId);
    
    long countByCustomerIdAndActiveTrue(String customerId);
    
    @Query("{ 'type': 'CIRCULAR', 'centerLongitude': ?0, 'centerLatitude': ?1, 'radiusMeters': { $lte: ?2 } }")
    List<Geofence> findCircularNearby(double longitude, double latitude, double maxRadius);
    
    boolean existsByNameAndCustomerId(String name, String customerId);
}