package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.Address;
import io.nuvalence.workmanager.service.generated.models.AddressModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AddressMapperTest {

    private final AddressMapper mapper = Mappers.getMapper(AddressMapper.class);

    @Test
    void addressToAddressDTO_test() {
        AddressModel addressModelResult = mapper.addressToAddressDTO(createAddress());

        assertEquals(createAddressModel(), addressModelResult);
    }

    @Test
    void addressDTOToAddress_test() {
        Address addressResult = mapper.addressDTOToAddress(createAddressModel());

        assertEquals(createAddress(), addressResult);
    }

    private Address createAddress() {
        return Address.builder()
                .address1("address1")
                .address2("address2")
                .city("city")
                .state("state")
                .postalCode("postalCode")
                .country("country")
                .county("county")
                .build();
    }

    private AddressModel createAddressModel() {
        AddressModel addressModel = new AddressModel();
        addressModel.address1("address1");
        addressModel.address2("address2");
        addressModel.city("city");
        addressModel.state("state");
        addressModel.postalCode("postalCode");
        addressModel.country("country");
        addressModel.county("county");

        return addressModel;
    }
}
