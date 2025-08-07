package com.fourkites.location.service;

import com.fourkites.location.domain.Address;

public interface GeocodeService {
    Address reverseGeocode(double latitude, double longitude);
    void geocodeAddress(String addressString);
}