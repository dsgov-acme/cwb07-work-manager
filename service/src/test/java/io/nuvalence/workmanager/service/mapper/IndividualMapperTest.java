package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileUpdateModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

class IndividualMapperTest {

    private final UUID owneruserId = UUID.randomUUID();

    private final IndividualMapper mapper = Mappers.getMapper(IndividualMapper.class);
    private final AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(mapper, "addressMapper", addressMapper);
    }

    @Test
    void individualToResponseModel() {
        UUID id = UUID.randomUUID();

        IndividualProfileResponseModel responseModelResult =
                mapper.individualToResponseModel(createIndividual(id));

        assertEquals(profileResponseModel(id), responseModelResult);
    }

    @Test
    void updateModelIndividual() {
        Individual individualResult = mapper.updateModelToIndividual(profileUpdateModel());

        assertEquals(createIndividual(null), individualResult);
    }

    @Test
    void createModelToIndividua() {
        Individual individualResult = mapper.createModelToIndividual(profileCreateModel());

        assertEquals(createIndividual(null), individualResult);
    }

    private Individual createIndividual(UUID id) {
        return Individual.builder().id(id).ssn("ssn").ownerUserId(owneruserId).build();
    }

    private IndividualProfileUpdateModel profileUpdateModel() {
        IndividualProfileUpdateModel individualProfileUpdateModel =
                new IndividualProfileUpdateModel();
        individualProfileUpdateModel.setSsn("ssn");
        individualProfileUpdateModel.setOwnerUserId(owneruserId);
        return individualProfileUpdateModel;
    }

    private IndividualProfileCreateModel profileCreateModel() {
        IndividualProfileCreateModel individualProfileCreateModel =
                new IndividualProfileCreateModel();
        individualProfileCreateModel.setSsn("ssn");
        individualProfileCreateModel.setOwnerUserId(owneruserId);
        return individualProfileCreateModel;
    }

    private IndividualProfileResponseModel profileResponseModel(UUID id) {
        IndividualProfileResponseModel individualProfileResponseModel =
                new IndividualProfileResponseModel();
        individualProfileResponseModel.setId(id);
        individualProfileResponseModel.setSsn("ssn");
        individualProfileResponseModel.setOwnerUserId(owneruserId);
        return individualProfileResponseModel;
    }
}
