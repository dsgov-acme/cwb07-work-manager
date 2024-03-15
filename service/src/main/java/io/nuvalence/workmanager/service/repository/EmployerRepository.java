package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface EmployerRepository
        extends JpaRepository<Employer, UUID>, JpaSpecificationExecutor<Employer> {}
