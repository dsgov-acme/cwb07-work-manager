package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndividualUserLinkRepository
        extends JpaRepository<IndividualUserLink, UUID>,
                JpaSpecificationExecutor<IndividualUserLink> {

    Optional<IndividualUserLink> findByProfileIdAndUserId(UUID profileId, UUID userId);

    List<IndividualUserLink> findByUserId(UUID userId);
}
