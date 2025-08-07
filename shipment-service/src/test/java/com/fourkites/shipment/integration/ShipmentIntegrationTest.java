package com.fourkites.shipment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourkites.shipment.domain.ShipmentMode;
import com.fourkites.shipment.domain.ShipmentStatus;
import com.fourkites.shipment.dto.AddressDTO;
import com.fourkites.shipment.dto.ShipmentDTO;
import com.fourkites.shipment.dto.StopDTO;
import com.fourkites.shipment.domain.StopType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "shipment-created", "shipment-updated", "shipment-cancelled",
    "shipment-delivered", "shipment-status-changed", "location-updates"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShipmentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("shipment_test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdShipmentId;
    private static String shipmentNumber = "TEST-" + System.currentTimeMillis();

    @BeforeAll
    static void setUp() {
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @Test
    @Order(1)
    void createShipment_Success() throws Exception {
        ShipmentDTO shipmentDTO = createTestShipmentDTO();

        MvcResult result = mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shipmentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentNumber").value(shipmentNumber))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.carrierId").value("CARR-001"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ShipmentDTO created = objectMapper.readValue(response, ShipmentDTO.class);
        createdShipmentId = created.getId().toString();
    }

    @Test
    @Order(2)
    void createShipment_Duplicate_Returns409() throws Exception {
        ShipmentDTO shipmentDTO = createTestShipmentDTO();

        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shipmentDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Shipment Already Exists"));
    }

    @Test
    @Order(3)
    void getShipment_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/{id}", createdShipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdShipmentId))
                .andExpect(jsonPath("$.shipmentNumber").value(shipmentNumber));
    }

    @Test
    @Order(4)
    void getShipmentByNumber_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/by-number/{shipmentNumber}", shipmentNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentNumber").value(shipmentNumber))
                .andExpect(jsonPath("$.id").value(createdShipmentId));
    }

    @Test
    @Order(5)
    void updateShipment_Success() throws Exception {
        ShipmentDTO updateDTO = createTestShipmentDTO();
        updateDTO.setPieceCount(20);
        updateDTO.setWeight(new BigDecimal("2000.00"));

        mockMvc.perform(put("/api/v1/shipments/{id}", createdShipmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pieceCount").value(20))
                .andExpect(jsonPath("$.weight").value(2000.00));
    }

    @Test
    @Order(6)
    void updateShipmentStatus_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/shipments/{id}/status", createdShipmentId)
                .param("status", "IN_TRANSIT"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/shipments/{id}", createdShipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    @Order(7)
    void addStop_Success() throws Exception {
        StopDTO stopDTO = StopDTO.builder()
                .sequenceNumber(1)
                .type(StopType.WAYPOINT)
                .location(AddressDTO.builder()
                        .addressLine1("789 Waypoint Rd")
                        .city("Waypoint City")
                        .state("WP")
                        .zipCode("55555")
                        .country("USA")
                        .build())
                .plannedArrival(LocalDateTime.now().plusDays(2))
                .plannedDeparture(LocalDateTime.now().plusDays(2).plusHours(2))
                .build();

        mockMvc.perform(post("/api/v1/shipments/{id}/stops", createdShipmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stopDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stops", hasSize(1)))
                .andExpect(jsonPath("$.stops[0].type").value("WAYPOINT"));
    }

    @Test
    @Order(8)
    void updateEstimatedDelivery_Success() throws Exception {
        LocalDateTime estimatedDelivery = LocalDateTime.now().plusDays(4);

        mockMvc.perform(patch("/api/v1/shipments/{id}/estimated-delivery", createdShipmentId)
                .param("estimatedDelivery", estimatedDelivery.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    void updateLocation_Success() throws Exception {
        String locationJson = "{\"latitude\": 41.8781, \"longitude\": -87.6298}";

        mockMvc.perform(post("/api/v1/shipments/{id}/location", createdShipmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(locationJson))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(10)
    void getShipmentsByCustomer_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/customer/{customerId}", "CUST-001")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].customerId").value("CUST-001"));
    }

    @Test
    @Order(11)
    void getShipmentsByCarrier_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/carrier/{carrierId}", "CARR-001")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].carrierId").value("CARR-001"));
    }

    @Test
    @Order(12)
    void getShipmentsByStatus_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/status/{status}", "IN_TRANSIT")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(13)
    void searchShipments_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/search")
                .param("query", shipmentNumber)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(14)
    void findNearbyShipments_Success() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/nearby")
                .param("latitude", "40.7128")
                .param("longitude", "-74.0060")
                .param("radiusKm", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(15)
    void markAsDelivered_Success() throws Exception {
        LocalDateTime deliveryTime = LocalDateTime.now();

        mockMvc.perform(post("/api/v1/shipments/{id}/deliver", createdShipmentId)
                .param("deliveryTime", deliveryTime.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/shipments/{id}", createdShipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @Order(16)
    void updateDeliveredShipment_Returns400() throws Exception {
        ShipmentDTO updateDTO = createTestShipmentDTO();
        updateDTO.setPieceCount(30);

        mockMvc.perform(put("/api/v1/shipments/{id}", createdShipmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Shipment State"));
    }

    @Test
    @Order(17)
    void getShipment_NotFound_Returns404() throws Exception {
        String randomId = UUID.randomUUID().toString();
        
        mockMvc.perform(get("/api/v1/shipments/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @Order(18)
    void createShipment_InvalidData_Returns400() throws Exception {
        ShipmentDTO invalidShipment = new ShipmentDTO();
        
        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidShipment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @Order(19)
    void getDeliveryWindow_Success() throws Exception {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);

        mockMvc.perform(get("/api/v1/shipments/delivery-window")
                .param("start", start.toString())
                .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(20)
    void deleteShipment_Success() throws Exception {
        ShipmentDTO toDelete = createTestShipmentDTO();
        toDelete.setShipmentNumber("DELETE-TEST-" + System.currentTimeMillis());

        MvcResult result = mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toDelete)))
                .andExpect(status().isCreated())
                .andReturn();

        ShipmentDTO created = objectMapper.readValue(
            result.getResponse().getContentAsString(), ShipmentDTO.class);

        mockMvc.perform(delete("/api/v1/shipments/{id}", created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/shipments/{id}", created.getId()))
                .andExpect(status().isNotFound());
    }

    private ShipmentDTO createTestShipmentDTO() {
        return ShipmentDTO.builder()
                .shipmentNumber(shipmentNumber)
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
                .commodityDescription("Test Commodity")
                .referenceNumber("REF-001")
                .build();
    }
}