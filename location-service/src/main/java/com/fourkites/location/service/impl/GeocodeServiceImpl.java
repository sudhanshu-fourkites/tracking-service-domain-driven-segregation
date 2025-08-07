package com.fourkites.location.service.impl;

import com.fourkites.location.domain.Address;
import com.fourkites.location.service.GeocodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GeocodeServiceImpl implements GeocodeService {
    
    @Override
    @Cacheable(value = "geocode-cache", key = "#latitude + ':' + #longitude")
    public Address reverseGeocode(double latitude, double longitude) {
        log.debug("Reverse geocoding: {}, {}", latitude, longitude);
        
        return Address.builder()
            .formattedAddress(String.format("%.6f, %.6f", latitude, longitude))
            .latitude(latitude)
            .longitude(longitude)
            .build();
    }
    
    @Override
    public void geocodeAddress(String addressString) {
        log.debug("Geocoding address: {}", addressString);
    }
}