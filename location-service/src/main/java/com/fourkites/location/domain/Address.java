package com.fourkites.location.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String streetNumber;
    private String streetName;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String formattedAddress;
    private String placeId;
    private String locationType;
}