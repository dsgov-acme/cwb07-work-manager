package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface IndividualRepository
        extends JpaRepository<Individual, UUID>, JpaSpecificationExecutor<Individual> {
    List<Individual> findByOwnerUserId(UUID ownerUserId);
}
