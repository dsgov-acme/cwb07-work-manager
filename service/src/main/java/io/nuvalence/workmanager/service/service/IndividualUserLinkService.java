package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.IndividualUserLinksFilters;
import io.nuvalence.workmanager.service.repository.IndividualUserLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IndividualUserLinkService {
    private final IndividualService individualService;
    private final IndividualUserLinkRepository repository;

    public IndividualUserLink saveIndividualUserLink(final IndividualUserLink individualUserLink) {
        return repository.save(individualUserLink);
    }

    public Optional<IndividualUserLink> getIndividualUserLinkByProfileAndUserId(
            UUID profileId, UUID userId) {
        if (profileId == null || userId == null) {
            return Optional.empty();
        }

        return repository.findByProfileIdAndUserId(profileId, userId);
    }

    public void deleteIndividualUserLink(IndividualUserLink individualUserLink) {
        repository.delete(individualUserLink);
    }

    public Page<IndividualUserLink> getIndividualLinksByFilters(
            final IndividualUserLinksFilters filters) {
        return repository.findAll(
                filters.getIndividualLinksSpecification(), filters.getPageRequest());
    }

    public List<IndividualUserLink> getIndividualLinksByUserId(String userId) {
        return repository.findByUserId(UUID.fromString(userId));
    }

    public IndividualUserLink createAdminUserLinkForProfile(Transaction transaction) {
        if (!transaction.getSubjectProfileType().equals(ProfileType.INDIVIDUAL)) {
            return null;
        }
        UUID currentUserId = UUID.fromString(SecurityContextUtility.getAuthenticatedUserId());
        Optional<IndividualUserLink> individualUserLinkOptional =
                getIndividualUserLinkByProfileAndUserId(
                        transaction.getSubjectProfileId(), currentUserId);
        if (individualUserLinkOptional.isPresent()) {
            return individualUserLinkOptional.get();
        }

        Optional<Individual> optionalIndividual =
                individualService.getIndividualById(transaction.getSubjectProfileId());
        if (optionalIndividual.isEmpty()) {
            return null;
        }

        Individual profile = optionalIndividual.get();
        IndividualUserLink individualUserLink =
                IndividualUserLink.builder()
                        .userId(profile.getOwnerUserId())
                        .profile(profile)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .build();

        return saveIndividualUserLink(individualUserLink);
    }
}
