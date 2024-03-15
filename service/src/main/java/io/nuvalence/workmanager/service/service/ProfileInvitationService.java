package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.models.ProfileInvitationFilters;
import io.nuvalence.workmanager.service.repository.ProfileInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
/**
 * Service for managing profile invitations.
 */
public class ProfileInvitationService {

    private final ProfileInvitationRepository repository;
    private final SendNotificationService sendNotificationService;

    public ProfileInvitation saveProfileInvitation(
            ProfileType profileType,
            String profileDisplayName,
            ProfileInvitation profileInvitation) {
        if (profileInvitation.getEmail() == null || profileInvitation.getEmail().isBlank()) {
            throw new ProvidedDataException("Email is required");
        }
        if (profileInvitation.getAccessLevel() == null) {
            throw new ProvidedDataException("Access level is required");
        }

        Optional<ProfileInvitation> optionalProfileInvitation =
                getActiveInvitationForEmailAndId(
                        profileInvitation.getEmail(), profileInvitation.getProfileId());
        if (optionalProfileInvitation.isPresent()) {
            throw new ConflictException("Invitation already exists for this email");
        }

        profileInvitation.setType(profileType);
        profileInvitation.setClaimed(false);

        ProfileInvitation savedProfileInvitation = saveProfileInvitation(profileInvitation);

        sendNotificationService.sendDirectNotification(
                savedProfileInvitation, profileDisplayName, "ProfileInvitationTemplate");

        return savedProfileInvitation;
    }

    public ProfileInvitation saveProfileInvitation(ProfileInvitation profileInvitation) {
        return repository.save(profileInvitation);
    }

    public Page<ProfileInvitation> getProfileInvitationsByFilters(
            ProfileInvitationFilters filters) {
        return repository.findAll(
                filters.getProfileInvitationSpecification(), filters.getPageRequest());
    }

    public Optional<ProfileInvitation> getActiveInvitationForEmailAndId(
            String email, UUID profileId) {
        return repository.findFirstByEmailAndProfileIdAndExpiresAfter(
                email, profileId, OffsetDateTime.now());
    }

    public Optional<ProfileInvitation> getInvitationByIdAndType(UUID id, ProfileType type) {
        return repository.findByIdAndType(id, type);
    }

    public Optional<ProfileInvitation> getProfileInvitationById(UUID id) {
        return repository.findById(id);
    }

    public Optional<ProfileInvitation> getProfileInvitationByIdAndEmail(
            UUID invitationId, String email) {
        return repository.findByIdAndEmail(invitationId, email);
    }

    public void deleteProfileInvitation(UUID profileInvitationId) {
        repository.deleteById(profileInvitationId);
    }
}
