package com.fourkites.shipment.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourkites.shipment.domain.Shipment;
import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.shipment-created}")
    private String shipmentCreatedTopic;

    @Value("${kafka.topics.shipment-updated}")
    private String shipmentUpdatedTopic;

    @Value("${kafka.topics.shipment-cancelled}")
    private String shipmentCancelledTopic;

    @Value("${kafka.topics.shipment-delivered}")
    private String shipmentDeliveredTopic;

    @Value("${kafka.topics.shipment-status-changed}")
    private String shipmentStatusChangedTopic;

    @Value("${kafka.topics.location-updates}")
    private String locationUpdatesTopic;

    public void sendShipmentCreatedEvent(Shipment shipment) {
        ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .shipmentId(shipment.getId().toString())
            .shipmentNumber(shipment.getShipmentNumber())
            .customerId(shipment.getCustomerId())
            .carrierId(shipment.getCarrierId())
            .mode(shipment.getMode().toString())
            .origin(mapAddress(shipment.getOrigin()))
            .destination(mapAddress(shipment.getDestination()))
            .plannedPickupTime(shipment.getPlannedPickupTime())
            .plannedDeliveryTime(shipment.getPlannedDeliveryTime())
            .build();

        sendEvent(shipmentCreatedTopic, shipment.getId().toString(), event);
    }

    public void sendShipmentUpdatedEvent(Shipment shipment) {
        ShipmentUpdatedEvent event = ShipmentUpdatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .shipmentId(shipment.getId().toString())
            .shipmentNumber(shipment.getShipmentNumber())
            .status(shipment.getStatus().toString())
            .estimatedDeliveryTime(shipment.getEstimatedDeliveryTime())
            .build();

        sendEvent(shipmentUpdatedTopic, shipment.getId().toString(), event);
    }

    public void sendShipmentCancelledEvent(Shipment shipment, String reason) {
        ShipmentCancelledEvent event = ShipmentCancelledEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .shipmentId(shipment.getId().toString())
            .shipmentNumber(shipment.getShipmentNumber())
            .reason(reason)
            .customerId(shipment.getCustomerId())
            .carrierId(shipment.getCarrierId())
            .build();

        sendEvent(shipmentCancelledTopic, shipment.getId().toString(), event);
    }

    public void sendShipmentDeliveredEvent(Shipment shipment) {
        ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .shipmentId(shipment.getId().toString())
            .shipmentNumber(shipment.getShipmentNumber())
            .actualDeliveryTime(shipment.getActualDeliveryTime())
            .customerId(shipment.getCustomerId())
            .carrierId(shipment.getCarrierId())
            .build();

        sendEvent(shipmentDeliveredTopic, shipment.getId().toString(), event);
    }

    public void sendShipmentStatusChangedEvent(Shipment shipment, ShipmentStatus newStatus) {
        ShipmentStatusChangedEvent event = ShipmentStatusChangedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .shipmentId(shipment.getId().toString())
            .shipmentNumber(shipment.getShipmentNumber())
            .previousStatus(shipment.getStatus().toString())
            .newStatus(newStatus.toString())
            .customerId(shipment.getCustomerId())
            .carrierId(shipment.getCarrierId())
            .build();

        sendEvent(shipmentStatusChangedTopic, shipment.getId().toString(), event);
    }

    public void sendLocationUpdateEvent(Shipment shipment, Double latitude, Double longitude) {
        LocationUpdateEvent event = LocationUpdateEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .shipmentId(shipment.getId().toString())
            .shipmentNumber(shipment.getShipmentNumber())
            .latitude(latitude)
            .longitude(longitude)
            .build();

        sendEvent(locationUpdatesTopic, shipment.getId().toString(), event);
    }

    private void sendEvent(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event sent successfully to topic: {} with key: {}", topic, key);
                } else {
                    log.error("Failed to send event to topic: {} with key: {}", topic, key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending event to Kafka: ", e);
        }
    }

    private AddressEvent mapAddress(com.fourkites.shipment.domain.Address address) {
        if (address == null) return null;
        
        return AddressEvent.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .city(address.getCity())
            .state(address.getState())
            .zipCode(address.getZipCode())
            .country(address.getCountry())
            .latitude(address.getLatitude())
            .longitude(address.getLongitude())
            .build();
    }
}