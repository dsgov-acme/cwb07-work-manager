package io.nuvalence.workmanager.service.config;

import io.nuvalence.auth.access.cerbos.CerbosPrincipalAttributesProvider;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.models.AccessProfileDto;
import io.nuvalence.workmanager.service.service.EmployerUserLinkService;
import io.nuvalence.workmanager.service.service.IndividualUserLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CerbosPrincipalAttributeProviderImpl implements CerbosPrincipalAttributesProvider {

    private final IndividualUserLinkService individualUserLinkService;
    private final EmployerUserLinkService employerUserLinkService;

    @Override
    public Map<String, Object> getAttributes(Authentication principal) {
        String id = SecurityContextUtility.getAuthenticatedUserId();

        Map<String, Object> attributes = new HashMap<>();
        if (id != null) {
            List<AccessProfileDto> accessProfiles = new ArrayList<>();
            accessProfiles.addAll(
                    createAccessProfilesFromIndividualLinks(
                            individualUserLinkService.getIndividualLinksByUserId(id)));
            accessProfiles.addAll(
                    createAccessProfilesFromEmployerLinks(
                            employerUserLinkService.getEmployerLinksByUserId(id)));

            attributes.put("accessProfiles", accessProfiles);
        }

        return attributes;
    }

    private List<AccessProfileDto> createAccessProfilesFromIndividualLinks(
            List<IndividualUserLink> links) {
        return links.stream()
                .map(
                        link ->
                                AccessProfileDto.builder()
                                        .id(link.getProfile().getId())
                                        .type(ProfileType.INDIVIDUAL)
                                        .level(link.getAccessLevel())
                                        .build())
                .toList();
    }

    private List<AccessProfileDto> createAccessProfilesFromEmployerLinks(
            List<EmployerUserLink> links) {
        return links.stream()
                .map(
                        link ->
                                AccessProfileDto.builder()
                                        .id(link.getProfile().getId())
                                        .type(ProfileType.EMPLOYER)
                                        .level(link.getProfileAccessLevel())
                                        .build())
                .toList();
    }
}
