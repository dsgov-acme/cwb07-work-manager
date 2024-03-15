package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.profile.Profile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CommonProfileService {

    private final IndividualService individualService;
    private final EmployerService employerService;

    public Optional<Profile> getProfileById(UUID id) {
        return individualService
                .getIndividualById(id)
                .map(i -> (Profile) i)
                .or(() -> employerService.getEmployerById(id).map(e -> (Profile) e));
    }
}
