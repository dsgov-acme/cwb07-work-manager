package io.nuvalence.workmanager.service.config;

import static io.nuvalence.workmanager.service.domain.profile.ProfileType.EMPLOYER;
import static io.nuvalence.workmanager.service.domain.profile.ProfileType.INDIVIDUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.models.AccessProfileDto;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CerbosPrincipalAttributeProviderImplTest {
    @Mock private IndividualUserLinkService individualUserLinkService;
    @Mock private EmployerUserLinkService employerUserLinkService;
    @InjectMocks private CerbosPrincipalAttributeProviderImpl provider;

    @Test
    void getAttributesTest() {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            UUID individualId = UUID.randomUUID();
            Individual individual = Individual.builder().id(individualId).build();
            IndividualUserLink individualUserLink =
                    IndividualUserLink.builder()
                            .profile(individual)
                            .accessLevel(ProfileAccessLevel.ADMIN)
                            .build();
            when(individualUserLinkService.getIndividualLinksByUserId(any()))
                    .thenReturn(List.of(individualUserLink));

            UUID employerId = UUID.randomUUID();
            Employer employer = Employer.builder().id(employerId).build();
            EmployerUserLink employerUserLink =
                    EmployerUserLink.builder()
                            .profile(employer)
                            .profileAccessLevel(ProfileAccessLevel.READER)
                            .build();
            when(employerUserLinkService.getEmployerLinksByUserId(any()))
                    .thenReturn(List.of(employerUserLink));

            Map<String, Object> result = provider.getAttributes(mock(Authentication.class));

            assertTrue(result.containsKey("accessProfiles"));
            Object accessProfilesObject = result.get("accessProfiles");
            assertNotNull(accessProfilesObject);
            assertTrue(accessProfilesObject instanceof List);

            List<?> accessProfiles = (List<?>) accessProfilesObject;
            assertEquals(2, accessProfiles.size());

            accessProfiles.forEach(profile -> assertTrue(profile instanceof AccessProfileDto));

            assertEquals(INDIVIDUAL, ((AccessProfileDto) accessProfiles.get(0)).getType());
            assertEquals(
                    ProfileAccessLevel.ADMIN,
                    ((AccessProfileDto) accessProfiles.get(0)).getLevel());

            assertEquals(EMPLOYER, ((AccessProfileDto) accessProfiles.get(1)).getType());
            assertEquals(
                    ProfileAccessLevel.READER,
                    ((AccessProfileDto) accessProfiles.get(1)).getLevel());
        }
    }
}
