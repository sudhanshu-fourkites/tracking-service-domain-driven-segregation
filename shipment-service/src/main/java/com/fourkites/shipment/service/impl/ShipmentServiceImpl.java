package com.fourkites.shipment.service.impl;

import com.fourkites.shipment.domain.Shipment;
import com.fourkites.shipment.domain.ShipmentEvent;
import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.domain.Stop;
import com.fourkites.shipment.dto.ShipmentDTO;
import com.fourkites.shipment.dto.StopDTO;
import com.fourkites.shipment.exception.ResourceNotFoundException;
import com.fourkites.shipment.exception.ShipmentAlreadyExistsException;
import com.fourkites.shipment.exception.InvalidShipmentStateException;
import com.fourkites.shipment.kafka.producer.ShipmentEventProducer;
import com.fourkites.shipment.mapper.ShipmentMapper;
import com.fourkites.shipment.mapper.StopMapper;
import com.fourkites.shipment.repository.ShipmentRepository;
import com.fourkites.shipment.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.ConcurrentModificationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final StopMapper stopMapper;
    private final ShipmentEventProducer eventProducer;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShipmentDTO createShipment(ShipmentDTO shipmentDTO) {
        log.info("Creating new shipment with number: {}", shipmentDTO.getShipmentNumber());
        
        if (shipmentRepository.existsByShipmentNumber(shipmentDTO.getShipmentNumber())) {
            throw new ShipmentAlreadyExistsException("Shipment with number " + 
                shipmentDTO.getShipmentNumber() + " already exists");
        }

        Shipment shipment = shipmentMapper.toEntity(shipmentDTO);
        shipment.setStatus(ShipmentStatus.CREATED);
        
        if (shipmentDTO.getStops() != null) {
            shipmentDTO.getStops().forEach(stopDTO -> {
                Stop stop = stopMapper.toEntity(stopDTO);
                shipment.addStop(stop);
            });
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        
        eventProducer.sendShipmentCreatedEvent(savedShipment);
        
        log.info("Shipment created successfully with ID: {}", savedShipment.getId());
        return shipmentMapper.toDto(savedShipment);
    }

    @Override
    @CacheEvict(value = "shipments", key = "#id")
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public ShipmentDTO updateShipment(UUID id, ShipmentDTO shipmentDTO) {
        log.info("Updating shipment with ID: {}", id);
        
        Shipment existingShipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));

        if (existingShipment.getStatus() == ShipmentStatus.DELIVERED || 
            existingShipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new InvalidShipmentStateException("Cannot update shipment in " + 
                existingShipment.getStatus() + " status");
        }

        shipmentMapper.updateEntityFromDto(shipmentDTO, existingShipment);
        Shipment updatedShipment = shipmentRepository.save(existingShipment);
        
        eventProducer.sendShipmentUpdatedEvent(updatedShipment);
        
        log.info("Shipment updated successfully with ID: {}", id);
        return shipmentMapper.toDto(updatedShipment);
    }

    @Override
    @Cacheable(value = "shipments", key = "#id")
    @Transactional(readOnly = true)
    public ShipmentDTO getShipment(UUID id) {
        log.debug("Fetching shipment with ID: {}", id);
        
        // Use entity graph to fetch associations eagerly
        Shipment shipment = shipmentRepository.findWithAssociationsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        return shipmentMapper.toDto(shipment);
    }

    @Override
    @Cacheable(value = "shipments", key = "#shipmentNumber")
    @Transactional(readOnly = true)
    public ShipmentDTO getShipmentByNumber(String shipmentNumber) {
        log.debug("Fetching shipment with number: {}", shipmentNumber);
        
        // Use optimized query with associations
        Shipment shipment = shipmentRepository.findByShipmentNumberWithAssociations(shipmentNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with number: " + shipmentNumber));
        
        return shipmentMapper.toDto(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDTO> getShipmentsByCustomer(String customerId, Pageable pageable) {
        log.debug("Fetching shipments for customer: {}", customerId);
        
        // Use entity graph to avoid N+1 queries
        Page<Shipment> shipments = shipmentRepository.findWithStopsByCustomerId(customerId, pageable);
        return shipments.map(shipmentMapper::toDto);
    }

    @Override
    public Page<ShipmentDTO> getShipmentsByCarrier(String carrierId, Pageable pageable) {
        log.debug("Fetching shipments for carrier: {}", carrierId);
        
        Page<Shipment> shipments = shipmentRepository.findByCarrierId(carrierId, pageable);
        return shipments.map(shipmentMapper::toDto);
    }

    @Override
    public Page<ShipmentDTO> getShipmentsByStatus(ShipmentStatus status, Pageable pageable) {
        log.debug("Fetching shipments with status: {}", status);
        
        Page<Shipment> shipments = shipmentRepository.findByStatus(status, pageable);
        return shipments.map(shipmentMapper::toDto);
    }

    @Override
    @CacheEvict(value = "shipments", key = "#id")
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public void updateShipmentStatus(UUID id, ShipmentStatus status) {
        log.info("Updating shipment status for ID: {} to {}", id, status);
        
        try {
            Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
            
            validateStatusTransition(shipment.getStatus(), status);
            
            ShipmentStatus oldStatus = shipment.getStatus();
            shipment.setStatus(status);
            
            ShipmentEvent event = ShipmentEvent.builder()
                .shipment(shipment)
                .eventType("STATUS_CHANGE")
                .eventTimestamp(LocalDateTime.now())
                .description("Status changed from " + oldStatus + " to " + status)
                .build();
            
            shipment.addEvent(event);
            
            shipmentRepository.save(shipment);
            
            eventProducer.sendShipmentStatusChangedEvent(shipment, status);
            
            log.info("Shipment status updated successfully");
        } catch (OptimisticLockException e) {
            log.error("Concurrent modification detected for shipment: {}", id);
            throw new ConcurrentModificationException("Shipment was modified by another transaction");
        }
    }

    @Override
    @CacheEvict(value = "shipments", key = "#id")
    public void cancelShipment(UUID id, String reason) {
        log.info("Cancelling shipment with ID: {}", id);
        
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException("Cannot cancel delivered shipment");
        }
        
        shipment.setStatus(ShipmentStatus.CANCELLED);
        
        ShipmentEvent event = ShipmentEvent.builder()
            .shipment(shipment)
            .eventType("CANCELLED")
            .eventTimestamp(LocalDateTime.now())
            .description("Shipment cancelled: " + reason)
            .build();
        
        shipment.addEvent(event);
        
        shipmentRepository.save(shipment);
        
        eventProducer.sendShipmentCancelledEvent(shipment, reason);
        
        log.info("Shipment cancelled successfully");
    }

    @Override
    @CacheEvict(value = "shipments", key = "#shipmentId")
    public ShipmentDTO addStop(UUID shipmentId, StopDTO stopDTO) {
        log.info("Adding stop to shipment: {}", shipmentId);
        
        Shipment shipment = shipmentRepository.findByIdWithStops(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));
        
        Stop stop = stopMapper.toEntity(stopDTO);
        shipment.addStop(stop);
        
        Shipment savedShipment = shipmentRepository.save(shipment);
        
        log.info("Stop added successfully to shipment");
        return shipmentMapper.toDto(savedShipment);
    }

    @Override
    @CacheEvict(value = "shipments", key = "#shipmentId")
    public void updateStop(UUID shipmentId, UUID stopId, StopDTO stopDTO) {
        log.info("Updating stop {} for shipment: {}", stopId, shipmentId);
        
        Shipment shipment = shipmentRepository.findByIdWithStops(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));
        
        Stop stop = shipment.getStops().stream()
            .filter(s -> s.getId().equals(stopId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Stop not found with ID: " + stopId));
        
        stopMapper.updateEntityFromDto(stopDTO, stop);
        shipmentRepository.save(shipment);
        
        log.info("Stop updated successfully");
    }

    @Override
    @CacheEvict(value = "shipments", key = "#shipmentId")
    public void removeStop(UUID shipmentId, UUID stopId) {
        log.info("Removing stop {} from shipment: {}", stopId, shipmentId);
        
        Shipment shipment = shipmentRepository.findByIdWithStops(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));
        
        shipment.getStops().removeIf(stop -> stop.getId().equals(stopId));
        shipmentRepository.save(shipment);
        
        log.info("Stop removed successfully");
    }

    @Override
    public List<ShipmentDTO> getShipmentsForDelivery(LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching shipments for delivery between {} and {}", start, end);
        
        List<Shipment> shipments = shipmentRepository.findByPlannedDeliveryTimeBetween(start, end);
        return shipments.stream()
            .map(shipmentMapper::toDto)
            .toList();
    }

    @Override
    @CacheEvict(value = "shipments", key = "#id")
    public void updateEstimatedDelivery(UUID id, LocalDateTime estimatedDelivery) {
        log.info("Updating estimated delivery for shipment: {}", id);
        
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        shipment.setEstimatedDeliveryTime(estimatedDelivery);
        
        ShipmentEvent event = ShipmentEvent.builder()
            .shipment(shipment)
            .eventType("ETA_UPDATED")
            .eventTimestamp(LocalDateTime.now())
            .description("Estimated delivery updated to: " + estimatedDelivery)
            .build();
        
        shipment.addEvent(event);
        
        shipmentRepository.save(shipment);
        
        log.info("Estimated delivery updated successfully");
    }

    @Override
    @CacheEvict(value = "shipments", key = "#id")
    public void markAsDelivered(UUID id, LocalDateTime deliveryTime) {
        log.info("Marking shipment as delivered: {}", id);
        
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setActualDeliveryTime(deliveryTime);
        
        ShipmentEvent event = ShipmentEvent.builder()
            .shipment(shipment)
            .eventType("DELIVERED")
            .eventTimestamp(deliveryTime)
            .description("Shipment delivered successfully")
            .build();
        
        shipment.addEvent(event);
        
        shipmentRepository.save(shipment);
        
        eventProducer.sendShipmentDeliveredEvent(shipment);
        
        log.info("Shipment marked as delivered successfully");
    }

    @Override
    public void processLocationUpdate(UUID shipmentId, Double latitude, Double longitude) {
        log.debug("Processing location update for shipment: {}", shipmentId);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));
        
        eventProducer.sendLocationUpdateEvent(shipment, latitude, longitude);
        
        log.debug("Location update processed successfully");
    }

    @Override
    public List<ShipmentDTO> findNearbyShipments(Double latitude, Double longitude, Double radiusKm) {
        log.debug("Finding shipments near location: {}, {}", latitude, longitude);
        
        List<Shipment> shipments = shipmentRepository.findShipmentsNearLocation(
            latitude, longitude, radiusKm * 1000);
        
        return shipments.stream()
            .map(shipmentMapper::toDto)
            .toList();
    }

    @Override
    public void archiveOldShipments(Integer daysOld) {
        log.info("Archiving shipments older than {} days", daysOld);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Shipment> oldShipments = shipmentRepository.findStaleShipments(
            ShipmentStatus.DELIVERED, cutoffDate);
        
        log.info("Found {} shipments to archive", oldShipments.size());
    }

    @Override
    public Page<ShipmentDTO> searchShipments(String query, Pageable pageable) {
        log.debug("Searching shipments with query: {}", query);
        
        Specification<Shipment> spec = (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.or(
                criteriaBuilder.like(root.get("shipmentNumber"), "%" + query + "%"),
                criteriaBuilder.like(root.get("referenceNumber"), "%" + query + "%"),
                criteriaBuilder.like(root.get("poNumber"), "%" + query + "%")
            );
        
        Page<Shipment> shipments = shipmentRepository.findAll(spec, pageable);
        return shipments.map(shipmentMapper::toDto);
    }

    @Override
    @CacheEvict(value = "shipments", key = "#id")
    public void deleteShipment(UUID id) {
        log.info("Deleting shipment with ID: {}", id);
        
        if (!shipmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + id);
        }
        
        shipmentRepository.deleteById(id);
        log.info("Shipment deleted successfully");
    }

    private void validateStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        if (currentStatus == ShipmentStatus.DELIVERED || currentStatus == ShipmentStatus.CANCELLED) {
            throw new InvalidShipmentStateException(
                "Cannot transition from " + currentStatus + " to " + newStatus);
        }
    }
}