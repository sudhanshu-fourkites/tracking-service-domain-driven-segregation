package com.fourkites.shipment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipment_events", indexes = {
    @Index(name = "idx_event_shipment", columnList = "shipment_id"),
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_event_timestamp", columnList = "eventTimestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "shipment")
public class ShipmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(length = 1000)
    private String description;

    private String location;
    private String reportedBy;
    private String source;

    @Column(columnDefinition = "jsonb")
    private String eventData;

    private LocalDateTime createdAt;
}