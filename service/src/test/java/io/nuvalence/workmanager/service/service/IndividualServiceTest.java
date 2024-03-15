package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.profile.*;
import io.nuvalence.workmanager.service.models.IndividualFilters;
import io.nuvalence.workmanager.service.models.auditevents.*;
import io.nuvalence.workmanager.service.repository.IndividualRepository;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class IndividualServiceTest {

    @Mock private IndividualRepository repository;
    @Mock private AuditEventService auditEventService;
    @Mock private UserManagementService userManagementService;

    private IndividualService service;

    @BeforeEach
    public void setUp() {
        service = new IndividualService(repository, auditEventService, userManagementService);
    }

    @Test
    void getIndividualById_Success() {
        UUID individualId = UUID.randomUUID();
        Individual individual = Individual.builder().id(individualId).build();

        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(individual));

        Optional<Individual> individualResult = service.getIndividualById(individualId);

        assertTrue(individualResult.isPresent());
        assertEquals(individual, individualResult.get());
        assertEquals(individualId, individualResult.get().getId());
    }

    @Test
    void getIndividualById_Null() {
        Optional<Individual> individualResult = service.getIndividualById(null);

        assertTrue(individualResult.isEmpty());
    }

    @Test
    void saveIndividual() {
        UUID individualId = UUID.randomUUID();
        Individual individual =
                Individual.builder()
                        .id(individualId)
                        .primaryAddress(createAddress())
                        .mailingAddress(createAddress())
                        .build();

        when(repository.save(any(Individual.class))).thenReturn(individual);

        Individual individualResult = service.saveIndividual(individual);

        assertEquals(individual, individualResult);
    }

    @Test
    void postAuditEventForIndividualCreated() {
        Individual individual =
                Individual.builder()
                        .id(UUID.randomUUID())
                        .createdBy(UUID.randomUUID().toString())
                        .build();

        ProfileCreatedAuditEventDto profileInfo =
                new ProfileCreatedAuditEventDto(individual.getCreatedBy());

        service.postAuditEventForIndividualCreated(individual);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(individual.getCreatedBy())
                        .userId(individual.getCreatedBy())
                        .summary("Profile Created.")
                        .businessObjectId(individual.getId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(profileInfo.toJson(), AuditActivityType.INDIVIDUAL_PROFILE_CREATED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void createOrGetIndividualForCurrentUserTest_IndividualExists() {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            when(userManagementService.getUserOptional(any()))
                    .thenReturn(Optional.ofNullable(User.builder().build()));
            Individual individual = Individual.builder().id(UUID.randomUUID()).build();
            when(repository.findByOwnerUserId(any())).thenReturn(List.of(individual));

            Individual result = service.createOrGetIndividualForCurrentUser();

            assertEquals(individual, result);
        }
    }

    @Test
    void createOrGetIndividualForCurrentUserTest_IndividualDoesNotExist() {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            String userId = UUID.randomUUID().toString();
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId);
            when(userManagementService.getUserOptional(any()))
                    .thenReturn(Optional.ofNullable(User.builder().build()));
            when(repository.findByOwnerUserId(any())).thenReturn(Collections.EMPTY_LIST);
            when(repository.save(any()))
                    .thenReturn(Individual.builder().ownerUserId(UUID.fromString(userId)).build());

            Individual result = service.createOrGetIndividualForCurrentUser();

            assertEquals(userId, result.getOwnerUserId().toString());
        }
    }

    @Test
    void postAuditEventForIndividualProfileUserAdded() {
        IndividualUserLink individualUserLink =
                IndividualUserLink.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .profile(
                                Individual.builder()
                                        .id(UUID.randomUUID())
                                        .ownerUserId(UUID.randomUUID())
                                        .build())
                        .createdBy(UUID.randomUUID().toString())
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .build();

        ProfileUserAddedAuditEventDto profileInfo =
                new ProfileUserAddedAuditEventDto(
                        individualUserLink.getProfile().getOwnerUserId().toString(),
                        individualUserLink.getUserId().toString(),
                        individualUserLink.getAccessLevel().toString());

        service.postAuditEventForIndividualProfileUserAdded(individualUserLink);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(individualUserLink.getCreatedBy())
                        .userId(String.valueOf(individualUserLink.getUserId()))
                        .summary("Individual Profile User Added.")
                        .businessObjectId(individualUserLink.getId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(profileInfo.toJson(), AuditActivityType.INDIVIDUAL_PROFILE_USER_ADDED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForIndividualProfileUserRemoved() {
        IndividualUserLink individualUserLink =
                IndividualUserLink.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .profile(Individual.builder().ownerUserId(UUID.randomUUID()).build())
                        .createdBy(UUID.randomUUID().toString())
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .build();

        ProfileUserRemovedAuditEventDto profileInfo =
                new ProfileUserRemovedAuditEventDto(
                        individualUserLink.getProfile().getOwnerUserId().toString(),
                        individualUserLink.getUserId().toString());

        service.postAuditEventForIndividualProfileUserRemoved(individualUserLink);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(individualUserLink.getCreatedBy())
                        .userId(individualUserLink.getCreatedBy())
                        .summary("Individual Profile User Removed.")
                        .businessObjectId(individualUserLink.getId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(
                                profileInfo.toJson(),
                                AuditActivityType.INDIVIDUAL_PROFILE_USER_REMOVED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void getIndividualByOwner_Null() {
        List<Individual> result = service.getIndividualsByOwner(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIndividualByOwner_Success() {
        UUID ownerId = UUID.randomUUID();

        when(repository.findByOwnerUserId(ownerId))
                .thenReturn(List.of(Individual.builder().ownerUserId(ownerId).build()));

        List<Individual> result = service.getIndividualsByOwner(ownerId);

        assertEquals(1, result.size());
        assertEquals(ownerId, result.get(0).getOwnerUserId());
    }

    @Test
    void getIndividualsByFiltersTest() {
        IndividualFilters filters = mock(IndividualFilters.class);

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        Individual.builder()
                                                .id(UUID.randomUUID())
                                                .ownerUserId(UUID.randomUUID())
                                                .build(),
                                        Individual.builder()
                                                .id(UUID.randomUUID())
                                                .ownerUserId(UUID.randomUUID())
                                                .build())));

        when(filters.getIndividualProfileSpecification()).thenReturn(mock(Specification.class));
        when(filters.getPageRequest()).thenReturn(PageRequest.of(0, 10));
        Page<Individual> result = service.getIndividualsByFilters(filters);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void createOrGetIndividualForCurrentUserTest_UserNotFound() {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            when(userManagementService.getUserOptional(any())).thenReturn(Optional.empty());

            assertThrows(
                    NotFoundException.class,
                    () -> {
                        service.createOrGetIndividualForCurrentUser();
                    },
                    "Current user not found");
        }
    }

    @Test
    void createOrGetIndividualForCurrentUserTest_HttpClientErrorException() {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            doThrow(HttpClientErrorException.class)
                    .when(userManagementService)
                    .getUserOptional(any());

            Exception exception =
                    assertThrows(
                            UnexpectedException.class,
                            () -> {
                                service.createOrGetIndividualForCurrentUser();
                            });

            assertEquals("Could not verify user existence", exception.getMessage());
        }
    }

    @Test
    void postAuditEventForProfileInvitationSent() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();

        service.postAuditEventForIndividualProfileInvites(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_SENT, userId);

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Sent")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(profileInviteInfo.toJson(), AuditActivityType.PROFILE_INVITATION_SENT)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForProfileInvitationClaimed() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();

        service.postAuditEventForIndividualProfileInvites(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_CLAIMED, userId);

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Claimed")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(
                                profileInviteInfo.toJson(),
                                AuditActivityType.PROFILE_INVITATION_CLAIMED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForProfileInvitationDeleted() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();

        service.postAuditEventForIndividualProfileInvites(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_DELETED, userId);

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Deleted")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(
                                profileInviteInfo.toJson(),
                                AuditActivityType.PROFILE_INVITATION_DELETED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    private Address createAddress() {
        return Address.builder()
                .address1("123 Main St")
                .city("Any-town")
                .state("CA")
                .postalCode("12345")
                .build();
    }

    private ProfileInvitation createProfileInvitation() {
        return ProfileInvitation.builder()
                .id(UUID.randomUUID())
                .profileId(UUID.randomUUID())
                .type(ProfileType.EMPLOYER)
                .accessLevel(ProfileAccessLevel.ADMIN)
                .email("test@example.com")
                .build();
    }
}
