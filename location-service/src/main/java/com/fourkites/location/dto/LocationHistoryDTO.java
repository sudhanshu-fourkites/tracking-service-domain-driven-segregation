package com.fourkites.location.dto;

import com.fourkites.location.domain.LocationStatistics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationHistoryDTO {
    private String id;
    private UUID shipmentId;
    private String date;
    private Integer locationCount;
    private LocationStatistics statistics;
}