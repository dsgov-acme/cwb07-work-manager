package io.nuvalence.workmanager.service.models;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import io.nuvalence.workmanager.service.utils.UserUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class EmployerUserLinksFiltersTest {

    @InjectMocks private EmployerUserLinkFilters filters;

    @Mock private Root<EmployerUserLink> root;

    @Mock private Path<String> pathExpression;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder criteriaBuilder;

    @Captor private ArgumentCaptor<Predicate> predicateCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIndividualSpecificationWitProfileId() {
        filters = new EmployerUserLinkFilters(UUID.randomUUID(), null, null, null, null, null);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        Path<Object> path = mock(Path.class);
        when(root.get("profile")).thenReturn(path);
        when(root.<String>get("id")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, filters.getProfileId().toString()))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            Specification<EmployerUserLink> specification =
                    filters.getEmployerUserLinkSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertNotNull(specification);
            Assertions.assertNotNull(result);

            Assertions.assertEquals(finalPredicate, result);
        }
    }

    @Test
    void testIndividualSpecificationWithListOfIds() {
        filters =
                new EmployerUserLinkFilters(
                        null, List.of(UUID.randomUUID()), null, null, null, null);
        Predicate finalPredicate = mock(Predicate.class);

        Path<Object> path = mock(Path.class);
        when(root.get("userId")).thenReturn(path);

        CriteriaBuilder.In<Object> in = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.in(any())).thenReturn(in);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            Specification<EmployerUserLink> specification =
                    filters.getEmployerUserLinkSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertNotNull(specification);
            Assertions.assertNotNull(result);

            Assertions.assertEquals(finalPredicate, result);
        }
    }

    @Test
    void testIndividualSpecificationWithUserType() {
        filters = new EmployerUserLinkFilters(null, null, null, null, null, null);

        Predicate accessLevelPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("profileAccessLevel")).thenReturn(pathExpression);
        when(criteriaBuilder.notEqual(pathExpression, "public")).thenReturn(accessLevelPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            Specification<EmployerUserLink> specification =
                    filters.getEmployerUserLinkSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertEquals(finalPredicate, result);
            verify(root).get("profileAccessLevel"); // make sure it's called
        }
    }

    @Test
    void testIndividualSpecificationWithNullValues() {
        // Given
        filters = new EmployerUserLinkFilters(null, null, null, null, null, null);
        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            Specification<EmployerUserLink> specification =
                    filters.getEmployerUserLinkSpecification();
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            Assertions.assertNull(result);
            verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
            verify(criteriaBuilder, atMost(2)).and();
        }
    }

    private UserDTO createUserDto() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(UUID.randomUUID());
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setEmail("TestEmail");
        return userDTO;
    }
}
