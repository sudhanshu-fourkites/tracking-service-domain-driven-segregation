package com.fourkites.shipment.mapper;

import com.fourkites.shipment.domain.Stop;
import com.fourkites.shipment.dto.StopDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface StopMapper {

    @Mapping(target = "shipment", ignore = true)
    Stop toEntity(StopDTO dto);

    StopDTO toDto(Stop entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(StopDTO dto, @MappingTarget Stop entity);
}