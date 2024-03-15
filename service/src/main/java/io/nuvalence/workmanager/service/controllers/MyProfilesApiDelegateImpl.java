package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.generated.controllers.MyProfilesApiDelegate;
import io.nuvalence.workmanager.service.generated.models.GeneralProfileModel;
import io.nuvalence.workmanager.service.mapper.EmployerUserLinkMapper;
import io.nuvalence.workmanager.service.mapper.IndividualUserLinkMapper;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyProfilesApiDelegateImpl implements MyProfilesApiDelegate {
    private final IndividualUserLinkService individualUserLinkService;
    private final EmployerUserLinkService employerUserLinkService;
    private final IndividualUserLinkMapper individualUserLinkMapper;
    private final EmployerUserLinkMapper employerUserLinkMapper;
    private final UserManagementService userManagementService;

    @Override
    public ResponseEntity<List<GeneralProfileModel>> getProfilesForAuthenticatedUser(String type) {
        String currentUserId = SecurityContextUtility.getAuthenticatedUserId();
        List<GeneralProfileModel> profiles = new ArrayList<>();

        if (type != null) {
            type = type.toUpperCase(Locale.ROOT);
            if (ProfileType.INDIVIDUAL.getValue().equals(type)) {
                profiles = getIndividualProfiles(currentUserId);
            } else if (ProfileType.EMPLOYER.getValue().equals(type)) {
                profiles = getEmployerProfiles(currentUserId);
            } else {
                String message = String.format("Invalid profile type: %s", type);
                log.error(message);
                throw new UnexpectedException(message);
            }
        } else {
            profiles.addAll(getIndividualProfiles(currentUserId));
            profiles.addAll(getEmployerProfiles(currentUserId));
        }

        return ResponseEntity.ok(profiles);
    }

    private List<GeneralProfileModel> getIndividualProfiles(String userId) {
        String userDisplayName =
                userManagementService
                        .getUserOptional(UUID.fromString(userId))
                        .map(User::getDisplayName)
                        .orElse("");

        return individualUserLinkService.getIndividualLinksByUserId(userId).stream()
                .map(
                        profile ->
                                individualUserLinkMapper.individualUserLinkToGeneralProfileModel(
                                        profile, userDisplayName))
                .toList();
    }

    private List<GeneralProfileModel> getEmployerProfiles(String userId) {
        return employerUserLinkService.getEmployerLinksByUserId(userId).stream()
                .map(employerUserLinkMapper::employerUserLinkToGeneralProfileModel)
                .toList();
    }
}
