package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.repository.RelatedPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RelatedPartyService {
    private final RelatedPartyRepository relatedPartyRepository;

    public RelatedParty saveRelatedParty(RelatedParty relatedParty) {
        return relatedPartyRepository.save(relatedParty);
    }

    public RelatedParty createOrGetRelatedPartyForIndividual(Individual individual) {
        Optional<RelatedParty> optionalRelatedParty =
                relatedPartyRepository.findByProfileIdAndType(
                        individual.getId(), ProfileType.INDIVIDUAL);

        if (optionalRelatedParty.isPresent()) {
            return optionalRelatedParty.get();
        }

        return saveRelatedParty(
                RelatedParty.builder()
                        .type(ProfileType.INDIVIDUAL)
                        .profileId(individual.getId())
                        .build());
    }

    public Optional<RelatedParty> getRelatedPartyByProfileIdAndType(
            UUID profileID, ProfileType type) {
        return relatedPartyRepository.findByProfileIdAndType(profileID, type);
    }
}
