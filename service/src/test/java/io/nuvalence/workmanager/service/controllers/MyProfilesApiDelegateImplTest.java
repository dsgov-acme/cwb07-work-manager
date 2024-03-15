package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:transaction-admin", "wm:transaction-config-admin"})
class MyProfilesApiDelegateImplTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;
    @MockBean private UserManagementService userManagementService;
    @MockBean private IndividualUserLinkService individualUserLinkService;
    @MockBean private EmployerUserLinkService employerUserLinkService;

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
    }

    @Test
    void getProfilesForAuthenticatedUserTest_NoType() throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            String originatorId = UUID.randomUUID().toString();
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);

            User user = mock(User.class);

            when(userManagementService.getUserOptional(UUID.fromString(originatorId)))
                    .thenReturn(Optional.ofNullable(user));
            when(user.getDisplayName()).thenReturn("Test User");

            when(individualUserLinkService.getIndividualLinksByUserId(originatorId))
                    .thenReturn(createIndividualProfiles());
            when(employerUserLinkService.getEmployerLinksByUserId(any()))
                    .thenReturn(createEmployerProfiles());

            mockMvc.perform(
                            get("/api/v1/my-profiles")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .characterEncoding("utf-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].type").value("INDIVIDUAL"))
                    .andExpect(jsonPath("$[0].level").value("READER"))
                    .andExpect(jsonPath("$[1].type").value("EMPLOYER"))
                    .andExpect(jsonPath("$[1].level").value("ADMIN"));
        }
    }

    @Test
    void getProfilesForAuthenticatedUserTest_Individual() throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            String originatorId = UUID.randomUUID().toString();
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);

            User user = mock(User.class);

            when(userManagementService.getUserOptional(UUID.fromString(originatorId)))
                    .thenReturn(Optional.ofNullable(user));
            when(user.getDisplayName()).thenReturn("Test User");

            when(individualUserLinkService.getIndividualLinksByUserId(originatorId))
                    .thenReturn(createIndividualProfiles());

            mockMvc.perform(
                            get("/api/v1/my-profiles?type=individual")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .characterEncoding("utf-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type").value("INDIVIDUAL"))
                    .andExpect(jsonPath("$[0].level").value("READER"));
        }
    }

    @Test
    void getProfilesForAuthenticatedUserTest_Employer() throws Exception {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            String originatorId = UUID.randomUUID().toString();
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(originatorId);

            User user = mock(User.class);

            when(userManagementService.getUserOptional(UUID.fromString(originatorId)))
                    .thenReturn(Optional.ofNullable(user));
            when(user.getDisplayName()).thenReturn("Test User");

            when(employerUserLinkService.getEmployerLinksByUserId(any()))
                    .thenReturn(createEmployerProfiles());

            mockMvc.perform(
                            get("/api/v1/my-profiles?type=employer")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .characterEncoding("utf-8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type").value("EMPLOYER"))
                    .andExpect(jsonPath("$[0].level").value("ADMIN"));
        }
    }

    @Test
    void getProfilesForAuthenticatedUserTest_Failure() throws Exception {
        mockMvc.perform(
                        get("/api/v1/my-profiles?type=invalid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .characterEncoding("utf-8"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.messages",
                                Matchers.hasItem(
                                        "type: must match \"(?i)(individual|employer)\"")));
    }

    private List<IndividualUserLink> createIndividualProfiles() {
        Individual individual = Individual.builder().id(UUID.randomUUID()).build();
        IndividualUserLink individualUserLinkOne =
                IndividualUserLink.builder()
                        .id(UUID.randomUUID())
                        .accessLevel(ProfileAccessLevel.READER)
                        .profile(individual)
                        .build();

        return List.of(individualUserLinkOne);
    }

    private List<EmployerUserLink> createEmployerProfiles() {
        Employer employerOne =
                Employer.builder().id(UUID.randomUUID()).legalName("Employer One").build();
        EmployerUserLink employerUserLinkOne =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(employerOne)
                        .userId(UUID.randomUUID())
                        .profileAccessLevel(ProfileAccessLevel.ADMIN)
                        .build();
        return List.of(employerUserLinkOne);
    }
}
