package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.models.IndividualFilters;
import io.nuvalence.workmanager.service.models.auditevents.*;
import io.nuvalence.workmanager.service.repository.IndividualRepository;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Service for managing individual profiles.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IndividualService {
    private final IndividualRepository repository;
    private final AuditEventService auditEventService;
    private final UserManagementService userManagementService;

    public Individual saveIndividual(final Individual individual) {
        if (individual.getMailingAddress() != null) {
            individual.getMailingAddress().setIndividualForMailing(individual);
        }

        if (individual.getPrimaryAddress() != null) {
            individual.getPrimaryAddress().setIndividualForAddress(individual);
        }
        return repository.save(individual);
    }

    /**
     * Gets an individual profile by ID.
     *
     * @param id the ID of the individual profile to get
     * @return the individual profile
     */
    public Optional<Individual> getIndividualById(final UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        return repository.findById(id);
    }

    public List<Individual> getIndividualsByOwner(final UUID ownerId) {
        if (ownerId == null) {
            return Collections.emptyList();
        }

        return repository.findByOwnerUserId(ownerId);
    }

    public Page<Individual> getIndividualsByFilters(final IndividualFilters filters) {
        return repository.findAll(
                filters.getIndividualProfileSpecification(), filters.getPageRequest());
    }

    /**
     * Posts an audit event for an individual profile being created.
     *
     * @param profile the individual profile that was created
     * @return the ID of the audit event
     */
    public UUID postAuditEventForIndividualCreated(Individual profile) {

        ProfileCreatedAuditEventDto profileInfo =
                new ProfileCreatedAuditEventDto(profile.getCreatedBy());

        final String summary = "Profile Created.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(profile.getCreatedBy())
                        .userId(profile.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(profile.getId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(profileInfo.toJson(), AuditActivityType.INDIVIDUAL_PROFILE_CREATED)
                        .build();

        return auditEventService.sendAuditEvent(auditEvent);
    }

    public Individual createOrGetIndividualForCurrentUser() {
        UUID currentUserId = UUID.fromString(SecurityContextUtility.getAuthenticatedUserId());

        Optional<User> optionalUser;
        try {
            optionalUser = userManagementService.getUserOptional(currentUserId);
            if (optionalUser.isEmpty()) {
                throw new NotFoundException("Current user not found");
            }
        } catch (HttpClientErrorException e) {
            log.error("An error reaching user management occurred: ", e);
            throw new UnexpectedException("Could not verify user existence");
        }

        List<Individual> individuals = getIndividualsByOwner(currentUserId);

        if (!individuals.isEmpty()) {
            return individuals.stream()
                    .sorted(Comparator.comparing(Individual::getLastUpdatedTimestamp).reversed())
                    .findFirst()
                    .orElseThrow(
                            () -> new UnexpectedException("Could not find individual profile"));
        }

        return saveIndividual(Individual.builder().ownerUserId(currentUserId).build());
    }

    public void postAuditEventForIndividualProfileUserAdded(IndividualUserLink individualUserLink) {

        ProfileUserAddedAuditEventDto profileUserInfo =
                new ProfileUserAddedAuditEventDto(
                        individualUserLink.getProfile().getOwnerUserId().toString(),
                        individualUserLink.getUserId().toString(),
                        individualUserLink.getAccessLevel().getValue());

        final String summary = "Individual Profile User Added.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(individualUserLink.getCreatedBy())
                        .userId(individualUserLink.getUserId().toString())
                        .summary(summary)
                        .businessObjectId(individualUserLink.getId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(
                                profileUserInfo.toJson(),
                                AuditActivityType.INDIVIDUAL_PROFILE_USER_ADDED)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForIndividualProfileUserRemoved(
            IndividualUserLink individualUserLink) {

        ProfileUserRemovedAuditEventDto profileUserInfo =
                new ProfileUserRemovedAuditEventDto(
                        individualUserLink.getProfile().getOwnerUserId().toString(),
                        individualUserLink.getUserId().toString());

        final String summary = "Individual Profile User Removed.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(individualUserLink.getCreatedBy())
                        .userId(individualUserLink.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(individualUserLink.getId())
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(
                                profileUserInfo.toJson(),
                                AuditActivityType.INDIVIDUAL_PROFILE_USER_REMOVED)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForIndividualProfileInvites(
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
                        .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                        .data(profileInviteInfo.toJson(), auditActivityType)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }
}
