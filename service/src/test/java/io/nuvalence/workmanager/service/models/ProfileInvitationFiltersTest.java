package io.nuvalence.workmanager.service.models;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class ProfileInvitationFiltersTest {

    @InjectMocks private ProfileInvitationFilters profileInvitationFilters;

    @Mock private Root<ProfileInvitation> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private Path<String> pathExpression;
    @Mock private Expression<String> lowerExpression;

    @Mock private CriteriaBuilder criteriaBuilder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetProfileInvitationsByFiltersWithAccessLevel() {
        profileInvitationFilters = ProfileInvitationFilters.builder().accessLevel("ADMIN").build();

        Predicate firstPredicate = mock(Predicate.class);
        when(root.<String>get("accessLevel")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, profileInvitationFilters.getAccessLevel()))
                .thenReturn(firstPredicate);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<ProfileInvitation> specification =
                profileInvitationFilters.getProfileInvitationSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testGetProfileInvitationsByFiltersWithEmailExactMatch() {
        profileInvitationFilters =
                ProfileInvitationFilters.builder()
                        .email("test@test.com")
                        .exactEmailMatch(true)
                        .build();

        Path<String> path = mock(Path.class);
        Predicate firstPredicate = mock(Predicate.class);
        when(root.<String>get("email")).thenReturn(path);
        when(criteriaBuilder.equal(path, profileInvitationFilters.getEmail()))
                .thenReturn(firstPredicate);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<ProfileInvitation> specification =
                profileInvitationFilters.getProfileInvitationSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testGetProfileInvitationsByFiltersWithEmailPartialMatch() {
        profileInvitationFilters =
                ProfileInvitationFilters.builder()
                        .email("test@test.com")
                        .exactEmailMatch(false)
                        .build();

        Path<String> path = mock(Path.class);
        Predicate firstPredicate = mock(Predicate.class);
        when(root.<String>get("email")).thenReturn(path);
        when(criteriaBuilder.lower(path)).thenReturn(lowerExpression);
        when(criteriaBuilder.like(lowerExpression, "KEY".toLowerCase(Locale.ROOT) + "%"))
                .thenReturn(firstPredicate);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<ProfileInvitation> specification =
                profileInvitationFilters.getProfileInvitationSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testGetProfileInvitationsByFiltersWithType() {
        profileInvitationFilters =
                ProfileInvitationFilters.builder().type(ProfileType.INDIVIDUAL.getValue()).build();

        Path<String> path = mock(Path.class);
        Predicate firstPredicate = mock(Predicate.class);
        when(root.<String>get("type")).thenReturn(path);
        when(criteriaBuilder.equal(path, profileInvitationFilters.getType()))
                .thenReturn(firstPredicate);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<ProfileInvitation> specification =
                profileInvitationFilters.getProfileInvitationSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testGetProfileInvitationsByFiltersWithProfileId() {
        profileInvitationFilters =
                ProfileInvitationFilters.builder().profileId(UUID.randomUUID()).build();

        Path<UUID> path = mock(Path.class);
        Predicate firstPredicate = mock(Predicate.class);
        when(root.<UUID>get("profileId")).thenReturn(path);
        when(criteriaBuilder.equal(path, profileInvitationFilters.getProfileId()))
                .thenReturn(firstPredicate);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<ProfileInvitation> specification =
                profileInvitationFilters.getProfileInvitationSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testConversationByFiltersWithNullValues() {
        // Given
        profileInvitationFilters =
                new ProfileInvitationFilters(null, null, null, null, null, null, null, null, null);

        // Execute
        Specification<ProfileInvitation> specification =
                profileInvitationFilters.getProfileInvitationSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
        verify(criteriaBuilder, atMost(2)).and();
    }
}
