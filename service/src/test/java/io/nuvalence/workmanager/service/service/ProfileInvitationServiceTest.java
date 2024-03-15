package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.models.ProfileInvitationFilters;
import io.nuvalence.workmanager.service.repository.ProfileInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProfileInvitationServiceTest {

    @Mock private ProfileInvitationRepository repository;
    @Mock private SendNotificationService sendNotificationService;

    private ProfileInvitationService service;

    @BeforeEach
    void setUp() {
        service = new ProfileInvitationService(repository, sendNotificationService);
    }

    @Test
    void saveProfileInvitation() {
        ProfileInvitation invitation = new ProfileInvitation();
        when(repository.save(any())).thenReturn(invitation);

        ProfileInvitation result = service.saveProfileInvitation(invitation);

        assertEquals(invitation, result);
        verify(repository).save(invitation);
    }

    @Test
    void getProfileInvitationsByFilters() {
        Page<ProfileInvitation> invitations = new PageImpl<>(List.of(new ProfileInvitation()));
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invitations);

        final ProfileInvitationFilters profileInvitationFilters =
                new ProfileInvitationFilters(
                        "createdTimestamp", "ASC", 0, 2, null, null, null, null, null);

        Page<ProfileInvitation> result =
                service.getProfileInvitationsByFilters(profileInvitationFilters);

        assertEquals(invitations, result);
        verify(repository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getActiveInvitationForEmail_Found() {
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setProfileId(UUID.randomUUID());
        invitation.setEmail("test@example.com");
        OffsetDateTime expires = OffsetDateTime.now().plusDays(1);
        invitation.setExpires(expires);

        when(repository.findFirstByEmailAndProfileIdAndExpiresAfter(anyString(), any(), any()))
                .thenReturn(Optional.of(invitation));

        Optional<ProfileInvitation> result =
                service.getActiveInvitationForEmailAndId(
                        "test@example.com", invitation.getProfileId());

        assertTrue(result.isPresent());
        assertEquals(invitation, result.get());
    }

    @Test
    void getActiveInvitationForEmail_NotFound() {
        UUID profileId = UUID.randomUUID();
        when(repository.findFirstByEmailAndProfileIdAndExpiresAfter(anyString(), any(), any()))
                .thenReturn(Optional.empty());

        Optional<ProfileInvitation> result =
                service.getActiveInvitationForEmailAndId("test@example.com", profileId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getProfileInvitationById_Found() {
        ProfileInvitation invitation = new ProfileInvitation();
        when(repository.findById(any())).thenReturn(Optional.of(invitation));

        Optional<ProfileInvitation> result = service.getProfileInvitationById(UUID.randomUUID());

        assertTrue(result.isPresent());
        assertEquals(invitation, result.get());
    }

    @Test
    void getProfileInvitationById_NotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        Optional<ProfileInvitation> result = service.getProfileInvitationById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteProfileInvitation() {
        service.deleteProfileInvitation(UUID.randomUUID());

        verify(repository).deleteById(any());
    }

    @Test
    void saveProfileInvitation_Success() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setEmail("test@example.com");
        invitation.setAccessLevel(ProfileAccessLevel.ADMIN);

        when(repository.save(any())).thenReturn(invitation);

        ProfileInvitation result =
                service.saveProfileInvitation(ProfileType.EMPLOYER, "displayName", invitation);

        assertEquals(invitation, result);
        verify(repository).save(invitation);
    }

    @Test
    void saveProfileInvitation_NoEmail() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setAccessLevel(ProfileAccessLevel.ADMIN);

        assertThrows(
                ProvidedDataException.class,
                () -> {
                    service.saveProfileInvitation(ProfileType.EMPLOYER, "displayName", invitation);
                });
    }

    @Test
    void saveProfileInvitation_NoAccessLevel() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setEmail("test@example.com");

        assertThrows(
                ProvidedDataException.class,
                () -> {
                    service.saveProfileInvitation(ProfileType.EMPLOYER, "displayName", invitation);
                });
    }

    @Test
    void saveProfileInvitation_InvitationExists() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setEmail("test@example.com");
        invitation.setAccessLevel(ProfileAccessLevel.ADMIN);

        when(repository.findFirstByEmailAndProfileIdAndExpiresAfter(anyString(), any(), any()))
                .thenReturn(Optional.of(invitation));

        assertThrows(
                ConflictException.class,
                () -> {
                    service.saveProfileInvitation(ProfileType.EMPLOYER, "displayName", invitation);
                });
    }

    @Test
    void getProfileInvitationByIdAndEmail_Found() {
        ProfileInvitation invitation = new ProfileInvitation();
        UUID invitationId = UUID.randomUUID();
        String email = "test@example.com";

        when(repository.findByIdAndEmail(invitationId, email)).thenReturn(Optional.of(invitation));

        Optional<ProfileInvitation> result =
                service.getProfileInvitationByIdAndEmail(invitationId, email);

        assertTrue(result.isPresent());
        assertEquals(invitation, result.get());
    }

    @Test
    void getProfileInvitationByIdAndEmail_NotFound() {
        UUID invitationId = UUID.randomUUID();
        String email = "test@example.com";

        when(repository.findByIdAndEmail(invitationId, email)).thenReturn(Optional.empty());

        Optional<ProfileInvitation> result =
                service.getProfileInvitationByIdAndEmail(invitationId, email);

        assertTrue(result.isEmpty());
    }

    @Test
    void getInvitationByIdAndType() {
        UUID id = UUID.randomUUID();
        ProfileType type = ProfileType.EMPLOYER;

        ProfileInvitation invitation = ProfileInvitation.builder().build();
        when(repository.findByIdAndType(id, type)).thenReturn(Optional.of(invitation));

        Optional<ProfileInvitation> optionalProfileInvitation =
                service.getInvitationByIdAndType(id, type);
        assertTrue(optionalProfileInvitation.isPresent());
        assertEquals(invitation, optionalProfileInvitation.get());
    }
}
