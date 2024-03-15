package io.nuvalence.workmanager.service.mapper;

import static io.nuvalence.workmanager.service.domain.profile.ProfileType.EMPLOYER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileLinkResponse;
import io.nuvalence.workmanager.service.generated.models.GeneralProfileModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.UUID;

class EmployerUserLinkMapperTest {
    private final EmployerUserLinkMapper mapper = Mappers.getMapper(EmployerUserLinkMapper.class);

    @Test
    void employerUserLinkToResponseModel() {
        Employer employer = new Employer();
        UUID employerId = UUID.randomUUID();
        employer.setId(employerId);
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        EmployerUserLink employerUserLink =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(employer)
                        .userId(UUID.randomUUID())
                        .profileAccessLevel(ProfileAccessLevel.ADMIN)
                        .createdBy(user1Id.toString())
                        .lastUpdatedBy(user2Id.toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .build();

        EmployerProfileLinkResponse responseModelResult =
                mapper.employerUserLinkToResponseModel(employerUserLink);

        assertEquals(employerId, responseModelResult.getProfileId());
    }

    @Test
    void employerUserLinkToGeneralProfileModelTest() {
        Employer employer = Employer.builder().id(UUID.randomUUID()).legalName("legalName").build();

        EmployerUserLink link =
                EmployerUserLink.builder()
                        .profile(employer)
                        .profileAccessLevel(ProfileAccessLevel.ADMIN)
                        .build();

        GeneralProfileModel result = mapper.employerUserLinkToGeneralProfileModel(link);

        assertEquals("legalName", result.getDisplayName());
        assertEquals(employer.getId(), result.getId());
        assertEquals(EMPLOYER.getValue(), result.getType());
        assertEquals(link.getProfileAccessLevel().getValue(), result.getLevel());
    }
}
