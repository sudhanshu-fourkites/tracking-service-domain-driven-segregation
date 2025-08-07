package com.fourkites.shipment.repository;

import com.fourkites.shipment.domain.Shipment;
import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.repository.projection.ShipmentSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    Page<Shipment> findByCustomerId(String customerId, Pageable pageable);

    Page<Shipment> findByCarrierId(String carrierId, Pageable pageable);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);

    @Query("SELECT s FROM Shipment s WHERE s.plannedDeliveryTime BETWEEN :start AND :end")
    List<Shipment> findByPlannedDeliveryTimeBetween(@Param("start") LocalDateTime start, 
                                                     @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Shipment s WHERE s.status = :status AND s.updatedAt < :cutoffTime")
    List<Shipment> findStaleShipments(@Param("status") ShipmentStatus status, 
                                      @Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT s FROM Shipment s WHERE s.customerId = :customerId AND s.status IN :statuses")
    Page<Shipment> findByCustomerIdAndStatusIn(@Param("customerId") String customerId,
                                               @Param("statuses") List<ShipmentStatus> statuses,
                                               Pageable pageable);

    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.stops WHERE s.id = :id")
    Optional<Shipment> findByIdWithStops(@Param("id") UUID id);

    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.events WHERE s.id = :id")
    Optional<Shipment> findByIdWithEvents(@Param("id") UUID id);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.customerId = :customerId AND s.status = :status")
    Long countByCustomerIdAndStatus(@Param("customerId") String customerId, 
                                    @Param("status") ShipmentStatus status);

    @Modifying
    @Query("UPDATE Shipment s SET s.status = :status, s.updatedAt = :updatedAt WHERE s.id = :id")
    int updateStatus(@Param("id") UUID id, 
                    @Param("status") ShipmentStatus status, 
                    @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT s FROM Shipment s WHERE s.temperatureControlled = true AND s.status = 'IN_TRANSIT'")
    List<Shipment> findActiveTemperatureControlledShipments();

    @Query("SELECT s FROM Shipment s WHERE s.hazmat = true AND s.status IN ('IN_TRANSIT', 'DISPATCHED')")
    List<Shipment> findActiveHazmatShipments();

    @Query(value = "SELECT * FROM shipments s WHERE " +
           "ST_DWithin(ST_MakePoint(CAST(s.dest_longitude AS FLOAT), CAST(s.dest_latitude AS FLOAT))::geography, " +
           "ST_MakePoint(CAST(?2 AS FLOAT), CAST(?1 AS FLOAT))::geography, ?3)", 
           nativeQuery = true)
    List<Shipment> findShipmentsNearLocation(double latitude,
                                             double longitude,
                                             double radiusMeters);

    boolean existsByShipmentNumber(String shipmentNumber);

    @Query("SELECT DISTINCT s.carrierId FROM Shipment s WHERE s.customerId = :customerId")
    List<String> findDistinctCarriersByCustomer(@Param("customerId") String customerId);
    
    // Entity Graph queries to avoid N+1 problems
    @EntityGraph(attributePaths = {"stops", "events"})
    Optional<Shipment> findWithAssociationsById(UUID id);
    
    @EntityGraph(attributePaths = {"stops"})
    Page<Shipment> findWithStopsByCustomerId(String customerId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"stops"})
    Page<Shipment> findWithStopsByCarrierId(String carrierId, Pageable pageable);
    
    @Query("SELECT DISTINCT s FROM Shipment s LEFT JOIN FETCH s.stops WHERE s.status = :status")
    Page<Shipment> findByStatusWithStops(@Param("status") ShipmentStatus status, Pageable pageable);
    
    @Query("SELECT DISTINCT s FROM Shipment s LEFT JOIN FETCH s.stops LEFT JOIN FETCH s.events WHERE s.shipmentNumber = :shipmentNumber")
    Optional<Shipment> findByShipmentNumberWithAssociations(@Param("shipmentNumber") String shipmentNumber);
    
    // Projection queries for efficient caching
    @Query("SELECT s.id as id, s.shipmentNumber as shipmentNumber, s.customerId as customerId, " +
           "s.carrierId as carrierId, s.status as status, s.mode as mode, " +
           "s.origin.city as originCity, s.origin.state as originState, " +
           "s.destination.city as destinationCity, s.destination.state as destinationState, " +
           "s.plannedPickupTime as plannedPickupTime, s.plannedDeliveryTime as plannedDeliveryTime, " +
           "s.estimatedDeliveryTime as estimatedDeliveryTime, s.actualDeliveryTime as actualDeliveryTime, " +
           "SIZE(s.stops) as stopCount, s.updatedAt as updatedAt " +
           "FROM Shipment s WHERE s.id = :id")
    <T> Optional<T> findSummaryById(@Param("id") UUID id, Class<T> type);
    
    @Query("SELECT s.id as id, s.shipmentNumber as shipmentNumber, s.customerId as customerId, " +
           "s.carrierId as carrierId, s.status as status, s.mode as mode, " +
           "s.origin.city as originCity, s.origin.state as originState, " +
           "s.destination.city as destinationCity, s.destination.state as destinationState, " +
           "s.plannedPickupTime as plannedPickupTime, s.plannedDeliveryTime as plannedDeliveryTime, " +
           "s.estimatedDeliveryTime as estimatedDeliveryTime, s.actualDeliveryTime as actualDeliveryTime, " +
           "SIZE(s.stops) as stopCount, s.updatedAt as updatedAt " +
           "FROM Shipment s WHERE s.customerId = :customerId")
    Page<ShipmentSummaryProjection> findSummariesByCustomerId(@Param("customerId") String customerId, Pageable pageable);
}