package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.profile.Address;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.models.EmployerFilters;
import io.nuvalence.workmanager.service.models.auditevents.*;
import io.nuvalence.workmanager.service.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
/**
 * Service for managing employer profiles.
 */
public class EmployerService {
    private final EmployerRepository repository;
    private final AuditEventService auditEventService;

    public Page<Employer> getEmployersByFilters(final EmployerFilters filters) {
        return repository.findAll(
                filters.getEmployerProfileSpecification(), filters.getPageRequest());
    }

    /**
     * Gets an employer profile by ID.
     *
     * @param id the ID of the employer profile to get
     * @return the employer profile
     */
    public Optional<Employer> getEmployerById(final UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        return repository.findById(id);
    }

    /**
     * Saves a single employer profile.
     *
     * @param employer the employer profile to save
     * @return the saved employer profile
     */
    public Employer saveEmployer(final Employer employer) {
        if (employer.getMailingAddress() != null) {
            employer.getMailingAddress().setEmployerForMailing(employer);
        }

        if (employer.getLocations() != null) {
            for (Address location : employer.getLocations()) {
                location.setEmployerForLocations(employer);
            }
        }

        return repository.save(employer);
    }

    /**
     * Publishes an audit event for an employer profile being created.
     *
     * @param profile the employer profile that was created
     * @return the UUID of the audit event that was published
     */
    public UUID postAuditEventForEmployerCreated(Employer profile) {

        ProfileCreatedAuditEventDto profileInfo =
                new ProfileCreatedAuditEventDto(profile.getCreatedBy());

        final String summary = "Profile Created.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(profile.getCreatedBy())
                        .userId(profile.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(profile.getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInfo.toJson(), AuditActivityType.EMPLOYER_PROFILE_CREATED)
                        .build();

        return auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForEmployerlProfileUserAdded(EmployerUserLink employerUserLink) {

        ProfileUserAddedAuditEventDto profileUserInfo =
                new ProfileUserAddedAuditEventDto(
                        employerUserLink.getProfile().getId().toString(),
                        employerUserLink.getUserId().toString(),
                        employerUserLink.getProfileAccessLevel().getValue());

        final String summary = "Employer Profile User Added.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(employerUserLink.getCreatedBy())
                        .userId(employerUserLink.getUserId().toString())
                        .summary(summary)
                        .businessObjectId(employerUserLink.getProfile().getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileUserInfo.toJson(),
                                AuditActivityType.EMPLOYER_PROFILE_USER_ADDED)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForEmployerProfileUserRemoved(EmployerUserLink employerUserLink) {

        ProfileUserRemovedAuditEventDto profileUserInfo =
                new ProfileUserRemovedAuditEventDto(
                        employerUserLink.getProfile().getId().toString(),
                        employerUserLink.getUserId().toString());

        final String summary = "Employer Profile User Removed.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(employerUserLink.getLastUpdatedBy())
                        .userId(employerUserLink.getUserId().toString())
                        .summary(summary)
                        .businessObjectId(employerUserLink.getProfile().getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileUserInfo.toJson(),
                                AuditActivityType.EMPLOYER_PROFILE_USER_REMOVED)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForEmployerProfileInvites(
            ProfileInvitation profileInvitation,
            AuditActivityType auditActivityType,
            String userId) {
        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        final String summary =
                switch (auditActivityType) {
                    case PROFILE_INVITATION_SENT -> "Profile Invitation Sent";
                    case PROFILE_INVITATION_CLAIMED -> "Profile Invitation Claimed";
                    case PROFILE_INVITATION_DELETED -> "Profile Invitation Deleted";
                    default -> "Profile Invitation Event";
                };

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary(summary)
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInviteInfo.toJson(), auditActivityType)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }
}
