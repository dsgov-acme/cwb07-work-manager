package io.nuvalence.workmanager.service.service;

import static io.nuvalence.workmanager.service.domain.profile.ProfileType.INDIVIDUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.repository.RelatedPartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class RelatedPartyServiceTest {
    @Mock private RelatedPartyRepository repository;

    @InjectMocks private RelatedPartyService service;

    @Test
    void saveRelatedPartyTest() {
        RelatedParty relatedParty =
                RelatedParty.builder()
                        .type(INDIVIDUAL)
                        .profileId(UUID.randomUUID())
                        .transactionAdditionalParties(null)
                        .build();
        when(repository.save(relatedParty)).thenReturn(relatedParty);

        RelatedParty result = service.saveRelatedParty(relatedParty);

        assertEquals(relatedParty, result);
    }

    @Test
    void createOrGetRelatedPartyForIndividualTest_Exists() {
        Individual individual = Individual.builder().id(UUID.randomUUID()).build();
        RelatedParty relatedParty = RelatedParty.builder().build();

        when(repository.findByProfileIdAndType(individual.getId(), INDIVIDUAL))
                .thenReturn(Optional.of(relatedParty));

        RelatedParty result = service.createOrGetRelatedPartyForIndividual(individual);

        assertEquals(relatedParty, result);
    }

    @Test
    void createOrGetRelatedPartyForIndividualTest_DoesNotExists() {
        Individual individual = Individual.builder().id(UUID.randomUUID()).build();

        when(repository.findByProfileIdAndType(individual.getId(), INDIVIDUAL))
                .thenReturn(Optional.empty());

        RelatedParty toSave =
                RelatedParty.builder().type(INDIVIDUAL).profileId(individual.getId()).build();
        when(repository.save(any())).thenReturn(toSave);

        RelatedParty result = service.createOrGetRelatedPartyForIndividual(individual);

        assertEquals(INDIVIDUAL, result.getType());
        assertEquals(individual.getId(), result.getProfileId());
    }
}
