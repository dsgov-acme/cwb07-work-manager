package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileAccessLevel;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.IndividualUserLinksFilters;
import io.nuvalence.workmanager.service.repository.IndividualUserLinkRepository;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class IndividualUserLinkServiceTest {
    @Mock private IndividualUserLinkRepository repository;
    @Mock private UserManagementService userManagementService;

    @Mock private IndividualService individualService;

    @InjectMocks private IndividualUserLinkService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveIndividualUserLink() {
        IndividualUserLink individualUserLink = new IndividualUserLink();
        when(repository.save(any(IndividualUserLink.class))).thenReturn(individualUserLink);

        IndividualUserLink savedLink = service.saveIndividualUserLink(individualUserLink);

        assertNotNull(savedLink);
        assertEquals(individualUserLink, savedLink);
    }

    @Test
    void testGetIndividualUserLinkByProfileAndUserId() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        IndividualUserLink expectedLink = new IndividualUserLink();

        when(repository.findByProfileIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(expectedLink));

        Optional<IndividualUserLink> actualLink =
                service.getIndividualUserLinkByProfileAndUserId(profileId, userId);

        assertTrue(actualLink.isPresent());
        assertEquals(expectedLink, actualLink.get());
    }

    @Test
    void testGetIndividualUserLinkByProfileAndUserId_NullInputs() {
        Optional<IndividualUserLink> link =
                service.getIndividualUserLinkByProfileAndUserId(null, null);
        assertTrue(link.isEmpty());
    }

    @Test
    void testDeleteIndividualUserLink() {
        IndividualUserLink individualUserLink = new IndividualUserLink();
        service.deleteIndividualUserLink(individualUserLink);
        Mockito.verify(repository).delete(individualUserLink);
    }

    @Test
    void testGetIndividualLinksByFilters() {
        // Given
        IndividualUserLinksFilters filters =
                IndividualUserLinksFilters.builder()
                        .userId(UUID.randomUUID())
                        .email("test@example.com")
                        .sortBy("name")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(10)
                        .name("John Doe")
                        .build();

        // Use the same PageRequest as in the service method
        PageRequest pageRequest =
                PageRequest.of(
                        filters.getPageNumber(),
                        filters.getPageSize(),
                        Sort.by(Sort.Order.asc(filters.getSortBy())) // Use the same sorting order
                        );

        // Mock the behavior of your repository
        when(repository.findAll(any(Specification.class), eq(pageRequest)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        // When
        Page<IndividualUserLink> result = service.getIndividualLinksByFilters(filters);

        assertNotNull(result);
    }

    @Test
    void getIndividualLinksByUserIdTest() {
        UUID userId = UUID.randomUUID();
        IndividualUserLink individualUserLink =
                IndividualUserLink.builder().id(UUID.randomUUID()).build();
        when(repository.findByUserId(userId)).thenReturn(List.of(individualUserLink));

        List<IndividualUserLink> result = service.getIndividualLinksByUserId(userId.toString());
        assertEquals(1, result.size());
        assertEquals(individualUserLink, result.get(0));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void createAdminUserLinkForProfileTest(boolean linkExisted) {
        UUID subjectProfileId = UUID.randomUUID();
        Transaction transaction =
                Transaction.builder()
                        .subjectProfileType(ProfileType.INDIVIDUAL)
                        .subjectProfileId(subjectProfileId)
                        .build();

        Individual individual =
                Individual.builder().id(UUID.randomUUID()).ownerUserId(UUID.randomUUID()).build();
        when(individualService.getIndividualById(subjectProfileId))
                .thenReturn(Optional.of(individual));

        IndividualUserLink individualUserLink =
                IndividualUserLink.builder()
                        .userId(individual.getOwnerUserId())
                        .profile(individual)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .build();
        when(repository.save(individualUserLink)).thenReturn(individualUserLink);
        when(repository.findByProfileIdAndUserId(any(UUID.class), any(UUID.class)))
                .thenReturn(linkExisted ? Optional.of(individualUserLink) : Optional.empty());

        try (MockedStatic<SecurityContextUtility> mock =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            IndividualUserLink result = service.createAdminUserLinkForProfile(transaction);

            assertEquals(individualUserLink, result);
        }
    }

    @Test
    void getIndividualLinksByUserId() {
        UUID userId = UUID.randomUUID();

        List<IndividualUserLink> expected =
                List.of(IndividualUserLink.builder().id(UUID.randomUUID()).build());

        when(repository.findByUserId(userId)).thenReturn(expected);

        List<IndividualUserLink> result = service.getIndividualLinksByUserId(userId.toString());

        assertEquals(expected, result);
    }
}
