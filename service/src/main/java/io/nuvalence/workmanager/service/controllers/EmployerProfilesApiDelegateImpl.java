package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.audit.AuditableAction;
import io.nuvalence.workmanager.service.audit.profile.EmployerProfileDataChangedAuditHandler;
import io.nuvalence.workmanager.service.audit.profile.EmployerProfileUserAccessLevelChangedAuditHandler;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.controllers.EmployerProfilesApiDelegate;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileLinkRequestModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileLinkResponse;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileUpdateModel;
import io.nuvalence.workmanager.service.generated.models.PageEmployerProfileLink;
import io.nuvalence.workmanager.service.generated.models.PageEmployerProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.PageProfileInvitationResponse;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationResponse;
import io.nuvalence.workmanager.service.mapper.EmployerMapper;
import io.nuvalence.workmanager.service.mapper.EmployerUserLinkMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.mapper.ProfileInvitationMapper;
import io.nuvalence.workmanager.service.models.EmployerFilters;
import io.nuvalence.workmanager.service.models.EmployerUserLinkFilters;
import io.nuvalence.workmanager.service.models.ProfileInvitationFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.service.AuditEventService;
import io.nuvalence.workmanager.service.service.EmployerService;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.ProfileInvitationService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployerProfilesApiDelegateImpl implements EmployerProfilesApiDelegate {
    private final AuthorizationHandler authorizationHandler;
    private final EmployerService employerService;
    private final EmployerMapper employerMapper;
    private final EmployerUserLinkMapper employerUserLinkMapper;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final RequestContextTimestamp requestContextTimestamp;
    private final AuditEventService employerAuditEventService;
    private final EmployerUserLinkService employerUserLinkService;
    private final UserManagementService userManagementService;
    private final ProfileInvitationService profileInvitationService;
    private final ProfileInvitationMapper profileInvitationMapper;

    private static final String EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE =
            "employer_profile_invitation";
    private static final String EMPLOYER_PROFILE_NOT_FOUND_MSG = "Employer profile not found";
    private static final String CREATION_AUDIT_EVENT_ERR_MSG =
            "An error has occurred when recording a creation audit event for an";

    private static final String INVITATION_NOT_FOUND_MSG = "Invitation not found";

    @Override
    public ResponseEntity<EmployerProfileResponseModel> getEmployerProfile(UUID profileId) {
        final EmployerProfileResponseModel employerProfileResponseModel =
                employerService
                        .getEmployerById(profileId)
                        .filter(
                                employerInstance ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", employerInstance))
                        .map(employerMapper::employerToResponseModel)
                        .orElseThrow(() -> new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG));

        return ResponseEntity.status(200).body(employerProfileResponseModel);
    }

    @Override
    public ResponseEntity<PageEmployerProfileResponseModel> getEmployerProfiles(
            String fein,
            String name,
            String type,
            String industry,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", Employer.class)) {
            throw new ForbiddenException();
        }

        Page<EmployerProfileResponseModel> results =
                employerService
                        .getEmployersByFilters(
                                new EmployerFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        fein,
                                        name,
                                        type,
                                        industry))
                        .map(employerMapper::employerToResponseModel);

        PageEmployerProfileResponseModel response = new PageEmployerProfileResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<PageEmployerProfileLink> getEmployerProfileLinks(
            UUID profileId,
            UUID userId,
            String name,
            String email,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        if (!authorizationHandler.isAllowed("view", EmployerUserLink.class)) {
            throw new ForbiddenException();
        }

        List<UUID> userIds = null;
        if ((!StringUtils.isBlank(name) || !StringUtils.isBlank(email)) && userId == null) {
            List<UserDTO> users = userManagementService.getUsers(name, email);
            userIds = users.stream().map(UserDTO::getId).toList();
        } else if (userId != null) {
            userIds = new ArrayList<>(List.of(userId));
        }

        Page<EmployerProfileLinkResponse> results =
                employerUserLinkService
                        .getEmployerUserLinks(
                                new EmployerUserLinkFilters(
                                        profileId,
                                        userIds,
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize))
                        .map(employerUserLinkMapper::employerUserLinkToResponseModel);

        PageEmployerProfileLink response = new PageEmployerProfileLink();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<EmployerProfileResponseModel> postEmployerProfile(
            EmployerProfileCreateModel employerProfileCreateModel) {
        if (!authorizationHandler.isAllowed("create", Employer.class)) {
            throw new ForbiddenException();
        }

        Employer employer =
                employerService.saveEmployer(
                        employerMapper.createModelToEmployer(employerProfileCreateModel));

        EmployerProfileResponseModel employerProfileResponseModel =
                employerMapper.employerToResponseModel(employer);

        postAuditEventForEmployerProfileCreated(employer);

        return ResponseEntity.status(200).body(employerProfileResponseModel);
    }

    @Override
    public ResponseEntity<EmployerProfileResponseModel> updateEmployerProfile(
            UUID profileId, EmployerProfileUpdateModel employerProfileUpdateModel) {
        if (!authorizationHandler.isAllowed("update", Employer.class)) {
            throw new ForbiddenException();
        }

        Optional<Employer> optionalEmployer = employerService.getEmployerById(profileId);
        if (optionalEmployer.isEmpty()) {
            throw new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG);
        }
        Employer existingEmployer = optionalEmployer.get();

        try {
            final Employer savedEmployer =
                    AuditableAction.builder(Employer.class)
                            .auditHandler(
                                    new EmployerProfileDataChangedAuditHandler(
                                            employerAuditEventService))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    employer -> {
                                        Employer employerToBeSaved =
                                                employerMapper.updateModelToEmployer(
                                                        employerProfileUpdateModel);
                                        employerToBeSaved.setId(profileId);
                                        employerToBeSaved.setCreatedBy(
                                                existingEmployer.getCreatedBy());
                                        employerToBeSaved.setCreatedTimestamp(
                                                existingEmployer.getCreatedTimestamp());

                                        employerService.saveEmployer(employerToBeSaved);

                                        return employerService
                                                .getEmployerById(profileId)
                                                .orElseThrow(
                                                        () ->
                                                                new UnexpectedException(
                                                                        EMPLOYER_PROFILE_NOT_FOUND_MSG
                                                                                + " after saving"));
                                    })
                            .build()
                            .execute(existingEmployer);

            return ResponseEntity.status(200)
                    .body(employerMapper.employerToResponseModel(savedEmployer));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private void postAuditEventForEmployerProfileCreated(Employer profile) {
        try {
            employerService.postAuditEventForEmployerCreated(profile);
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

    @Override
    public ResponseEntity<EmployerProfileLinkResponse> updateEmployerProfileLink(
            UUID profileId,
            UUID userId,
            EmployerProfileLinkRequestModel employerProfileLinkRequest) {

        validateProfileUserPermissions(
                profileId,
                "update",
                Employer.class,
                "User is not authorized to edit users in this profile");

        Optional<Employer> optionalEmployer = employerService.getEmployerById(profileId);
        if (optionalEmployer.isEmpty()) {
            throw new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG);
        }

        EmployerUserLink savedEmployerUserLink =
                saveEmployerUserLink(
                        profileId,
                        userId,
                        optionalEmployer.get(),
                        ProfileAccessLevel.fromValue(
                                employerProfileLinkRequest.getProfileAccessLevel()));
        return ResponseEntity.status(200)
                .body(
                        employerUserLinkMapper.employerUserLinkToResponseModel(
                                savedEmployerUserLink));
    }

    private EmployerUserLink saveEmployerUserLink(
            UUID profileId, UUID userId, Employer employer, ProfileAccessLevel profileAccessLevel) {
        Optional<EmployerUserLink> employerUserLinkOptional =
                employerUserLinkService.getEmployerUserLink(profileId, userId);

        EmployerUserLink employerUserLinkToBeSaved = new EmployerUserLink();

        if (employerUserLinkOptional.isPresent()) {
            employerUserLinkToBeSaved.setId(employerUserLinkOptional.get().getId());
            employerUserLinkToBeSaved.setCreatedBy(employerUserLinkOptional.get().getCreatedBy());
            employerUserLinkToBeSaved.setCreatedTimestamp(
                    employerUserLinkOptional.get().getCreatedTimestamp());
        }

        employerUserLinkToBeSaved.setProfile(employer);
        employerUserLinkToBeSaved.setUserId(userId);
        employerUserLinkToBeSaved.setProfileAccessLevel(profileAccessLevel);

        try {
            if (employerUserLinkOptional.isPresent()) {
                AuditableAction.builder(EmployerUserLink.class)
                        .auditHandler(
                                new EmployerProfileUserAccessLevelChangedAuditHandler(
                                        employerAuditEventService))
                        .requestContextTimestamp(requestContextTimestamp)
                        .action(
                                employerLink ->
                                        employerUserLinkService.saveEmployerUserLink(
                                                employerUserLinkToBeSaved))
                        .build()
                        .execute(employerUserLinkOptional.get());
            } else {
                employerUserLinkService.saveEmployerUserLink(employerUserLinkToBeSaved);
            }

        } catch (Exception e) {
            throw new UnexpectedException(e);
        }

        EmployerUserLink savedEmployerUserLink =
                employerUserLinkService
                        .getEmployerUserLink(profileId, userId)
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "Employer User Link not found after saving"));

        if (employerUserLinkOptional.isEmpty()) {
            postAuditEventForEmployerProfileUserAdded(savedEmployerUserLink);
        }
        return savedEmployerUserLink;
    }

    @Override
    public ResponseEntity<Void> deleteEmployerProfileLink(UUID profileId, UUID userId) {

        validateProfileUserPermissions(
                profileId,
                "delete",
                EmployerUserLink.class,
                "User is not authorized to remove users from this profile");

        EmployerUserLink employerUserLink =
                employerUserLinkService
                        .getEmployerUserLink(profileId, userId)
                        .orElseThrow(() -> new NotFoundException("Employer user link not found"));

        try {
            employerUserLinkService.deleteEmployerUserLink(employerUserLink.getId());
            String currentUserId = SecurityContextUtility.getAuthenticatedUserId();
            employerUserLink.setLastUpdatedBy(currentUserId);
            postAuditEventForEmployerProfileUserRemoved(employerUserLink);
        } catch (Exception e) {
            throw new UnexpectedException("Could not delete profile " + e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> postEmployerProfileInvitation(
            UUID profileId, ProfileInvitationRequestModel profileInvitationRequestModel) {

        validateProfileUserPermissions(
                profileId,
                "invite",
                EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE,
                "User is not authorized to invite users to this profile");

        Optional<Employer> employer = employerService.getEmployerById(profileId);

        if (employer.isEmpty()) {
            throw new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG);
        }

        ProfileInvitation profileInvitation =
                profileInvitationMapper.createModelToProfileInvitation(
                        profileId, profileInvitationRequestModel);

        ProfileInvitation savedProfileInvitation =
                profileInvitationService.saveProfileInvitation(
                        ProfileType.EMPLOYER, employer.get().getLegalName(), profileInvitation);

        ProfileInvitationResponse profileInvitationResponse =
                profileInvitationMapper.profileInvitationToResponseModel(savedProfileInvitation);

        postAuditEventForProfileInvitation(
                profileInvitation,
                AuditActivityType.PROFILE_INVITATION_SENT,
                SecurityContextUtility.getAuthenticatedUserId());

        return ResponseEntity.status(200).body(profileInvitationResponse);
    }

    @Override
    public ResponseEntity<PageProfileInvitationResponse> getEmployerProfileInvitations(
            UUID profileId,
            String accessLevel,
            String email,
            Boolean exactEmailMatch,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("read", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE)) {
            throw new ForbiddenException();
        }
        if (UserUtility.getAuthenticatedUserType().equalsIgnoreCase(UserType.PUBLIC.getValue())) {
            final String userId = SecurityContextUtility.getAuthenticatedUserId();
            Optional<EmployerUserLink> profilesLinkOptional =
                    employerUserLinkService.getEmployerUserLink(profileId, UUID.fromString(userId));
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
                                        ProfileType.EMPLOYER.getValue()))
                        .map(profileInvitationMapper::profileInvitationToResponseModel);

        PageProfileInvitationResponse response = new PageProfileInvitationResponse();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<Void> claimEmployerProfileInvitation(UUID invitationId) {
        if (!authorizationHandler.isAllowed("claim", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE)) {
            throw new ForbiddenException();
        }

        final String email = SecurityContextUtility.getAuthenticatedUserEmail();
        // validates that the current logged user is the owner of the invitation
        Optional<ProfileInvitation> profileInvitationOptional =
                profileInvitationService.getProfileInvitationByIdAndEmail(invitationId, email);

        if (profileInvitationOptional.isEmpty()) {
            throw new NotFoundException(INVITATION_NOT_FOUND_MSG);
        }

        ProfileInvitation profileInvitation = profileInvitationOptional.get();

        UUID profileId = profileInvitation.getProfileId();

        Employer employer =
                employerService
                        .getEmployerById(profileId)
                        .orElseThrow(() -> new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG));

        if (profileInvitation.getClaimed() != null && profileInvitation.getClaimed()) {
            throw new ConflictException("You have already claimed an invitation to this profile");
        }

        final String userId = SecurityContextUtility.getAuthenticatedUserId();
        saveEmployerUserLink(
                profileId, UUID.fromString(userId), employer, profileInvitation.getAccessLevel());

        profileInvitation.setClaimed(true);
        profileInvitation.setClaimedTimestamp(OffsetDateTime.now());
        profileInvitationService.saveProfileInvitation(profileInvitation);

        postAuditEventForProfileInvitation(
                profileInvitation, AuditActivityType.PROFILE_INVITATION_CLAIMED, userId);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEmployerProfileInvitation(UUID profileId, UUID invitationId) {

        validateProfileUserPermissions(
                profileId,
                "delete",
                EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE,
                "User is not authorized to delete invites to this profile");

        ProfileInvitation profileInvitation =
                profileInvitationService
                        .getProfileInvitationById(invitationId)
                        .orElseThrow(() -> new NotFoundException(INVITATION_NOT_FOUND_MSG));

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
    public ResponseEntity<ProfileInvitationResponse> getEmployerProfileInvitationById(
            UUID invitationId) {
        if (!authorizationHandler.isAllowed("read", EMPLOYER_PROFILE_INVITATION_CERBOS_TYPE)) {
            throw new ForbiddenException();
        }

        ProfileInvitation invitation =
                profileInvitationService
                        .getInvitationByIdAndType(invitationId, ProfileType.EMPLOYER)
                        .orElseThrow(() -> new NotFoundException(INVITATION_NOT_FOUND_MSG));

        if (UserUtility.getAuthenticatedUserType().equalsIgnoreCase(UserType.PUBLIC.getValue())) {
            final String userId = SecurityContextUtility.getAuthenticatedUserId();
            Optional<EmployerUserLink> profilesLinkOptional =
                    employerUserLinkService.getEmployerUserLink(
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

    private void postAuditEventForEmployerProfileUserAdded(EmployerUserLink employerUserLink) {
        try {
            employerService.postAuditEventForEmployerlProfileUserAdded(employerUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " employer  profile user with owner user id %s for user with"
                                    + " id %s.",
                            employerUserLink.getProfile().getCreatedBy(),
                            employerUserLink.getUserId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForEmployerProfileUserRemoved(EmployerUserLink employerUserLink) {
        try {
            employerService.postAuditEventForEmployerProfileUserRemoved(employerUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " employer  profile user with owner user id %s for user with"
                                    + " id %s.",
                            employerUserLink.getProfile().getCreatedBy(),
                            employerUserLink.getUserId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForProfileInvitation(
            ProfileInvitation profileInvitation,
            AuditActivityType auditActivityType,
            String userId) {
        try {
            employerService.postAuditEventForEmployerProfileInvites(
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
            UUID profileId, String action, Object type, String exceptionMessage) {

        if (!isCerbosAllowed(action, type)) {
            String authenticatedUserType = UserUtility.getAuthenticatedUserType();

            if (!authenticatedUserType.equalsIgnoreCase(UserType.PUBLIC.getValue())) {
                throw new ForbiddenException(exceptionMessage);
            }

            final String userId = SecurityContextUtility.getAuthenticatedUserId();

            Optional<EmployerUserLink> profilesLinkOptional =
                    employerUserLinkService.getEmployerUserLink(profileId, UUID.fromString(userId));

            if (profilesLinkOptional.isEmpty()
                    || !profilesLinkOptional
                            .get()
                            .getProfileAccessLevel()
                            .equals(ProfileAccessLevel.ADMIN)) {
                throw new ForbiddenException(exceptionMessage);
            }
        }
    }

    private boolean isCerbosAllowed(String action, Object type) {
        boolean isAllowed = false;

        if (type instanceof String) {
            isAllowed = authorizationHandler.isAllowed(action, (String) type);
        } else if (type instanceof Class<?>) {
            isAllowed = authorizationHandler.isAllowed(action, (Class<?>) type);
        }
        return isAllowed;
    }
}
