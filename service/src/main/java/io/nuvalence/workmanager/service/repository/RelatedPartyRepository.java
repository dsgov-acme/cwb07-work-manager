package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.profile.RelatedPartyId;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RelatedPartyRepository extends CrudRepository<RelatedParty, RelatedPartyId> {
    Optional<RelatedParty> findByProfileIdAndType(UUID profileId, ProfileType type);
}
