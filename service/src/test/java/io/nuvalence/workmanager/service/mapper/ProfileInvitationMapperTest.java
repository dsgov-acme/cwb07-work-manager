package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

class ProfileInvitationMapperTest {

    private final ProfileInvitationMapper mapper = Mappers.getMapper(ProfileInvitationMapper.class);

    @Test
    void createModelToProfileInvitation() {
        UUID profileId = UUID.randomUUID();
        ProfileInvitationRequestModel model = new ProfileInvitationRequestModel();
        model.setAccessLevel("ADMIN");
        model.setEmail("test@email.com");

        ProfileInvitation result = mapper.createModelToProfileInvitation(profileId, model);

        ProfileInvitation expected =
                ProfileInvitation.builder()
                        .profileId(profileId)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .email("test@email.com")
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void profileInvitationToResponseModel() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .id(id)
                        .profileId(profileId)
                        .type(ProfileType.EMPLOYER)
                        .accessLevel(ProfileAccessLevel.WRITER)
                        .email("test@email.com")
                        .build();

        ProfileInvitationResponse result = mapper.profileInvitationToResponseModel(invitation);

        ProfileInvitationResponse expected = new ProfileInvitationResponse();
        expected.setId(id);
        expected.setProfileId(profileId);
        expected.setProfileType("EMPLOYER");
        expected.setAccessLevel("WRITER");
        expected.setEmail("test@email.com");

        assertEquals(expected, result);
    }
}
