package com.fourkites.shipment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stops", indexes = {
    @Index(name = "idx_stop_shipment", columnList = "shipment_id"),
    @Index(name = "idx_stop_sequence", columnList = "sequenceNumber")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "shipment")
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StopType type;

    @Embedded
    private Address location;

    private LocalDateTime plannedArrival;
    private LocalDateTime actualArrival;
    private LocalDateTime plannedDeparture;
    private LocalDateTime actualDeparture;

    private String stopReferenceNumber;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String notes;

    @Enumerated(EnumType.STRING)
    private StopStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}