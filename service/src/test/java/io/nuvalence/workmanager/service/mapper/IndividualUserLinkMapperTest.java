package io.nuvalence.workmanager.service.mapper;

import static io.nuvalence.workmanager.service.domain.profile.ProfileType.INDIVIDUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.generated.models.GeneralProfileModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkResponseModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkUpdateModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

class IndividualUserLinkMapperTest {

    private final UUID userId = UUID.randomUUID();

    private final IndividualUserLinkMapper mapper =
            Mappers.getMapper(IndividualUserLinkMapper.class);

    @Test
    void individualToResponseModel() {
        IndividualProfileLinkResponseModel responseModelResult =
                mapper.individualLUserlinkToResponseModel(createIndividualUserLink());

        assertEquals(individualProfileLinkResponseModel(), responseModelResult);
    }

    @Test
    void updateModelToIndividualLink() {
        IndividualUserLink individualUserLinkResult =
                mapper.updateModelToIndividualLink(profileUpdateModel());

        IndividualUserLink individualUserLink = createIndividualUserLink();
        assertEquals(individualUserLink, individualUserLinkResult);
    }

    @Test
    void individualUserLinkToGeneralProfileModelTest() {
        Individual individual = Individual.builder().id(UUID.randomUUID()).build();

        IndividualUserLink link = createIndividualUserLink();
        link.setProfile(individual);

        GeneralProfileModel result =
                mapper.individualUserLinkToGeneralProfileModel(link, "displayName");

        assertEquals("displayName", result.getDisplayName());
        assertEquals(individual.getId(), result.getId());
        assertEquals(INDIVIDUAL.getValue(), result.getType());
        assertEquals(link.getAccessLevel().getValue(), result.getLevel());
    }

    private IndividualUserLink createIndividualUserLink() {
        return IndividualUserLink.builder()
                .userId(userId)
                .accessLevel(ProfileAccessLevel.ADMIN)
                .build();
    }

    private IndividualProfileLinkUpdateModel profileUpdateModel() {
        IndividualProfileLinkUpdateModel individualProfileUpdateModel =
                new IndividualProfileLinkUpdateModel();
        ;
        individualProfileUpdateModel.setAccessLevel(String.valueOf(ProfileAccessLevel.ADMIN));
        individualProfileUpdateModel.setUserId(userId);
        return individualProfileUpdateModel;
    }

    private IndividualProfileLinkResponseModel individualProfileLinkResponseModel() {
        IndividualProfileLinkResponseModel individualProfileResponseModel =
                new IndividualProfileLinkResponseModel();
        individualProfileResponseModel.setUserId(userId);
        individualProfileResponseModel.setAccessLevel(String.valueOf(ProfileAccessLevel.ADMIN));
        return individualProfileResponseModel;
    }
}
