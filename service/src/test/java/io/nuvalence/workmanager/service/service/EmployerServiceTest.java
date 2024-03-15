package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.*;
import io.nuvalence.workmanager.service.models.EmployerFilters;
import io.nuvalence.workmanager.service.models.auditevents.*;
import io.nuvalence.workmanager.service.repository.EmployerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmployerServiceTest {

    @Mock private EmployerRepository repository;
    @Mock private AuditEventService employerAuditEventService;

    private EmployerService service;

    @BeforeEach
    public void setUp() {
        service = new EmployerService(repository, employerAuditEventService);
    }

    @Test
    void getEmployersByFilters() {
        Employer employer = Employer.builder().id(UUID.randomUUID()).build();
        Page<Employer> employerPageExpected = new PageImpl<>(Collections.singletonList(employer));

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(employerPageExpected);

        Page<Employer> employerPageResult =
                service.getEmployersByFilters(
                        EmployerFilters.builder()
                                .sortBy("legalName")
                                .sortOrder("ASC")
                                .pageNumber(0)
                                .pageSize(10)
                                .fein("fein")
                                .name("name")
                                .type("LLC")
                                .industry("industry")
                                .build());

        assertEquals(employerPageExpected, employerPageResult);
    }

    @Test
    void getEmployerById_Success() {
        Employer employer = Employer.builder().id(UUID.randomUUID()).build();

        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(employer));

        Optional<Employer> employerResult = service.getEmployerById(UUID.randomUUID());

        assertTrue(employerResult.isPresent());
        assertEquals(employer, employerResult.get());
    }

    @Test
    void getEmployerById_Null() {
        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

        Optional<Employer> employerResult = service.getEmployerById(UUID.randomUUID());

        assertTrue(employerResult.isEmpty());
    }

    @Test
    void getEmployerByIdWithNullId() {
        Optional<Employer> employerResult = service.getEmployerById(null);
        assertTrue(employerResult.isEmpty());
    }

    @Test
    void saveEmployer() {
        Employer employer =
                Employer.builder()
                        .id(UUID.randomUUID())
                        .mailingAddress(createAddress())
                        .locations(List.of(createAddress(), createAddress()))
                        .build();

        when(repository.save(any(Employer.class))).thenReturn(employer);

        Employer employerResult = service.saveEmployer(employer);

        assertEquals(employer, employerResult);
    }

    @Test
    void postAuditEventForEmployerCreated() {
        Employer employer =
                Employer.builder()
                        .id(UUID.randomUUID())
                        .createdBy(UUID.randomUUID().toString())
                        .build();

        ProfileCreatedAuditEventDto profileInfo =
                new ProfileCreatedAuditEventDto(employer.getCreatedBy());

        service.postAuditEventForEmployerCreated(employer);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(employerAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(employer.getCreatedBy())
                        .userId(employer.getCreatedBy())
                        .summary("Profile Created.")
                        .businessObjectId(employer.getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInfo.toJson(), AuditActivityType.EMPLOYER_PROFILE_CREATED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForEmployerProfileUserAdded() {
        EmployerUserLink employerUserLink =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .profile(Employer.builder().id(UUID.randomUUID()).build())
                        .createdBy(UUID.randomUUID().toString())
                        .profileAccessLevel(ProfileAccessLevel.ADMIN)
                        .build();

        ProfileUserAddedAuditEventDto profileInfo =
                new ProfileUserAddedAuditEventDto(
                        employerUserLink.getProfile().getId().toString(),
                        employerUserLink.getUserId().toString(),
                        employerUserLink.getProfileAccessLevel().toString());

        service.postAuditEventForEmployerlProfileUserAdded(employerUserLink);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(employerAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(employerUserLink.getCreatedBy())
                        .userId(employerUserLink.getUserId().toString())
                        .summary("Employer Profile User Added.")
                        .businessObjectId(employerUserLink.getProfile().getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInfo.toJson(), AuditActivityType.EMPLOYER_PROFILE_USER_ADDED)
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
        EmployerUserLink employerUserLink =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .profile(Employer.builder().id(UUID.randomUUID()).build())
                        .createdBy(UUID.randomUUID().toString())
                        .profileAccessLevel(ProfileAccessLevel.ADMIN)
                        .build();

        ProfileUserRemovedAuditEventDto profileInfo =
                new ProfileUserRemovedAuditEventDto(
                        employerUserLink.getProfile().getId().toString(),
                        employerUserLink.getUserId().toString());

        String lastUpdatedBy = UUID.randomUUID().toString();
        employerUserLink.setLastUpdatedBy(lastUpdatedBy);

        service.postAuditEventForEmployerProfileUserRemoved(employerUserLink);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(employerAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(lastUpdatedBy)
                        .userId(employerUserLink.getUserId().toString())
                        .summary("Employer Profile User Removed.")
                        .businessObjectId(employerUserLink.getProfile().getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInfo.toJson(), AuditActivityType.EMPLOYER_PROFILE_USER_REMOVED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForProfileInvitationSent() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();
        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);

        service.postAuditEventForEmployerProfileInvites(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_SENT, userId);

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Sent")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInviteInfo.toJson(), AuditActivityType.PROFILE_INVITATION_SENT)
                        .build();

        verify(employerAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

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

        service.postAuditEventForEmployerProfileInvites(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_CLAIMED, userId);

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(employerAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Claimed")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
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

        service.postAuditEventForEmployerProfileInvites(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_DELETED, userId);

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(employerAuditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Deleted")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
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
