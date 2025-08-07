package com.fourkites.shipment.unit.service;

import com.fourkites.shipment.domain.*;
import com.fourkites.shipment.dto.ShipmentDTO;
import com.fourkites.shipment.dto.AddressDTO;
import com.fourkites.shipment.exception.InvalidShipmentStateException;
import com.fourkites.shipment.exception.ResourceNotFoundException;
import com.fourkites.shipment.exception.ShipmentAlreadyExistsException;
import com.fourkites.shipment.kafka.producer.ShipmentEventProducer;
import com.fourkites.shipment.mapper.ShipmentMapper;
import com.fourkites.shipment.mapper.StopMapper;
import com.fourkites.shipment.repository.ShipmentRepository;
import com.fourkites.shipment.service.impl.ShipmentServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private StopMapper stopMapper;

    @Mock
    private ShipmentEventProducer eventProducer;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private Shipment testShipment;
    private ShipmentDTO testShipmentDTO;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        
        testShipment = Shipment.builder()
            .id(testId)
            .shipmentNumber("SHP-001")
            .customerId("CUST-001")
            .carrierId("CARR-001")
            .status(ShipmentStatus.CREATED)
            .mode(ShipmentMode.TRUCK_FTL)
            .origin(Address.builder()
                .addressLine1("123 Origin St")
                .city("Origin City")
                .state("OS")
                .zipCode("12345")
                .country("USA")
                .latitude(new BigDecimal("40.7128"))
                .longitude(new BigDecimal("-74.0060"))
                .build())
            .destination(Address.builder()
                .addressLine1("456 Dest Ave")
                .city("Dest City")
                .state("DS")
                .zipCode("67890")
                .country("USA")
                .latitude(new BigDecimal("34.0522"))
                .longitude(new BigDecimal("-118.2437"))
                .build())
            .plannedPickupTime(LocalDateTime.now().plusDays(1))
            .plannedDeliveryTime(LocalDateTime.now().plusDays(3))
            .weight(new BigDecimal("1000.00"))
            .volume(new BigDecimal("500.00"))
            .pieceCount(10)
            .build();

        testShipmentDTO = ShipmentDTO.builder()
            .id(testId)
            .shipmentNumber("SHP-001")
            .customerId("CUST-001")
            .carrierId("CARR-001")
            .status(ShipmentStatus.CREATED)
            .mode(ShipmentMode.TRUCK_FTL)
            .origin(AddressDTO.builder()
                .addressLine1("123 Origin St")
                .city("Origin City")
                .state("OS")
                .zipCode("12345")
                .country("USA")
                .latitude(new BigDecimal("40.7128"))
                .longitude(new BigDecimal("-74.0060"))
                .build())
            .destination(AddressDTO.builder()
                .addressLine1("456 Dest Ave")
                .city("Dest City")
                .state("DS")
                .zipCode("67890")
                .country("USA")
                .latitude(new BigDecimal("34.0522"))
                .longitude(new BigDecimal("-118.2437"))
                .build())
            .plannedPickupTime(LocalDateTime.now().plusDays(1))
            .plannedDeliveryTime(LocalDateTime.now().plusDays(3))
            .weight(new BigDecimal("1000.00"))
            .volume(new BigDecimal("500.00"))
            .pieceCount(10)
            .build();
    }

    @Test
    void createShipment_Success() {
        when(shipmentRepository.existsByShipmentNumber(anyString())).thenReturn(false);
        when(shipmentMapper.toEntity(any(ShipmentDTO.class))).thenReturn(testShipment);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);
        when(shipmentMapper.toDto(any(Shipment.class))).thenReturn(testShipmentDTO);

        ShipmentDTO result = shipmentService.createShipment(testShipmentDTO);

        assertThat(result).isNotNull();
        assertThat(result.getShipmentNumber()).isEqualTo("SHP-001");
        verify(shipmentRepository).save(any(Shipment.class));
        verify(eventProducer).sendShipmentCreatedEvent(any(Shipment.class));
    }

    @Test
    void createShipment_AlreadyExists_ThrowsException() {
        when(shipmentRepository.existsByShipmentNumber(anyString())).thenReturn(true);

        assertThatThrownBy(() -> shipmentService.createShipment(testShipmentDTO))
            .isInstanceOf(ShipmentAlreadyExistsException.class)
            .hasMessageContaining("already exists");

        verify(shipmentRepository, never()).save(any());
        verify(eventProducer, never()).sendShipmentCreatedEvent(any());
    }

    @Test
    void updateShipment_Success() {
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);
        when(shipmentMapper.toDto(any(Shipment.class))).thenReturn(testShipmentDTO);

        ShipmentDTO result = shipmentService.updateShipment(testId, testShipmentDTO);

        assertThat(result).isNotNull();
        verify(shipmentMapper).updateEntityFromDto(testShipmentDTO, testShipment);
        verify(shipmentRepository).save(testShipment);
        verify(eventProducer).sendShipmentUpdatedEvent(testShipment);
    }

    @Test
    void updateShipment_NotFound_ThrowsException() {
        when(shipmentRepository.findById(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.updateShipment(testId, testShipmentDTO))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void updateShipment_DeliveredStatus_ThrowsException() {
        testShipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));

        assertThatThrownBy(() -> shipmentService.updateShipment(testId, testShipmentDTO))
            .isInstanceOf(InvalidShipmentStateException.class)
            .hasMessageContaining("Cannot update shipment");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void getShipment_Success() {
        when(shipmentRepository.findByIdWithStops(testId)).thenReturn(Optional.of(testShipment));
        when(shipmentMapper.toDto(testShipment)).thenReturn(testShipmentDTO);

        ShipmentDTO result = shipmentService.getShipment(testId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        verify(shipmentRepository).findByIdWithStops(testId);
    }

    @Test
    void getShipment_NotFound_ThrowsException() {
        when(shipmentRepository.findByIdWithStops(testId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getShipment(testId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void getShipmentByNumber_Success() {
        when(shipmentRepository.findByShipmentNumber("SHP-001")).thenReturn(Optional.of(testShipment));
        when(shipmentMapper.toDto(testShipment)).thenReturn(testShipmentDTO);

        ShipmentDTO result = shipmentService.getShipmentByNumber("SHP-001");

        assertThat(result).isNotNull();
        assertThat(result.getShipmentNumber()).isEqualTo("SHP-001");
    }

    @Test
    void getShipmentsByCustomer_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shipment> shipmentPage = new PageImpl<>(List.of(testShipment));
        
        when(shipmentRepository.findByCustomerId("CUST-001", pageable)).thenReturn(shipmentPage);
        when(shipmentMapper.toDto(any(Shipment.class))).thenReturn(testShipmentDTO);

        Page<ShipmentDTO> result = shipmentService.getShipmentsByCustomer("CUST-001", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerId()).isEqualTo("CUST-001");
    }

    @Test
    void updateShipmentStatus_Success() {
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        shipmentService.updateShipmentStatus(testId, ShipmentStatus.IN_TRANSIT);

        assertThat(testShipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(shipmentRepository).save(testShipment);
        verify(eventProducer).sendShipmentStatusChangedEvent(testShipment, ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void updateShipmentStatus_InvalidTransition_ThrowsException() {
        testShipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));

        assertThatThrownBy(() -> 
            shipmentService.updateShipmentStatus(testId, ShipmentStatus.IN_TRANSIT))
            .isInstanceOf(InvalidShipmentStateException.class)
            .hasMessageContaining("Cannot transition");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void cancelShipment_Success() {
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        shipmentService.cancelShipment(testId, "Customer request");

        assertThat(testShipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        verify(shipmentRepository).save(testShipment);
        verify(eventProducer).sendShipmentCancelledEvent(testShipment, "Customer request");
    }

    @Test
    void cancelShipment_AlreadyDelivered_ThrowsException() {
        testShipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));

        assertThatThrownBy(() -> shipmentService.cancelShipment(testId, "Too late"))
            .isInstanceOf(InvalidShipmentStateException.class)
            .hasMessageContaining("Cannot cancel delivered shipment");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void markAsDelivered_Success() {
        LocalDateTime deliveryTime = LocalDateTime.now();
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        shipmentService.markAsDelivered(testId, deliveryTime);

        assertThat(testShipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(testShipment.getActualDeliveryTime()).isEqualTo(deliveryTime);
        verify(shipmentRepository).save(testShipment);
        verify(eventProducer).sendShipmentDeliveredEvent(testShipment);
    }

    @Test
    void getShipmentsForDelivery_Success() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        List<Shipment> shipments = List.of(testShipment);
        
        when(shipmentRepository.findByPlannedDeliveryTimeBetween(start, end)).thenReturn(shipments);
        when(shipmentMapper.toDto(any(Shipment.class))).thenReturn(testShipmentDTO);

        List<ShipmentDTO> result = shipmentService.getShipmentsForDelivery(start, end);

        assertThat(result).hasSize(1);
        verify(shipmentRepository).findByPlannedDeliveryTimeBetween(start, end);
    }

    @Test
    void updateEstimatedDelivery_Success() {
        LocalDateTime estimatedTime = LocalDateTime.now().plusDays(2);
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        shipmentService.updateEstimatedDelivery(testId, estimatedTime);

        assertThat(testShipment.getEstimatedDeliveryTime()).isEqualTo(estimatedTime);
        verify(shipmentRepository).save(testShipment);
    }

    @Test
    void processLocationUpdate_Success() {
        when(shipmentRepository.findById(testId)).thenReturn(Optional.of(testShipment));

        shipmentService.processLocationUpdate(testId, 40.7128, -74.0060);

        verify(eventProducer).sendLocationUpdateEvent(testShipment, 40.7128, -74.0060);
    }

    @Test
    void findNearbyShipments_Success() {
        List<Shipment> nearbyShipments = List.of(testShipment);
        when(shipmentRepository.findShipmentsNearLocation(40.7128, -74.0060, 50000))
            .thenReturn(nearbyShipments);
        when(shipmentMapper.toDto(any(Shipment.class))).thenReturn(testShipmentDTO);

        List<ShipmentDTO> result = shipmentService.findNearbyShipments(40.7128, -74.0060, 50.0);

        assertThat(result).hasSize(1);
        verify(shipmentRepository).findShipmentsNearLocation(40.7128, -74.0060, 50000);
    }

    @Test
    void deleteShipment_Success() {
        when(shipmentRepository.existsById(testId)).thenReturn(true);

        shipmentService.deleteShipment(testId);

        verify(shipmentRepository).deleteById(testId);
    }

    @Test
    void deleteShipment_NotFound_ThrowsException() {
        when(shipmentRepository.existsById(testId)).thenReturn(false);

        assertThatThrownBy(() -> shipmentService.deleteShipment(testId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");

        verify(shipmentRepository, never()).deleteById(any());
    }
}