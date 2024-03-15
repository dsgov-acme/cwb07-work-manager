package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.models.EmployerUserLinkFilters;
import io.nuvalence.workmanager.service.repository.EmployerUserLinkRepository;
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
public class EmployerUserLinkService {
    private final EmployerUserLinkRepository repository;

    public EmployerUserLink saveEmployerUserLink(EmployerUserLink employerUserLink) {
        return repository.save(employerUserLink);
    }

    public Optional<EmployerUserLink> getEmployerUserLink(UUID profileId, UUID userId) {
        return repository.findByProfileIdAndUserId(profileId, userId);
    }

    public List<EmployerUserLink> getEmployerByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    public void deleteEmployerUserLink(UUID id) {
        repository.deleteById(id);
    }

    public Page<EmployerUserLink> getEmployerUserLinks(final EmployerUserLinkFilters filters) {
        return repository.findAll(
                filters.getEmployerUserLinkSpecification(), filters.getPageRequest());
    }

    public List<EmployerUserLink> getEmployerLinksByUserId(String userId) {
        return repository.findByUserId(UUID.fromString(userId));
    }
}
