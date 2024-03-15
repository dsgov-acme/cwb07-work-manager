package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployerUserLinkRepository
        extends JpaRepository<EmployerUserLink, UUID>, JpaSpecificationExecutor<EmployerUserLink> {

    Optional<EmployerUserLink> findByProfileIdAndUserId(UUID profileId, UUID userId);

    List<EmployerUserLink> findByUserId(UUID userId);
}
