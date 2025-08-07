package com.fourkites.shipment.mapper;

import com.fourkites.shipment.domain.Address;
import com.fourkites.shipment.dto.AddressDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    
    Address toEntity(AddressDTO dto);
    
    AddressDTO toDto(Address entity);
}