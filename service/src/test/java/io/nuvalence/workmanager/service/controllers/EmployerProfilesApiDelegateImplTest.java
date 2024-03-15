package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.profile.Address;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.models.AddressModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileLinkRequestModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileUpdateModel;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.workmanager.service.models.EmployerFilters;
import io.nuvalence.workmanager.service.models.EmployerUserLinkFilters;
import io.nuvalence.workmanager.service.models.ProfileInvitationFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.repository.EmployerRepository;
import io.nuvalence.workmanager.service.service.EmployerService;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.ProfileInvitationService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import io.nuvalence.workmanager.service.utils.UserUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:employer_profile_admin"})
class EmployerProfilesApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private EmployerRepository repository;

    @MockBean private EmployerUserLinkService employerUserLinkService;

    @MockBean private EmployerService employerService;

    @MockBean private ProfileInvitationService profileInvitationService;

    @Mock private Appender<ILoggingEvent> mockAppender;

    private static final String EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE =
            "employer_profile_invitation";
    @MockBean private UserManagementService userManagementService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
        Logger logger = (Logger) LoggerFactory.getLogger(EmployerProfilesApiDelegateImpl.class);
        logger.addAppender(mockAppender);

        this.objectMapper = SpringConfig.getMapper();
    }

    @Test
    void getEmployerProfile_Success() throws Exception {
        Employer employer = createEmployer();
        UUID profileId = employer.getId();

        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));

        mockMvc.perform(get("/api/v1/employer-profiles/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId.toString()))
                .andExpect(jsonPath("$.fein").value(employer.getFein()))
                .andExpect(jsonPath("$.legalName").value(employer.getLegalName()))
                .andExpect(jsonPath("$.otherNames", hasSize(1)))
                .andExpect(jsonPath("$.otherNames[0]").value(employer.getOtherNames().get(0)))
                .andExpect(jsonPath("$.type").value(employer.getType()))
                .andExpect(jsonPath("$.industry").value(employer.getIndustry()))
                .andExpect(jsonPath("$.summaryOfBusiness").value(employer.getSummaryOfBusiness()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(employer.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.locations", hasSize(1)))
                .andExpect(
                        jsonPath("$.locations[0].address1")
                                .value(employer.getLocations().get(0).getAddress1()));
    }

    @Test
    void getEmployerProfile_NotFound() throws Exception {
        Employer employer = createEmployer();
        UUID profileId = employer.getId();
        when(employerService.getEmployerById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/employer-profiles/" + profileId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Employer profile not found\"]}"));
    }

    @Test
    void getEmployerProfiles() throws Exception {
        Employer employer = createEmployer();
        Page<Employer> employerPage = new PageImpl<>(Collections.singletonList(employer));
        when(employerService.getEmployersByFilters(any(EmployerFilters.class)))
                .thenReturn(employerPage);

        mockMvc.perform(get("/api/v1/employer-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(employer.getId().toString()))
                .andExpect(jsonPath("$.items[0].fein").value(employer.getFein()))
                .andExpect(jsonPath("$.items[0].legalName").value(employer.getLegalName()))
                .andExpect(jsonPath("$.items[0].otherNames", hasSize(1)))
                .andExpect(
                        jsonPath("$.items[0].otherNames[0]").value(employer.getOtherNames().get(0)))
                .andExpect(jsonPath("$.items[0].type").value(employer.getType()))
                .andExpect(jsonPath("$.items[0].industry").value(employer.getIndustry()))
                .andExpect(
                        jsonPath("$.items[0].summaryOfBusiness")
                                .value(employer.getSummaryOfBusiness()))
                .andExpect(
                        jsonPath("$.items[0].mailingAddress.address1")
                                .value(employer.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.items[0].locations", hasSize(1)))
                .andExpect(
                        jsonPath("$.items[0].locations[0].address1")
                                .value(employer.getLocations().get(0).getAddress1()))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0));
    }

    @Test
    void getEmployerProfilesForbbiben() throws Exception {

        when(authorizationHandler.isAllowed("view", Employer.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/employer-profiles")).andExpect(status().isForbidden());
    }

    @Test
    void postEmployerProfile() throws Exception {
        EmployerProfileCreateModel employer = profileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(employerService.saveEmployer(any(Employer.class))).thenReturn(createEmployer());

        mockMvc.perform(
                        post("/api/v1/employer-profiles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fein").value(employer.getFein()))
                .andExpect(jsonPath("$.legalName").value(employer.getLegalName()))
                .andExpect(jsonPath("$.otherNames", hasSize(1)))
                .andExpect(jsonPath("$.otherNames[0]").value(employer.getOtherNames().get(0)))
                .andExpect(jsonPath("$.type").value(employer.getType()))
                .andExpect(jsonPath("$.industry").value(employer.getIndustry()))
                .andExpect(jsonPath("$.summaryOfBusiness").value(employer.getSummaryOfBusiness()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(employer.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.locations", hasSize(1)))
                .andExpect(
                        jsonPath("$.locations[0].address1")
                                .value(employer.getLocations().get(0).getAddress1()));
    }

    @Test
    void postEmployerProfileForbidden() throws Exception {
        EmployerProfileCreateModel employer = profileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(authorizationHandler.isAllowed("create", Employer.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/employer-profiles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEmployerProfile_Success() throws Exception {
        EmployerProfileUpdateModel employer = profileUpdateModel();

        Employer modifiedEmployer =
                Employer.builder()
                        .id(UUID.randomUUID())
                        .fein("fein - changed")
                        .legalName("legalName - changed")
                        .otherNames(Collections.singletonList("otherNames - changed"))
                        .type("LLC")
                        .industry("industry - changed")
                        .summaryOfBusiness("summaryOfBusiness - changed")
                        .businessPhone("businessPhone - changed")
                        .mailingAddress(createAddress())
                        .locations(List.of(createAddress()))
                        .build();

        when(employerService.getEmployerById(any(UUID.class)))
                .thenReturn(Optional.of(createEmployer()))
                .thenReturn(Optional.of(modifiedEmployer));

        when(employerService.saveEmployer(any(Employer.class))).thenReturn(modifiedEmployer);

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            put("/api/v1/employer-profiles/" + modifiedEmployer.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBodyJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fein").value(employer.getFein()))
                    .andExpect(jsonPath("$.legalName").value(employer.getLegalName()))
                    .andExpect(jsonPath("$.otherNames", hasSize(1)))
                    .andExpect(jsonPath("$.otherNames[0]").value(employer.getOtherNames().get(0)))
                    .andExpect(jsonPath("$.type").value(employer.getType()))
                    .andExpect(jsonPath("$.industry").value(employer.getIndustry()))
                    .andExpect(
                            jsonPath("$.summaryOfBusiness").value(employer.getSummaryOfBusiness()))
                    .andExpect(
                            jsonPath("$.mailingAddress.address1")
                                    .value(employer.getMailingAddress().getAddress1()))
                    .andExpect(jsonPath("$.locations", hasSize(1)))
                    .andExpect(
                            jsonPath("$.locations[0].address1")
                                    .value(employer.getLocations().get(0).getAddress1()));
        }
    }

    @Test
    void updateEmployerProfile_Forbidden() throws Exception {
        EmployerProfileUpdateModel employer = profileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(authorizationHandler.isAllowed("update", Employer.class)).thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/employer-profiles/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEmployerProfile_NotFound() throws Exception {
        EmployerProfileUpdateModel employer = profileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(employer);

        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(
                        put("/api/v1/employer-profiles/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Employer profile not found\"]}"));
    }

    @ParameterizedTest
    @CsvSource({"agency, false", "public, false", "public, true", "agency, true"})
    void updateEmployerProfileLink_SuccessAndForbidden(String userType, boolean isAuthorized)
            throws Exception {

        if ((userType.equals("agency") && !isAuthorized) || userType.equals("public")) {
            when(authorizationHandler.isAllowed("update", Employer.class)).thenReturn(false);
        }

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLinkRequestModel request = new EmployerProfileLinkRequestModel();
        request.setProfileAccessLevel("ADMIN");

        Employer employer = new Employer();
        employer.setId(profileId);
        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setUserId(userId);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(
                ProfileAccessLevel.fromValue(request.getProfileAccessLevel()));

        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));
        when(employerUserLinkService.getEmployerUserLink(any(), any()))
                .thenReturn(
                        Optional.of(
                                EmployerUserLink.builder()
                                        .userId(userId)
                                        .profile(employer)
                                        .profileAccessLevel(
                                                isAuthorized
                                                        ? ProfileAccessLevel.ADMIN
                                                        : ProfileAccessLevel.READER)
                                        .build()));

        when(employerUserLinkService.saveEmployerUserLink(any(EmployerUserLink.class)))
                .thenReturn(employerUserLink);

        try (MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

            var mvcRequest =
                    mockMvc.perform(
                            put("/api/v1/employer-profiles/" + profileId + "/link/" + userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            if (isAuthorized) {
                mvcRequest
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userId").value(userId.toString()))
                        .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                        .andExpect(
                                jsonPath("$.profileAccessLevel")
                                        .value(request.getProfileAccessLevel()));
            } else {
                mvcRequest.andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void updateEmployerProfileLink_SuccessCreate() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLinkRequestModel request = new EmployerProfileLinkRequestModel();
        request.setProfileAccessLevel("ADMIN");

        Employer employer = new Employer();
        employer.setId(profileId);
        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setUserId(userId);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(
                ProfileAccessLevel.fromValue(request.getProfileAccessLevel()));

        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));
        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(employerUserLink));

        mockMvc.perform(
                        put("/api/v1/employer-profiles/" + profileId + "/link/" + userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.profileId").value(profileId.toString()))
                .andExpect(jsonPath("$.profileAccessLevel").value(request.getProfileAccessLevel()));
    }

    @Test
    void updateEmployerProfileLink_EmployerNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLinkRequestModel request = new EmployerProfileLinkRequestModel();
        request.setProfileAccessLevel("ADMIN");

        when(employerService.getEmployerById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        put("/api/v1/employer-profiles/" + profileId + "/users/" + userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"agency, false", "public, false", "public, true", "agency, true"})
    void deleteEmployerProfileLink_SuccessAndForbidden(String userType, boolean isAuthorized)
            throws Exception {

        if ((userType.equals("agency") && !isAuthorized) || userType.equals("public")) {
            when(authorizationHandler.isAllowed("delete", EmployerUserLink.class))
                    .thenReturn(false);
        }

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setId(UUID.randomUUID());

        when(employerUserLinkService.getEmployerUserLink(any(), any()))
                .thenReturn(
                        Optional.of(
                                EmployerUserLink.builder()
                                        .userId(userId)
                                        .profileAccessLevel(
                                                isAuthorized
                                                        ? ProfileAccessLevel.ADMIN
                                                        : ProfileAccessLevel.READER)
                                        .build()));

        ArgumentCaptor<EmployerUserLink> employerUserLinkCaptor =
                ArgumentCaptor.forClass(EmployerUserLink.class);

        doNothing()
                .when(employerService)
                .postAuditEventForEmployerProfileUserRemoved(employerUserLinkCaptor.capture());

        String lastUpdatedBy = UUID.randomUUID().toString();

        try (MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(lastUpdatedBy);

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

            mockMvc.perform(delete("/api/v1/employer-profiles/" + profileId + "/link/" + userId))
                    .andExpect(isAuthorized ? status().isNoContent() : status().isForbidden());

            if (isAuthorized) {
                assertEquals(lastUpdatedBy, employerUserLinkCaptor.getValue().getLastUpdatedBy());
            }
        }
    }

    @Test
    void deleteEmployerProfileLink_NotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/employer-profiles/" + profileId + "/link/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEmployerProfileLinks_Success() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String name = "name";
        String email = "email";
        Employer employer = new Employer();
        employer.setId(profileId);
        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setUserId(userId);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(ProfileAccessLevel.fromValue("ADMIN"));

        when(employerUserLinkService.getEmployerUserLinks(any(EmployerUserLinkFilters.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(employerUserLink)));

        when(userManagementService.getUsers(name, email))
                .thenReturn(List.of(UserDTO.builder().id(userId).build()));

        mockMvc.perform(
                        get(
                                "/api/v1/employer-profiles/"
                                        + profileId
                                        + "/link?name="
                                        + name
                                        + "&email="
                                        + email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.items[0].profileId").value(profileId.toString()))
                .andExpect(
                        jsonPath("$.items[0].profileAccessLevel")
                                .value(ProfileAccessLevel.fromValue("ADMIN").toString()));
    }

    @ParameterizedTest
    @CsvSource({
        "public, false, false",
        "public, true, false",
        "public, true, true",
        "agency, true, true"
    })
    void postEmployerProfileInvitation_SuccessAndForbidden(
            String userType, boolean isProfileMember, boolean isAdmin) throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String profileDisplayName = "Test Employer";
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("ADMIN");

        Employer employer = new Employer();
        employer.setId(profileId);
        employer.setLegalName(profileDisplayName);

        if (userType.equals("public")) {
            when(authorizationHandler.isAllowed("invite", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                    .thenReturn(false);
        }

        ProfileInvitation savedInvitation = new ProfileInvitation();
        savedInvitation.setId(UUID.randomUUID());
        savedInvitation.setEmail(invitationRequest.getEmail());
        savedInvitation.setAccessLevel(ProfileAccessLevel.ADMIN);
        savedInvitation.setProfileId(profileId);
        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));
        when(profileInvitationService.saveProfileInvitation(
                        ProfileType.EMPLOYER, profileDisplayName, savedInvitation))
                .thenReturn(savedInvitation);

        Optional<EmployerUserLink> linkOptional = Optional.empty();
        if (isProfileMember) {
            EmployerUserLink link = new EmployerUserLink();
            link.setProfileAccessLevel(
                    isAdmin ? ProfileAccessLevel.ADMIN : ProfileAccessLevel.READER);
            linkOptional = Optional.of(link);
        }
        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(linkOptional);

        try (MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            var result =
                    mockMvc.perform(
                                    post("/api/v1/employer-profiles/" + profileId + "/invitations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            invitationRequest)))
                            .andExpect(
                                    (isProfileMember && isAdmin)
                                            ? status().isOk()
                                            : status().isForbidden());

            if (isProfileMember && isAdmin) {
                result.andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.email").value(invitationRequest.getEmail()));
            }
        }
    }

    @Test
    void postEmployerProfileInvitation_EmployerNotFound() throws Exception {

        UUID profileId = UUID.randomUUID();

        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("ADMIN");

        when(employerService.getEmployerById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/api/v1/employer-profiles/" + profileId + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void postEmployerProfileInvitation_forbidden_non_existing_profile_link() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("WRITER");

        when(authorizationHandler.isAllowed("invite", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(Optional.empty());

        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockMvc.perform(
                            post("/api/v1/employer-profiles/" + profileId + "/invitations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invitationRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void postEmployerProfileInvitation_forbidden_non_admin_access() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("WRITER");

        when(authorizationHandler.isAllowed("invite", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setProfileAccessLevel(ProfileAccessLevel.READER);
        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(Optional.of(employerUserLink));
        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockMvc.perform(
                            post("/api/v1/employer-profiles/" + profileId + "/invitations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invitationRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "public, false, false",
        "public, true, false",
        "public, true, true",
        "agency, true, true"
    })
    void postEmployerProfileInvitation_AuditEventFailure(
            String userType, boolean isProfileMember, boolean isAdmin) throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String profileDisplayName = "Test Employer";
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("ADMIN");

        if (userType.equals("public")) {
            when(authorizationHandler.isAllowed("invite", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                    .thenReturn(false);
        }

        Employer employer = new Employer();
        employer.setId(profileId);
        employer.setLegalName(profileDisplayName);

        ProfileInvitation savedInvitation = new ProfileInvitation();
        savedInvitation.setId(UUID.randomUUID());
        savedInvitation.setEmail(invitationRequest.getEmail());
        savedInvitation.setAccessLevel(ProfileAccessLevel.ADMIN);
        savedInvitation.setProfileId(profileId);

        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));
        when(profileInvitationService.saveProfileInvitation(
                        ProfileType.EMPLOYER, profileDisplayName, savedInvitation))
                .thenReturn(savedInvitation);

        Optional<EmployerUserLink> linkOptional = Optional.empty();
        if (isProfileMember) {
            EmployerUserLink link = new EmployerUserLink();
            link.setProfileAccessLevel(
                    isAdmin ? ProfileAccessLevel.ADMIN : ProfileAccessLevel.READER);
            linkOptional = Optional.of(link);
        }
        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(linkOptional);

        // Simulate an exception when postAuditEventForEmployerProfileInvites is called
        RuntimeException testException = new RuntimeException("Test Exception");
        doThrow(testException)
                .when(employerService)
                .postAuditEventForEmployerProfileInvites(
                        any(ProfileInvitation.class),
                        any(AuditActivityType.class),
                        any(String.class));

        try (MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class)) {

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);

            var result =
                    mockMvc.perform(
                                    post("/api/v1/employer-profiles/" + profileId + "/invitations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            invitationRequest)))
                            .andExpect(
                                    (isProfileMember && isAdmin)
                                            ? status().isOk()
                                            : status().isForbidden());

            if (isProfileMember && isAdmin) {
                result.andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.email").value(invitationRequest.getEmail()));

                verify(mockAppender)
                        .doAppend(
                                ArgumentMatchers.argThat(
                                        argument -> {
                                            assertSame(
                                                    argument.getThrowableProxy().getMessage(),
                                                    testException.getMessage());
                                            assertSame(Level.ERROR, argument.getLevel());
                                            return true;
                                        }));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"public, false", "public, true", "agency, true"})
    void getEmployerProfileInvitations_Success(String userType, boolean authorized)
            throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Page<ProfileInvitation> invitationPage =
                new PageImpl<>(Collections.singletonList(new ProfileInvitation()));
        when(profileInvitationService.getProfileInvitationsByFilters(
                        any(ProfileInvitationFilters.class)))
                .thenReturn(invitationPage);

        when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                .thenReturn(authorized ? Optional.of(new EmployerUserLink()) : Optional.empty());

        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            var result =
                    mockMvc.perform(get("/api/v1/employer-profiles/" + profileId + "/invitations"))
                            .andExpect(authorized ? status().isOk() : status().isForbidden());

            if (authorized) {
                result.andExpect(jsonPath("$.items", hasSize(1)));
            }
        }
    }

    @Test
    void getEmployerProfileInvitations_forbidden() throws Exception {

        UUID profileId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("read", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/employer-profiles/" + profileId + "/invitations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void claimEmployerProfileInvitation_Success() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileAccessLevel accessLevel = ProfileAccessLevel.READER;
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setEmail("test@example.com");
        invitation.setAccessLevel(accessLevel);
        invitation.setProfileId(profileId);

        Employer employer = new Employer();
        employer.setId(profileId);
        employer.setCreatedBy("some-user");
        when(profileInvitationService.getProfileInvitationByIdAndEmail(
                        invitation.getId(), invitation.getEmail()))
                .thenReturn(Optional.of(invitation));
        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));

        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setUserId(UUID.randomUUID());
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(accessLevel);

        when(employerUserLinkService.saveEmployerUserLink(any(EmployerUserLink.class)))
                .thenReturn(employerUserLink);

        when(employerUserLinkService.getEmployerUserLink(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(employerUserLink));

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID userId = UUID.randomUUID();

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn(invitation.getEmail());
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            mockMvc.perform(
                            post(
                                    "/api/v1/employer-profiles/"
                                            + "invitations/"
                                            + invitationId
                                            + "/claim"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<ProfileInvitation> invitationCaptor =
                    ArgumentCaptor.forClass(ProfileInvitation.class);
            verify(profileInvitationService).saveProfileInvitation(invitationCaptor.capture());
            assertTrue(invitationCaptor.getValue().getClaimed());
        }
    }

    @Test
    void claimEmployerProfileInvitation_nonExistingInvitation() throws Exception {

        UUID nonExistingInvitationId = UUID.randomUUID();

        when(profileInvitationService.getProfileInvitationByIdAndEmail(
                        nonExistingInvitationId, "test@test.com"))
                .thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("test@test.com");
            mockMvc.perform(
                            post(
                                    "/api/v1/employer-profiles/"
                                            + "/invitations/"
                                            + nonExistingInvitationId
                                            + "/claim"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void claimEmployerProfileInvitation_nonExistingProfile() throws Exception {

        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = new ProfileInvitation();

        when(profileInvitationService.getProfileInvitationByIdAndEmail(
                        invitationId, "test@test.com"))
                .thenReturn(Optional.of(invitation));

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("test@test.com");
            mockMvc.perform(
                            post(
                                    "/api/v1/employer-profiles/"
                                            + "/invitations/"
                                            + invitationId
                                            + "/claim"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void claimEmployerProfileInvitation_unauthorized() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setEmail("test@example.com");

        Employer employer = new Employer();
        employer.setId(profileId);

        when(profileInvitationService.getProfileInvitationById(invitation.getId()))
                .thenReturn(Optional.of(invitation));
        when(employerService.getEmployerById(profileId)).thenReturn(Optional.of(employer));

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID userId = UUID.randomUUID();

            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            mockMvc.perform(
                            post(
                                    "/api/v1/employer-profiles/"
                                            + "/invitations/"
                                            + invitationId
                                            + "/claim"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void claimEmployerProfileInvitation_forbidden() throws Exception {

        UUID invitationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("claim", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        mockMvc.perform(
                        post(
                                "/api/v1/employer-profiles/"
                                        + "/invitations/"
                                        + invitationId
                                        + "/claim"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEmployerProfileInvitation_Success() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setClaimed(false);

        when(profileInvitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            delete(
                                    "/api/v1/employer-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId))
                    .andExpect(status().isNoContent());

            verify(profileInvitationService).deleteProfileInvitation(invitationId);
        }
    }

    @Test
    void deleteEmployerProfileInvitation_nonExisting() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID nonExistingInvitationId = UUID.randomUUID();

        when(profileInvitationService.getProfileInvitationById(nonExistingInvitationId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        delete(
                                "/api/v1/employer-profiles/"
                                        + profileId
                                        + "/invitations/"
                                        + nonExistingInvitationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEmployerProfileInvitation_alreadyClaimed() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setClaimed(true);

        when(profileInvitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        mockMvc.perform(
                        delete(
                                "/api/v1/employer-profiles/"
                                        + profileId
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEmployerProfileInvitation_forbidden_ProfileLink_not_exist() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("delete", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        when(employerUserLinkService.getEmployerUserLink(profileId, UUID.randomUUID()))
                .thenReturn(Optional.empty());

        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            delete(
                                    "/api/v1/employer-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void deleteEmployerProfileInvitation_forbidden_non_admin_access() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("delete", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setProfileAccessLevel(ProfileAccessLevel.READER);
        when(employerUserLinkService.getEmployerUserLink(profileId, UUID.randomUUID()))
                .thenReturn(Optional.of(employerUserLink));

        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            delete(
                                    "/api/v1/employer-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void getEmployerProfileLinksUserIdSuccess() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employer employer = new Employer();
        employer.setId(profileId);
        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setUserId(userId);
        employerUserLink.setProfile(employer);
        employerUserLink.setProfileAccessLevel(ProfileAccessLevel.fromValue("ADMIN"));

        when(employerUserLinkService.getEmployerUserLinks(any(EmployerUserLinkFilters.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(employerUserLink)));
        mockMvc.perform(get("/api/v1/employer-profiles/" + profileId + "/link?userId=" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.items[0].profileId").value(profileId.toString()))
                .andExpect(
                        jsonPath("$.items[0].profileAccessLevel")
                                .value(ProfileAccessLevel.fromValue("ADMIN").toString()));
    }

    @Test
    void getEmployerProfileLinks_Forbidden() throws Exception {
        UUID profileId = UUID.randomUUID();

        when(authorizationHandler.isAllowed("view", EmployerUserLink.class)).thenReturn(false);
        mockMvc.perform(get("/api/v1/employer-profiles/" + profileId + "/link"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEmployerProfileInvitationById_forbidden() throws Exception {
        UUID invitationId = UUID.randomUUID();

        when(authorizationHandler.isAllowed("read", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/employer-profiles/invitations/" + invitationId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEmployerProfileInvitationById_invitationNotFound() throws Exception {
        UUID invitationId = UUID.randomUUID();
        when(profileInvitationService.getInvitationByIdAndType(invitationId, ProfileType.EMPLOYER))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/employer-profiles/invitations/" + invitationId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Invitation not found")));
    }

    @Test
    void getEmployerProfileInvitationById_noLinkFound() throws Exception {
        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            UUID invitationId = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();

            ProfileInvitation invitation =
                    ProfileInvitation.builder().id(invitationId).profileId(profileId).build();
            when(profileInvitationService.getInvitationByIdAndType(
                            invitationId, ProfileType.EMPLOYER))
                    .thenReturn(Optional.of(invitation));

            UUID userId = UUID.randomUUID();
            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                    .thenReturn(Optional.empty());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("invalid@email.com");

            mockMvc.perform(get("/api/v1/employer-profiles/invitations/" + invitationId))
                    .andExpect(status().isForbidden())
                    .andExpect(
                            content()
                                    .string(
                                            containsString(
                                                    "User does not have access to this profile")));
        }
    }

    @Test
    void getEmployerProfileInvitationById_success() throws Exception {
        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            UUID invitationId = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();

            String invitationEmail = "email@email.com";
            ProfileInvitation invitation =
                    ProfileInvitation.builder()
                            .id(invitationId)
                            .profileId(profileId)
                            .type(ProfileType.EMPLOYER)
                            .accessLevel(ProfileAccessLevel.ADMIN)
                            .claimed(false)
                            .email(invitationEmail)
                            .build();
            when(profileInvitationService.getInvitationByIdAndType(
                            invitationId, ProfileType.EMPLOYER))
                    .thenReturn(Optional.of(invitation));

            UUID userId = UUID.randomUUID();
            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn(invitationEmail);

            EmployerUserLink employerUserLink = EmployerUserLink.builder().build();
            when(employerUserLinkService.getEmployerUserLink(profileId, userId))
                    .thenReturn(Optional.of(employerUserLink));

            mockMvc.perform(get("/api/v1/employer-profiles/invitations/" + invitationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(invitation.getId().toString()))
                    .andExpect(jsonPath("$.profileId").value(invitation.getProfileId().toString()))
                    .andExpect(jsonPath("$.profileType").value(invitation.getType().getValue()))
                    .andExpect(
                            jsonPath("$.accessLevel").value(invitation.getAccessLevel().getValue()))
                    .andExpect(jsonPath("$.email").value(invitation.getEmail()))
                    .andExpect(jsonPath("$.claimed").value(invitation.getClaimed().toString()));
        }
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

    private Employer createEmployer() {
        return Employer.builder()
                .id(UUID.randomUUID())
                .fein("fein")
                .legalName("legalName")
                .otherNames(Collections.singletonList("otherNames"))
                .type("LLC")
                .industry("industry")
                .summaryOfBusiness("summaryOfBusiness")
                .businessPhone("businessPhone")
                .mailingAddress(createAddress())
                .locations(List.of(createAddress()))
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

    private EmployerProfileCreateModel profileCreateModel() {
        EmployerProfileCreateModel employerProfileCreateModel = new EmployerProfileCreateModel();
        employerProfileCreateModel.setFein("fein");
        employerProfileCreateModel.setLegalName("legalName");
        employerProfileCreateModel.setOtherNames(Collections.singletonList("otherNames"));
        employerProfileCreateModel.setType("LLC");
        employerProfileCreateModel.setIndustry("industry");
        employerProfileCreateModel.setSummaryOfBusiness("summaryOfBusiness");
        employerProfileCreateModel.setMailingAddress(createAddressModel());
        employerProfileCreateModel.setLocations(List.of(createAddressModel()));
        employerProfileCreateModel.businessPhone("businessPhone");

        return employerProfileCreateModel;
    }

    private EmployerProfileUpdateModel profileUpdateModel() {
        EmployerProfileUpdateModel employerProfileUpdateModel = new EmployerProfileUpdateModel();
        employerProfileUpdateModel.setFein("fein - changed");
        employerProfileUpdateModel.setLegalName("legalName - changed");
        employerProfileUpdateModel.setOtherNames(Collections.singletonList("otherNames - changed"));
        employerProfileUpdateModel.setType("LLC");
        employerProfileUpdateModel.setIndustry("industry - changed");
        employerProfileUpdateModel.setSummaryOfBusiness("summaryOfBusiness - changed");
        employerProfileUpdateModel.setMailingAddress(createAddressModel());
        employerProfileUpdateModel.setLocations(List.of(createAddressModel()));
        employerProfileUpdateModel.businessPhone("businessPhone - changed");

        return employerProfileUpdateModel;
    }
}
