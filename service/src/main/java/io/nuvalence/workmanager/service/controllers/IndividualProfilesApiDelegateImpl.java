package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.audit.AuditableAction;
import io.nuvalence.workmanager.service.audit.profile.IndividualProfileDataChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.profile.IndividualProfileOwnerChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.profile.IndividualProfileUserAccessLevelChangedAuditHandler;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.controllers.IndividualProfilesApiDelegate;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkResponseModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkUpdateModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileUpdateModel;
import io.nuvalence.workmanager.service.generated.models.PageIndividualLinksResponseModel;
import io.nuvalence.workmanager.service.generated.models.PageIndividualProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.PageProfileInvitationResponse;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationResponse;
import io.nuvalence.workmanager.service.mapper.IndividualMapper;
import io.nuvalence.workmanager.service.mapper.IndividualUserLinkMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.mapper.ProfileInvitationMapper;
import io.nuvalence.workmanager.service.models.IndividualFilters;
import io.nuvalence.workmanager.service.models.IndividualUserLinksFilters;
import io.nuvalence.workmanager.service.models.ProfileInvitationFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.service.AuditEventService;
import io.nuvalence.workmanager.service.service.IndividualService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import io.nuvalence.workmanager.service.service.ProfileInvitationService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndividualProfilesApiDelegateImpl implements IndividualProfilesApiDelegate {
    public static final String INVITATION_NOT_FOUND = "Invitation not found";
    private final AuthorizationHandler authorizationHandler;
    private final IndividualService individualService;
    private final IndividualMapper individualMapper;
    private final IndividualUserLinkService individualUserLinkService;
    private final IndividualUserLinkMapper individualUserLinkMapper;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final UserManagementService userManagementService;
    private final RequestContextTimestamp requestContextTimestamp;
    private final AuditEventService individualAuditEventService;
    private final ProfileInvitationService profileInvitationService;
    private final ProfileInvitationMapper profileInvitationMapper;

    private static final String INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE =
            "individual_profile_invitation";
    private static final String INDIVIDUAL_PROFILE_NOT_FOUND_MSG = "Individual profile not found";
    private static final String CREATION_AUDIT_EVENT_ERR_MSG =
            "An error has occurred when recording a creation audit event for an";

    @Override
    public ResponseEntity<IndividualProfileResponseModel> postIndividualProfile(
            IndividualProfileCreateModel individualProfileCreateModel) {
        if (!authorizationHandler.isAllowed("create", Individual.class)) {
            throw new ForbiddenException();
        }

        Individual individual =
                individualService.saveIndividual(
                        individualMapper.createModelToIndividual(individualProfileCreateModel));

        postAuditEventForIndividualProfileCreated(individual);

        IndividualProfileResponseModel individualProfileResponseModel =
                individualMapper.individualToResponseModel(individual);

        return ResponseEntity.status(HttpStatus.OK).body(individualProfileResponseModel);
    }

    @Override
    public ResponseEntity<IndividualProfileResponseModel> getIndividualProfile(UUID profileId) {
        final IndividualProfileResponseModel individualProfileResponseModel =
                individualService
                        .getIndividualById(profileId)
                        .filter(
                                individualInstance ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", individualInstance))
                        .map(individualMapper::individualToResponseModel)
                        .orElseThrow(() -> new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG));

        return ResponseEntity.status(200).body(individualProfileResponseModel);
    }

    @Override
    public ResponseEntity<IndividualProfileResponseModel> updateIndividualProfile(
            UUID profileId, IndividualProfileUpdateModel individualProfileUpdateModel) {
        if (!authorizationHandler.isAllowed("update", Individual.class)) {
            throw new ForbiddenException();
        }

        Optional<Individual> optionalIndividual = individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }
        Individual existingIndividual = optionalIndividual.get();

        try {
            final Individual savedIndividual =
                    AuditableAction.builder(Individual.class)
                            .auditHandler(
                                    new IndividualProfileDataChangedAuditHandler(
                                            individualAuditEventService))
                            .auditHandler(
                                    new IndividualProfileOwnerChangedAuditHandler(
                                            individualAuditEventService))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    individual -> {
                                        Individual individualToBeSaved =
                                                individualMapper.updateModelToIndividual(
                                                        individualProfileUpdateModel);
                                        individualToBeSaved.setId(profileId);
                                        individualToBeSaved.setCreatedBy(
                                                existingIndividual.getCreatedBy());
                                        individualToBeSaved.setCreatedTimestamp(
                                                existingIndividual.getCreatedTimestamp());

                                        individualService.saveIndividual(individualToBeSaved);

                                        return individualService
                                                .getIndividualById(profileId)
                                                .orElseThrow(
                                                        () ->
                                                                new UnexpectedException(
                                                                        INDIVIDUAL_PROFILE_NOT_FOUND_MSG
                                                                                + " after saving"));
                                    })
                            .build()
                            .execute(existingIndividual);

            return ResponseEntity.status(200)
                    .body(individualMapper.individualToResponseModel(savedIndividual));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public ResponseEntity<PageIndividualProfileResponseModel> getIndividualProfiles(
            UUID ownerUserId,
            String ssn,
            String name,
            String email,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", Individual.class)) {
            throw new ForbiddenException();
        }

        Page<IndividualProfileResponseModel> results =
                individualService
                        .getIndividualsByFilters(
                                new IndividualFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        ownerUserId,
                                        ssn,
                                        email,
                                        name,
                                        userManagementService))
                        .map(individualMapper::individualToResponseModel);

        PageIndividualProfileResponseModel response = new PageIndividualProfileResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));
        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<IndividualProfileLinkResponseModel> updateIndividualProfileLink(
            UUID profileId, IndividualProfileLinkUpdateModel individualProfileLinkUpdateModel) {
        if (!authorizationHandler.isAllowed("update", IndividualUserLink.class)) {
            throw new ForbiddenException();
        }

        Optional<Individual> optionalIndividual = individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }

        IndividualUserLink savedIndividualLink =
                saveIndividualUserLink(
                        profileId,
                        optionalIndividual.get(),
                        ProfileAccessLevel.fromValue(
                                individualProfileLinkUpdateModel.getAccessLevel()),
                        individualProfileLinkUpdateModel.getUserId());

        return ResponseEntity.status(200)
                .body(
                        individualUserLinkMapper.individualLUserlinkToResponseModel(
                                savedIndividualLink));
    }

    private IndividualUserLink saveIndividualUserLink(
            UUID profileId,
            Individual individual,
            ProfileAccessLevel profileAccessLevel,
            UUID userId) {

        Optional<IndividualUserLink> optionalIndividualLink =
                individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId);
        IndividualUserLink individualLinkToBeSaved =
                IndividualUserLink.builder()
                        .userId(userId)
                        .accessLevel(profileAccessLevel)
                        .profile(individual)
                        .build();

        if (optionalIndividualLink.isPresent()) {
            individualLinkToBeSaved.setId(optionalIndividualLink.get().getId());
            individualLinkToBeSaved.setCreatedBy(optionalIndividualLink.get().getCreatedBy());
            individualLinkToBeSaved.setCreatedTimestamp(
                    optionalIndividualLink.get().getCreatedTimestamp());
        }

        individualLinkToBeSaved.setProfile(individual);
        individualLinkToBeSaved.setUserId(userId);
        individualLinkToBeSaved.setAccessLevel(profileAccessLevel);

        try {
            if (optionalIndividualLink.isPresent()) {
                AuditableAction.builder(IndividualUserLink.class)
                        .auditHandler(
                                new IndividualProfileUserAccessLevelChangedAuditHandler(
                                        individualAuditEventService))
                        .requestContextTimestamp(requestContextTimestamp)
                        .action(
                                individualUserLink ->
                                        individualUserLinkService.saveIndividualUserLink(
                                                individualLinkToBeSaved))
                        .build()
                        .execute(optionalIndividualLink.get());
            } else {
                individualUserLinkService.saveIndividualUserLink(individualLinkToBeSaved);
            }

        } catch (Exception e) {
            throw new UnexpectedException(e);
        }

        IndividualUserLink savedIndividualLink =
                individualUserLinkService
                        .getIndividualUserLinkByProfileAndUserId(profileId, userId)
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "Individual profile link not found after saving"));

        if (optionalIndividualLink.isEmpty()) {
            postAuditEventForIndividualProfileUserCreated(savedIndividualLink);
        }
        return savedIndividualLink;
    }

    @Override
    public ResponseEntity<PageIndividualLinksResponseModel> getIndividualLinks(
            UUID profileId,
            UUID userid,
            String name,
            String email,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        if (!authorizationHandler.isAllowed("view", IndividualUserLink.class)) {
            throw new ForbiddenException();
        }

        Optional<Individual> optionalIndividual = individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }

        Page<IndividualProfileLinkResponseModel> results =
                individualUserLinkService
                        .getIndividualLinksByFilters(
                                new IndividualUserLinksFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        userid,
                                        email,
                                        name,
                                        userManagementService))
                        .map(individualUserLinkMapper::individualLUserlinkToResponseModel);

        PageIndividualLinksResponseModel response = new PageIndividualLinksResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));
        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<Void> deleteIndividualProfileLink(UUID profileId, UUID userId) {
        if (!authorizationHandler.isAllowed("delete", IndividualUserLink.class)) {
            throw new ForbiddenException();
        }

        Optional<Individual> optionalIndividual = individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }
        Optional<IndividualUserLink> existingIndividualUserLink =
                individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId);

        if (existingIndividualUserLink.isEmpty()) {
            throw new NotFoundException("individual user link not found");
        }

        individualUserLinkService.deleteIndividualUserLink(existingIndividualUserLink.get());

        postAuditEventForIndividualProfileUserRemoved(existingIndividualUserLink.get());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> postIndividualProfileInvitation(
            UUID profileId, ProfileInvitationRequestModel profileInvitationRequestModel) {

        validateProfileUserPermissions(
                profileId,
                "invite",
                INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE,
                "User is not authorized to invite users to this profile");

        Optional<Individual> individual = individualService.getIndividualById(profileId);
        if (individual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }

        ProfileInvitation profileInvitation =
                profileInvitationMapper.createModelToProfileInvitation(
                        profileId, profileInvitationRequestModel);

        User ownerUser = userManagementService.getUser(individual.get().getOwnerUserId());

        ProfileInvitation savedProfileInvitation =
                profileInvitationService.saveProfileInvitation(
                        ProfileType.INDIVIDUAL, ownerUser.getDisplayName(), profileInvitation);

        ProfileInvitationResponse profileInvitationResponse =
                profileInvitationMapper.profileInvitationToResponseModel(savedProfileInvitation);

        postAuditEventForProfileInvitation(
                profileInvitation,
                AuditActivityType.PROFILE_INVITATION_SENT,
                SecurityContextUtility.getAuthenticatedUserId());

        return ResponseEntity.status(200).body(profileInvitationResponse);
    }

    @Override
    public ResponseEntity<PageProfileInvitationResponse> getIndividualProfileInvitations(
            UUID profileId,
            String accessLevel,
            String email,
            Boolean exactEmailMatch,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("read", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE)) {
            throw new ForbiddenException();
        }

        if (UserUtility.getAuthenticatedUserType().equalsIgnoreCase(UserType.PUBLIC.getValue())) {
            final String userId = SecurityContextUtility.getAuthenticatedUserId();
            Optional<IndividualUserLink> profilesLinkOptional =
                    individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, UUID.fromString(userId));
            if (profilesLinkOptional.isEmpty()) {
                throw new ForbiddenException("User does not have access to this profile");
            }
        }

        Page<ProfileInvitationResponse> results =
                profileInvitationService
                        .getProfileInvitationsByFilters(
                                new ProfileInvitationFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        accessLevel,
                                        email,
                                        exactEmailMatch,
                                        profileId,
                                        ProfileType.INDIVIDUAL.getValue()))
                        .map(profileInvitationMapper::profileInvitationToResponseModel);

        PageProfileInvitationResponse response = new PageProfileInvitationResponse();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<Void> claimIndividualProfileInvitation(UUID invitationId) {
        if (!authorizationHandler.isAllowed("claim", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE)) {
            throw new ForbiddenException();
        }

        // validates that the current logged user is the owner of the invitation
        final String email = SecurityContextUtility.getAuthenticatedUserEmail();
        Optional<ProfileInvitation> optionalProfileInvitation =
                profileInvitationService.getProfileInvitationByIdAndEmail(invitationId, email);

        if (optionalProfileInvitation.isEmpty()) {
            throw new NotFoundException(INVITATION_NOT_FOUND);
        }

        ProfileInvitation profileInvitation = optionalProfileInvitation.get();

        UUID profileId = profileInvitation.getProfileId();

        Individual individual =
                individualService
                        .getIndividualById(profileId)
                        .orElseThrow(() -> new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG));

        if (profileInvitation.getClaimed() != null && profileInvitation.getClaimed()) {
            throw new ConflictException("Invitation has already been claimed");
        }

        final String userId = SecurityContextUtility.getAuthenticatedUserId();
        saveIndividualUserLink(
                profileId, individual, profileInvitation.getAccessLevel(), UUID.fromString(userId));

        profileInvitation.setClaimed(true);
        profileInvitation.setClaimedTimestamp(OffsetDateTime.now());
        profileInvitationService.saveProfileInvitation(profileInvitation);

        postAuditEventForProfileInvitation(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_CLAIMED, userId);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteIndividualProfileInvitation(
            UUID profileId, UUID invitationId) {

        validateProfileUserPermissions(
                profileId,
                "delete",
                INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE,
                "User is not authorized to delete invites to this profile");

        ProfileInvitation profileInvitation =
                profileInvitationService
                        .getProfileInvitationById(invitationId)
                        .orElseThrow(() -> new NotFoundException(INVITATION_NOT_FOUND));

        if (Boolean.TRUE.equals(profileInvitation.getClaimed())) {
            throw new BusinessLogicException(
                    "Cannot delete an invitation that has already been claimed");
        }

        profileInvitationService.deleteProfileInvitation(invitationId);

        postAuditEventForProfileInvitation(
                profileInvitation,
                AuditActivityType.PROFILE_INVITATION_DELETED,
                SecurityContextUtility.getAuthenticatedUserId());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> getIndividualProfileInvitationById(
            UUID invitationId) {
        if (!authorizationHandler.isAllowed("read", INDIVIDUAL_PROFILE_INVITATION_CERBOS_TYPE)) {
            throw new ForbiddenException();
        }

        ProfileInvitation invitation =
                profileInvitationService
                        .getInvitationByIdAndType(invitationId, ProfileType.INDIVIDUAL)
                        .orElseThrow(() -> new NotFoundException(INVITATION_NOT_FOUND));

        if (UserUtility.getAuthenticatedUserType().equalsIgnoreCase(UserType.PUBLIC.getValue())) {
            final String userId = SecurityContextUtility.getAuthenticatedUserId();
            Optional<IndividualUserLink> profilesLinkOptional =
                    individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                            invitation.getProfileId(), UUID.fromString(userId));
            if (profilesLinkOptional.isEmpty()
                    && !SecurityContextUtility.getAuthenticatedUserEmail()
                            .equals(invitation.getEmail())) {
                throw new ForbiddenException("User does not have access to this profile");
            }
        }

        return ResponseEntity.ok(
                profileInvitationMapper.profileInvitationToResponseModel(invitation));
    }

    private void postAuditEventForIndividualProfileCreated(Individual profile) {
        try {
            individualService.postAuditEventForIndividualCreated(profile);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " employer profile with user id %s for profile with id %s.",
                            profile.getCreatedBy(),
                            profile.getId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForIndividualProfileUserCreated(
            IndividualUserLink individualUserLink) {
        try {
            individualService.postAuditEventForIndividualProfileUserAdded(individualUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " individual  profile user with owner user id %s for user"
                                    + " with id %s.",
                            individualUserLink.getProfile().getCreatedBy(),
                            individualUserLink.getUserId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForIndividualProfileUserRemoved(
            IndividualUserLink individualUserLink) {
        try {
            individualService.postAuditEventForIndividualProfileUserRemoved(individualUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " individual  profile user with owner user id %s for user"
                                    + " with id %s.",
                            individualUserLink.getProfile().getCreatedBy(),
                            individualUserLink.getUserId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForProfileInvitation(
            ProfileInvitation profileInvitation,
            AuditActivityType auditActivityType,
            String userId) {
        try {
            individualService.postAuditEventForIndividualProfileInvites(
                    profileInvitation, auditActivityType, userId);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "An error has occurred when recording a audit event for profile"
                                + " invitation invite with ID %s for triggered with id %s. Action"
                                + " attempted was %s",
                            profileInvitation.getId(), userId, auditActivityType);
            log.error(errorMessage, e);
        }
    }

    private void validateProfileUserPermissions(
            UUID profileId, String action, String type, String exceptionMessage) {

        if (!authorizationHandler.isAllowed(action, type)) {
            if (!UserUtility.getAuthenticatedUserType()
                    .equalsIgnoreCase(UserType.PUBLIC.getValue())) {
                throw new ForbiddenException(exceptionMessage);
            }

            final String userId = SecurityContextUtility.getAuthenticatedUserId();
            Optional<IndividualUserLink> profilesLinkOptional =
                    individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                            profileId, UUID.fromString(userId));

            if (profilesLinkOptional.isEmpty()
                    || !profilesLinkOptional
                            .get()
                            .getAccessLevel()
                            .equals(ProfileAccessLevel.ADMIN)) {
                throw new ForbiddenException(exceptionMessage);
            }
        }
    }
}
