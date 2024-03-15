package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.models.AddressModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkUpdateModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileUpdateModel;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.workmanager.service.models.ProfileInvitationFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.repository.IndividualRepository;
import io.nuvalence.workmanager.service.service.IndividualService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import io.nuvalence.workmanager.service.service.ProfileInvitationService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:individual_profile_admin"})
class IndividualProfilesApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private IndividualRepository repository;

    @MockBean private IndividualService individualService;

    @MockBean private IndividualUserLinkService individualUserLinkService;

    @MockBean private ProfileInvitationService profileInvitationService;

    @MockBean private UserManagementService userManagementService;

    @Mock private Appender<ILoggingEvent> mockAppender;

    private static final String INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE =
            "individual_profile_invitation";

    private ObjectMapper objectMapper;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
        Logger logger = (Logger) LoggerFactory.getLogger(IndividualProfilesApiDelegateImpl.class);
        logger.addAppender(mockAppender);

        this.objectMapper = SpringConfig.getMapper();
    }

    @Test
    void getIndividualProfile_Success() throws Exception {
        Individual individual = createIndividual();
        UUID profileId = individual.getId();

        when(individualService.getIndividualById(profileId)).thenReturn(Optional.of(individual));

        mockMvc.perform(get("/api/v1/individual-profiles/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId.toString()))
                .andExpect(jsonPath("$.ssn").value(individual.getSsn()))
                .andExpect(jsonPath("$.ownerUserId").value(individual.getOwnerUserId().toString()))
                .andExpect(
                        jsonPath("$.primaryAddress.address1")
                                .value(individual.getPrimaryAddress().getAddress1()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(individual.getMailingAddress().getAddress1()));
    }

    @Test
    void getIndividualProfile_NotFound() throws Exception {
        Individual individual = createIndividual();
        UUID profileId = individual.getId();
        when(repository.findById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/individual-profiles/" + profileId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Individual profile not found\"]}"));
    }

    @Test
    void postIndividualProfile() throws Exception {
        IndividualProfileCreateModel individual = profileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(repository.save(any(Individual.class))).thenReturn(createIndividual());
        when(individualService.saveIndividual(any(Individual.class)))
                .thenReturn(createIndividual());

        mockMvc.perform(
                        post("/api/v1/individual-profiles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssn").value(individual.getSsn()))
                .andExpect(jsonPath("$.ownerUserId").value(individual.getOwnerUserId().toString()))
                .andExpect(
                        jsonPath("$.primaryAddress.address1")
                                .value(individual.getPrimaryAddress().getAddress1()))
                .andExpect(
                        jsonPath("$.mailingAddress.address1")
                                .value(individual.getMailingAddress().getAddress1()));
    }

    @Test
    void posIndividualProfileUnAuthorize() throws Exception {
        IndividualProfileCreateModel individual = profileCreateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(authorizationHandler.isAllowed("create", Individual.class)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/individual-profiles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateIndividualProfile_Success() throws Exception {
        IndividualProfileUpdateModel individual = profileUpdateModel();

        Individual modifiedIndividual =
                Individual.builder()
                        .id(UUID.randomUUID())
                        .ssn("ssn2")
                        .ownerUserId(UUID.randomUUID())
                        .primaryAddress(createAddress())
                        .mailingAddress(createAddress())
                        .build();

        when(individualService.getIndividualById(any(UUID.class)))
                .thenReturn(Optional.of(createIndividual()))
                .thenReturn(Optional.of(modifiedIndividual));

        when(repository.save(any(Individual.class))).thenReturn(modifiedIndividual);

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            put("/api/v1/individual-profiles/" + modifiedIndividual.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBodyJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ssn").value(modifiedIndividual.getSsn()))
                    .andExpect(
                            jsonPath("$.ownerUserId")
                                    .value(modifiedIndividual.getOwnerUserId().toString()))
                    .andExpect(
                            jsonPath("$.primaryAddress.address1")
                                    .value(modifiedIndividual.getPrimaryAddress().getAddress1()))
                    .andExpect(
                            jsonPath("$.mailingAddress.address1")
                                    .value(modifiedIndividual.getMailingAddress().getAddress1()));
        }
    }

    @Test
    void updateIndividualProfile_NotFound() throws Exception {
        IndividualProfileUpdateModel individual = profileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(
                        put("/api/v1/individual-profiles/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"messages\":[\"Individual profile not found\"]}"));
    }

    @Test
    void updateIndividualProfile_UnAuthorize() throws Exception {
        IndividualProfileUpdateModel individual = profileUpdateModel();

        String requestBodyJson = objectMapper.writeValueAsString(individual);

        when(authorizationHandler.isAllowed("update", Individual.class)).thenReturn(false);

        mockMvc.perform(
                        put("/api/v1/individual-profiles/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void getIndividualProfiles() throws Exception {
        Individual individual = createIndividual();
        Page<Individual> individualPage = new PageImpl<>(Collections.singletonList(individual));

        when(individualService.getIndividualsByFilters(any())).thenReturn(individualPage);

        mockMvc.perform(get("/api/v1/individual-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(individual.getId().toString()))
                .andExpect(jsonPath("$.items[0].ssn").value(individual.getSsn()))
                .andExpect(
                        jsonPath("$.items[0].ownerUserId")
                                .value(individual.getOwnerUserId().toString()))
                .andExpect(
                        jsonPath("$.items[0].primaryAddress.address1")
                                .value(individual.getPrimaryAddress().getAddress1()))
                .andExpect(
                        jsonPath("$.items[0].mailingAddress.address1")
                                .value(individual.getMailingAddress().getAddress1()))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0));
    }

    @Test
    void getIndividualProfilesUnAuthorize() throws Exception {

        when(authorizationHandler.isAllowed("view", Individual.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/individual-profiles")).andExpect(status().isForbidden());
    }

    @Test
    void deleteIndividualProfileLink_Success() throws Exception {
        // Arrange
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        IndividualUserLink userLink = new IndividualUserLink();
        userLink.setId(UUID.randomUUID());

        when(authorizationHandler.isAllowed("delete", IndividualUserLink.class)).thenReturn(true);
        when(individualService.getIndividualById(profileId))
                .thenReturn(Optional.of(createIndividual()));
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
                .thenReturn(Optional.of(userLink));

        // Act & Assert
        mockMvc.perform(
                        delete("/api/v1/individual-profiles/{profileId}/links", profileId)
                                .param("userId", userId.toString()))
                .andExpect(status().isOk());
        verify(individualUserLinkService).deleteIndividualUserLink(userLink);
    }

    @Test
    void deleteIndividualProfileLink_Forbidden() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("delete", IndividualUserLink.class)).thenReturn(false);

        mockMvc.perform(
                        delete("/api/v1/individual-profiles/{profileId}/links", profileId)
                                .param("userId", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteIndividualProfileLink_IndividualNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(individualService.getIndividualById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        delete("/api/v1/individual-profiles/{profileId}/links", profileId)
                                .param("userId", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIndividualProfileLink_UserLinkNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(individualService.getIndividualById(profileId))
                .thenReturn(Optional.of(new Individual()));
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        delete("/api/v1/individual-profiles/{profileId}/links", profileId)
                                .param("userId", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateIndividualProfileLink() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setUserId(UUID.randomUUID());
        updateModel.setAccessLevel("READER");

        Individual existingIndividual =
                Individual.builder()
                        .id(UUID.randomUUID())
                        .ssn("ssn")
                        .ownerUserId(UUID.randomUUID())
                        .primaryAddress(createAddress())
                        .mailingAddress(createAddress())
                        .build();

        when(individualService.getIndividualById(profileId))
                .thenReturn(Optional.of(existingIndividual));
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, updateModel.getUserId()))
                .thenReturn(Optional.empty());

        IndividualUserLink savedIndividualLink =
                IndividualUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(existingIndividual)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .userId(UUID.randomUUID())
                        .build();
        when(individualUserLinkService.saveIndividualUserLink(any(IndividualUserLink.class)))
                .thenReturn(savedIndividualLink);
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, updateModel.getUserId()))
                .thenReturn(Optional.of(savedIndividualLink));

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            mockMvc.perform(
                            MockMvcRequestBuilders.put(
                                            "/api/v1/individual-profiles/{profileId}/links",
                                            profileId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(asJsonString(updateModel)))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }

    @Test
    void testUpdateIndividualProfileLink_Forbidden() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setUserId(UUID.randomUUID());
        updateModel.setAccessLevel("READER");

        when(authorizationHandler.isAllowed("update", IndividualUserLink.class)).thenReturn(false);
        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                        "/api/v1/individual-profiles/{profileId}/links", profileId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(updateModel)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testUpdateIndividualProfileLink_IndividualNotFound_AfterSaving() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setUserId(UUID.randomUUID());
        updateModel.setAccessLevel("READER");

        Individual existingIndividual =
                new Individual(); // create an instance or use Mockito to mock it

        when(individualService.getIndividualById(profileId))
                .thenReturn(Optional.of(existingIndividual));
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, updateModel.getUserId()))
                .thenReturn(Optional.empty());

        IndividualUserLink savedIndividualLink =
                new IndividualUserLink(); // create an instance or use Mockito to mock it
        when(individualUserLinkService.saveIndividualUserLink(any(IndividualUserLink.class)))
                .thenReturn(savedIndividualLink);
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, updateModel.getUserId()))
                .thenReturn(Optional.empty());

        // Use assertThrows to check for 500 error, since not finding the resource after saving it
        // is in fact unexpected behavior, if that ever happens
        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                        "/api/v1/individual-profiles/{profileId}/links", profileId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(updateModel)))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    void testUpdateIndividualProfileLink_IndividualNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        IndividualProfileLinkUpdateModel updateModel = new IndividualProfileLinkUpdateModel();
        updateModel.setUserId(UUID.randomUUID());
        updateModel.setAccessLevel("READER");

        when(individualService.getIndividualById(profileId)).thenReturn(Optional.empty());
        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                        "/api/v1/individual-profiles/{profileId}/links", profileId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(updateModel)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void getIndividualProfilesLinks() throws Exception {
        IndividualUserLink individualUserLink = createIndividualUserLink();
        Page<IndividualUserLink> individualPage =
                new PageImpl<>(Collections.singletonList(individualUserLink));

        when(individualService.getIndividualById(individualUserLink.getProfile().getId()))
                .thenReturn(Optional.of(createIndividual()));

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(individualPage);

        when(individualUserLinkService.getIndividualLinksByFilters(any()))
                .thenReturn(individualPage);

        mockMvc.perform(
                        get(
                                "/api/v1/individual-profiles/{profileId}/links",
                                individualUserLink.getProfile().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(
                        jsonPath("$.items[0].userId")
                                .value(individualUserLink.getUserId().toString()))
                .andExpect(
                        jsonPath("$.items[0].accessLevel")
                                .value(individualUserLink.getAccessLevel().toString()))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0));
    }

    @Test
    void getIndividualProfilesLinks_Forbidden() throws Exception {
        IndividualUserLink individualUserLink = createIndividualUserLink();

        when(authorizationHandler.isAllowed("view", IndividualUserLink.class)).thenReturn(false);

        mockMvc.perform(
                        get(
                                "/api/v1/individual-profiles/{profileId}/links",
                                individualUserLink.getProfile().getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getIndividualProfilesLinks_IndividualNotFound() throws Exception {
        IndividualUserLink individualUserLink = createIndividualUserLink();

        when(individualService.getIndividualById(individualUserLink.getProfile().getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/v1/individual-profiles/{profileId}/links",
                                individualUserLink.getProfile().getId()))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
        "public, false, false",
        "public, true, false",
        "public, true, true",
        "agency, true, true"
    })
    void postIndividualProfileInvitation_SuccessAndForbidden(
            String userType, boolean isProfileMember, boolean isAdmin) throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String profileDisplayName = "Test Profile";
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("ADMIN");

        Individual individual = new Individual();
        individual.setId(profileId);
        individual.setOwnerUserId(userId);

        if (userType.equals("public")) {
            when(authorizationHandler.isAllowed(
                            "invite", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                    .thenReturn(false);
        }

        ProfileInvitation savedInvitation = new ProfileInvitation();
        savedInvitation.setId(UUID.randomUUID());
        savedInvitation.setEmail(invitationRequest.getEmail());
        savedInvitation.setAccessLevel(ProfileAccessLevel.ADMIN);
        savedInvitation.setProfileId(profileId);

        when(individualService.getIndividualById(profileId)).thenReturn(Optional.of(individual));
        when(profileInvitationService.saveProfileInvitation(
                        ProfileType.INDIVIDUAL, profileDisplayName, savedInvitation))
                .thenReturn(savedInvitation);
        when(userManagementService.getUser(userId))
                .thenReturn(
                        User.builder()
                                .id(UUID.randomUUID())
                                .displayName(profileDisplayName)
                                .build());

        Optional<IndividualUserLink> linkOptional = Optional.empty();
        if (isProfileMember) {
            IndividualUserLink link = new IndividualUserLink();
            link.setAccessLevel(isAdmin ? ProfileAccessLevel.ADMIN : ProfileAccessLevel.READER);
            linkOptional = Optional.of(link);
        }
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
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
                                    post("/api/v1/individual-profiles/"
                                                    + profileId
                                                    + "/invitations")
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
    void postIndividualProfileInvitation_IndividualNotFound() throws Exception {

        UUID profileId = UUID.randomUUID();

        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("ADMIN");

        when(individualService.getIndividualById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/api/v1/individual-profiles/" + profileId + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void postIndividualProfileInvitation_forbidden_non_existing_profile_link() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("WRITER");

        when(authorizationHandler.isAllowed("invite", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
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
                            post("/api/v1/individual-profiles/" + profileId + "/invitations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invitationRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void postIndividualProfileInvitation_forbidden_non_admin_access() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("WRITER");

        when(authorizationHandler.isAllowed("invite", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        IndividualUserLink link = new IndividualUserLink();
        link.setAccessLevel(ProfileAccessLevel.READER);
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
                .thenReturn(Optional.of(link));

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
                            post("/api/v1/individual-profiles/" + profileId + "/invitations")
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
    void postIndividualProfileInvitation_AuditEventFailure(
            String userType, boolean isProfileMember, boolean isAdmin) throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String profileDisplayName = "Test Profile";
        ProfileInvitationRequestModel invitationRequest = new ProfileInvitationRequestModel();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setAccessLevel("ADMIN");

        Individual individual = new Individual();
        individual.setId(profileId);
        individual.setOwnerUserId(userId);

        if (userType.equals("public")) {
            when(authorizationHandler.isAllowed(
                            "invite", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                    .thenReturn(false);
        }

        ProfileInvitation savedInvitation = new ProfileInvitation();
        savedInvitation.setId(UUID.randomUUID());
        savedInvitation.setEmail(invitationRequest.getEmail());
        savedInvitation.setAccessLevel(ProfileAccessLevel.ADMIN);
        savedInvitation.setProfileId(profileId);

        when(individualService.getIndividualById(profileId)).thenReturn(Optional.of(individual));
        when(profileInvitationService.saveProfileInvitation(
                        ProfileType.INDIVIDUAL, profileDisplayName, savedInvitation))
                .thenReturn(savedInvitation);
        when(userManagementService.getUser(userId))
                .thenReturn(
                        User.builder()
                                .id(UUID.randomUUID())
                                .displayName(profileDisplayName)
                                .build());

        Optional<IndividualUserLink> linkOptional = Optional.empty();
        if (isProfileMember) {
            IndividualUserLink link = new IndividualUserLink();
            link.setAccessLevel(isAdmin ? ProfileAccessLevel.ADMIN : ProfileAccessLevel.READER);
            linkOptional = Optional.of(link);
        }
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
                .thenReturn(linkOptional);

        // Simulate an exception when postAuditEventForEmployerProfileInvites is called
        RuntimeException testException = new RuntimeException("Test Exception");
        doThrow(testException)
                .when(individualService)
                .postAuditEventForIndividualProfileInvites(
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

            var result =
                    mockMvc.perform(
                                    post("/api/v1/individual-profiles/"
                                                    + profileId
                                                    + "/invitations")
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
    void getIndividualProfileInvitations_Success(String userType, boolean authorized)
            throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Page<ProfileInvitation> invitationPage =
                new PageImpl<>(Collections.singletonList(new ProfileInvitation()));
        when(profileInvitationService.getProfileInvitationsByFilters(
                        any(ProfileInvitationFilters.class)))
                .thenReturn(invitationPage);

        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(profileId, userId))
                .thenReturn(authorized ? Optional.of(new IndividualUserLink()) : Optional.empty());

        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            var result =
                    mockMvc.perform(
                                    get(
                                            "/api/v1/individual-profiles/"
                                                    + profileId
                                                    + "/invitations"))
                            .andExpect(authorized ? status().isOk() : status().isForbidden());

            if (authorized) {
                result.andExpect(jsonPath("$.items", hasSize(1)));
            }
        }
    }

    @Test
    void getIndividualProfileInvitations_forbidden() throws Exception {

        UUID profileId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("read", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/individual-profiles/" + profileId + "/invitations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void claimIndividualProfileInvitation_Success() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileAccessLevel accessLevel = ProfileAccessLevel.READER;
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setEmail("test@example.com");
        invitation.setAccessLevel(accessLevel);
        invitation.setProfileId(profileId);

        Individual individual = new Individual();
        individual.setId(profileId);
        individual.setCreatedBy("some-user");

        when(profileInvitationService.getProfileInvitationByIdAndEmail(
                        invitation.getId(), invitation.getEmail()))
                .thenReturn(Optional.of(invitation));
        when(individualService.getIndividualById(profileId)).thenReturn(Optional.of(individual));

        IndividualUserLink individualUserLink = new IndividualUserLink();
        individualUserLink.setUserId(UUID.randomUUID());
        individualUserLink.setProfile(individual);
        individualUserLink.setAccessLevel(accessLevel);

        when(individualUserLinkService.saveIndividualUserLink(any(IndividualUserLink.class)))
                .thenReturn(individualUserLink);

        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(individualUserLink));

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID userId = UUID.randomUUID();

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn(invitation.getEmail());
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            mockMvc.perform(
                            post(
                                    "/api/v1/individual-profiles/"
                                            + "/invitations/"
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
    void claimIndividualProfileInvitation_nonExistingInvitation() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID nonExistingInvitationId = UUID.randomUUID();

        when(profileInvitationService.getProfileInvitationById(nonExistingInvitationId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post(
                                "/api/v1/individual-profiles/"
                                        + profileId
                                        + "/invitations/"
                                        + nonExistingInvitationId
                                        + "/claim"))
                .andExpect(status().isNotFound());
    }

    @Test
    void claimIndividualProfileInvitation_nonExistingProfile() throws Exception {

        UUID nonExistingProfileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = new ProfileInvitation();

        when(profileInvitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        when(individualService.getIndividualById(nonExistingProfileId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post(
                                "/api/v1/individual-profiles/"
                                        + nonExistingProfileId
                                        + "/invitations/"
                                        + invitationId
                                        + "/claim"))
                .andExpect(status().isNotFound());
    }

    @Test
    void claimIndividualProfileInvitation_unauthorized() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setEmail("test@example.com");

        Individual individual = new Individual();
        individual.setId(profileId);

        when(profileInvitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));
        when(individualService.getIndividualById(profileId)).thenReturn(Optional.of(individual));

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID userId = UUID.randomUUID();

            mocked.when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("other@example.com");
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            mockMvc.perform(
                            post(
                                    "/api/v1/individual-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId
                                            + "/claim"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void claimIndividualProfileInvitation_forbidden() throws Exception {

        UUID invitationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("claim", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        mockMvc.perform(
                        post(
                                "/api/v1/individual-profiles/"
                                        + "invitations/"
                                        + invitationId
                                        + "/claim"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteIndividualProfileInvitation_Success() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setClaimed(false);

        when(profileInvitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            UUID userId = UUID.randomUUID();
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockMvc.perform(
                            delete(
                                    "/api/v1/individual-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId))
                    .andExpect(status().isNoContent());
        }

        verify(profileInvitationService).deleteProfileInvitation(invitationId);
    }

    @Test
    void deleteIndividualProfileInvitation_nonExisting() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID nonExistingInvitationId = UUID.randomUUID();

        when(profileInvitationService.getProfileInvitationById(nonExistingInvitationId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        delete(
                                "/api/v1/individual-profiles/"
                                        + profileId
                                        + "/invitations/"
                                        + nonExistingInvitationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIndividualProfileInvitation_alreadyClaimed() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(invitationId);
        invitation.setClaimed(true);

        when(profileInvitationService.getProfileInvitationById(invitationId))
                .thenReturn(Optional.of(invitation));

        mockMvc.perform(
                        delete(
                                "/api/v1/individual-profiles/"
                                        + profileId
                                        + "/invitations/"
                                        + invitationId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteIndividualProfileInvitation_forbidden_ProfileLink_not_exist() throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("delete", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, UUID.randomUUID()))
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
                                    "/api/v1/individual-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @CsvSource({"agency", "public"})
    void deleteIndividualProfileInvitation_forbidden_non_admin_access(String userType)
            throws Exception {

        UUID profileId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("delete", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);
        IndividualUserLink userLink = new IndividualUserLink();
        userLink.setAccessLevel(ProfileAccessLevel.READER);
        when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, UUID.randomUUID()))
                .thenReturn(Optional.of(userLink));

        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            userUtilityMock.when(UserUtility::getAuthenticatedUserType).thenReturn(userType);
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            mockMvc.perform(
                            delete(
                                    "/api/v1/individual-profiles/"
                                            + profileId
                                            + "/invitations/"
                                            + invitationId))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void getIndividualProfileInvitationById_forbidden() throws Exception {
        UUID invitationId = UUID.randomUUID();

        when(authorizationHandler.isAllowed("read", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/individual-profiles/invitations/" + invitationId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getIndividualProfileInvitationById_invitationNotFound() throws Exception {
        UUID invitationId = UUID.randomUUID();
        when(profileInvitationService.getInvitationByIdAndType(
                        invitationId, ProfileType.INDIVIDUAL))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/individual-profiles/invitations/" + invitationId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Invitation not found")));
    }

    @Test
    void getIndividualProfileInvitationById_noLinkFound() throws Exception {
        try (MockedStatic<UserUtility> userUtilityMock = Mockito.mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> securityContextUtilityMock =
                        Mockito.mockStatic(SecurityContextUtility.class)) {

            UUID invitationId = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();

            ProfileInvitation invitation =
                    ProfileInvitation.builder().id(invitationId).profileId(profileId).build();
            when(profileInvitationService.getInvitationByIdAndType(
                            invitationId, ProfileType.INDIVIDUAL))
                    .thenReturn(Optional.of(invitation));

            UUID userId = UUID.randomUUID();
            userUtilityMock
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, userId))
                    .thenReturn(Optional.empty());
            securityContextUtilityMock
                    .when(SecurityContextUtility::getAuthenticatedUserEmail)
                    .thenReturn("invalid@email.com");

            mockMvc.perform(get("/api/v1/individual-profiles/invitations/" + invitationId))
                    .andExpect(status().isForbidden())
                    .andExpect(
                            content()
                                    .string(
                                            containsString(
                                                    "User does not have access to this profile")));
        }
    }

    @Test
    void getIndividualProfileInvitationById_success() throws Exception {
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
                            .type(ProfileType.INDIVIDUAL)
                            .accessLevel(ProfileAccessLevel.ADMIN)
                            .claimed(false)
                            .email(invitationEmail)
                            .build();
            when(profileInvitationService.getInvitationByIdAndType(
                            invitationId, ProfileType.INDIVIDUAL))
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

            IndividualUserLink individualUserLink = IndividualUserLink.builder().build();
            when(individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, userId))
                    .thenReturn(Optional.of(individualUserLink));

            mockMvc.perform(get("/api/v1/individual-profiles/invitations/" + invitationId))
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

    // Utility method to convert objects to JSON string
    private String asJsonString(Object obj) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(obj);
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

    private Individual createIndividual() {
        return Individual.builder()
                .id(UUID.randomUUID())
                .ssn("ssn")
                .ownerUserId(userId)
                .primaryAddress(createAddress())
                .mailingAddress(createAddress())
                .build();
    }

    private IndividualUserLink createIndividualUserLink() {
        return IndividualUserLink.builder()
                .id(UUID.randomUUID())
                .accessLevel(ProfileAccessLevel.ADMIN)
                .profile(createIndividual())
                .userId(UUID.randomUUID())
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

    private IndividualProfileCreateModel profileCreateModel() {
        IndividualProfileCreateModel individualProfileCreateModel =
                new IndividualProfileCreateModel();
        individualProfileCreateModel.setSsn("ssn");
        individualProfileCreateModel.setOwnerUserId(userId);
        individualProfileCreateModel.setPrimaryAddress(createAddressModel());
        individualProfileCreateModel.setMailingAddress(createAddressModel());
        return individualProfileCreateModel;
    }

    private IndividualProfileUpdateModel profileUpdateModel() {
        IndividualProfileUpdateModel individualProfileUpdateModel =
                new IndividualProfileUpdateModel();
        individualProfileUpdateModel.setSsn("ssn");
        individualProfileUpdateModel.setOwnerUserId(userId);
        individualProfileUpdateModel.setPrimaryAddress(createAddressModel());
        individualProfileUpdateModel.setMailingAddress(createAddressModel());
        return individualProfileUpdateModel;
    }
}
