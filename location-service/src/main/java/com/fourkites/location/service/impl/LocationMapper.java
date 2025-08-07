package com.fourkites.location.service.impl;

import com.fourkites.location.domain.Location;
import com.fourkites.location.dto.LocationDTO;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {
    
    public LocationDTO toDTO(Location location) {
        if (location == null) {
            return null;
        }
        
        return LocationDTO.builder()
            .id(location.getId())
            .shipmentId(location.getShipmentId())
            .deviceId(location.getDeviceId())
            .carrierId(location.getCarrierId())
            .latitude(location.getLatitude())
            .longitude(location.getLongitude())
            .altitude(location.getAltitude())
            .accuracy(location.getAccuracy())
            .speed(location.getSpeed())
            .heading(location.getHeading())
            .timestamp(location.getTimestamp())
            .receivedAt(location.getReceivedAt())
            .source(location.getSource() != null ? location.getSource().name() : null)
            .quality(location.getQuality() != null ? location.getQuality().name() : null)
            .metadata(location.getMetadata())
            .address(location.getAddress())
            .geofenceId(location.getGeofenceId())
            .geofenceEvent(location.getGeofenceEvent() != null ? location.getGeofenceEvent().name() : null)
            .batteryLevel(location.getBatteryLevel())
            .signalStrength(location.getSignalStrength())
            .networkType(location.getNetworkType())
            .isMoving(location.getIsMoving())
            .stopId(location.getStopId())
            .distanceFromStop(location.getDistanceFromStop())
            .build();
    }
}