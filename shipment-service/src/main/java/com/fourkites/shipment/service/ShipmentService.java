package com.fourkites.shipment.service;

import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.dto.ShipmentDTO;
import com.fourkites.shipment.dto.StopDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ShipmentService {

    ShipmentDTO createShipment(ShipmentDTO shipmentDTO);

    ShipmentDTO updateShipment(UUID id, ShipmentDTO shipmentDTO);

    ShipmentDTO getShipment(UUID id);

    ShipmentDTO getShipmentByNumber(String shipmentNumber);

    Page<ShipmentDTO> getShipmentsByCustomer(String customerId, Pageable pageable);

    Page<ShipmentDTO> getShipmentsByCarrier(String carrierId, Pageable pageable);

    Page<ShipmentDTO> getShipmentsByStatus(ShipmentStatus status, Pageable pageable);

    void updateShipmentStatus(UUID id, ShipmentStatus status);

    void cancelShipment(UUID id, String reason);

    ShipmentDTO addStop(UUID shipmentId, StopDTO stopDTO);

    void updateStop(UUID shipmentId, UUID stopId, StopDTO stopDTO);

    void removeStop(UUID shipmentId, UUID stopId);

    List<ShipmentDTO> getShipmentsForDelivery(LocalDateTime start, LocalDateTime end);

    void updateEstimatedDelivery(UUID id, LocalDateTime estimatedDelivery);

    void markAsDelivered(UUID id, LocalDateTime deliveryTime);

    void processLocationUpdate(UUID shipmentId, Double latitude, Double longitude);

    List<ShipmentDTO> findNearbyShipments(Double latitude, Double longitude, Double radiusKm);

    void archiveOldShipments(Integer daysOld);

    Page<ShipmentDTO> searchShipments(String query, Pageable pageable);

    void deleteShipment(UUID id);
}