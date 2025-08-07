package com.fourkites.shipment.mapper;

import com.fourkites.shipment.domain.Shipment;
import com.fourkites.shipment.dto.ShipmentDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {StopMapper.class, AddressMapper.class})
public interface ShipmentMapper {

    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "events", ignore = true)
    Shipment toEntity(ShipmentDTO dto);

    ShipmentDTO toDto(Shipment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shipmentNumber", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateEntityFromDto(ShipmentDTO dto, @MappingTarget Shipment entity);
}