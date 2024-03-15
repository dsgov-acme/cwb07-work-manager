package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.Address;
import io.nuvalence.workmanager.service.generated.models.AddressModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressMapper INSTANCE = Mappers.getMapper(AddressMapper.class);

    AddressModel addressToAddressDTO(Address address);

    Address addressDTOToAddress(AddressModel addressModel);
}
