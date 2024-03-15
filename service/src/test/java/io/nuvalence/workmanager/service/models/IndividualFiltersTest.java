package io.nuvalence.workmanager.service.models;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

class IndividualFiltersTest {

    @Mock private UserManagementService userManagementService;

    @InjectMocks private IndividualFilters individualFilters;

    @Mock private Root<Individual> root;

    @Mock private Path<String> pathExpression;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder criteriaBuilder;

    @Captor private ArgumentCaptor<Predicate> predicateCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetIndividualProfileSpecificationWithssn() {
        individualFilters =
                new IndividualFilters(
                        null, null, null, null, null, "testssn", null, null, userManagementService);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("ssn")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, individualFilters.getSsn()))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.or(any())).thenReturn(finalPredicate);

        // Execute
        Specification<Individual> specification =
                individualFilters.getIndividualProfileSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertEquals(finalPredicate, result);
        verify(criteriaBuilder).or(predicateCaptor.capture());

        var value = predicateCaptor.getValue();
        Assertions.assertEquals(firstPredicate, value);
    }

    @Test
    void testGetIndividualProfileSpecificationWithOwnerUserId() {
        individualFilters =
                new IndividualFilters(
                        null,
                        null,
                        null,
                        null,
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        userManagementService);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("ownerUserId")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, individualFilters.getOwnerUserId()))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.or(any())).thenReturn(finalPredicate);

        // Execute
        Specification<Individual> specification =
                individualFilters.getIndividualProfileSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertEquals(finalPredicate, result);
        verify(criteriaBuilder).or(predicateCaptor.capture());

        var value = predicateCaptor.getValue();
        Assertions.assertEquals(firstPredicate, value);
    }

    @Test
    void testIndividualSpecificationWithNullValues() {
        // Given
        individualFilters =
                new IndividualFilters(
                        null, null, null, null, null, null, null, null, userManagementService);

        // Execute
        Specification<Individual> specification =
                individualFilters.getIndividualProfileSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
        verify(criteriaBuilder, atMost(2)).and();
    }

    @Test
    void testIndividualSpecificationWithEmail() {
        individualFilters =
                new IndividualFilters(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "email@email.com",
                        null,
                        userManagementService);

        when(userManagementService.getUsers(any(), eq("email@email.com")))
                .thenReturn(List.of(createUserDto()));

        Path<Object> path = mock(Path.class);
        when(root.get("ownerUserId")).thenReturn(path);

        CriteriaBuilder.In<Object> in = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.in(any())).thenReturn(in);

        CriteriaBuilder.In<Object> in2 = mock(CriteriaBuilder.In.class);
        when(in.value(any())).thenReturn(in2);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.or(any())).thenReturn(finalPredicate);
        Specification<Individual> specification =
                individualFilters.getIndividualProfileSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testIndividualSpecificationWithName() {
        individualFilters =
                new IndividualFilters(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "email@email.com",
                        null,
                        userManagementService);

        when(userManagementService.getUsers(any(), eq("email@email.com")))
                .thenReturn(List.of(createUserDto()));

        Path<Object> path = mock(Path.class);
        when(root.get("ownerUserId")).thenReturn(path);

        CriteriaBuilder.In<Object> in = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.in(any())).thenReturn(in);

        CriteriaBuilder.In<Object> in2 = mock(CriteriaBuilder.In.class);
        when(in.value(any())).thenReturn(in2);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.or(any())).thenReturn(finalPredicate);
        Specification<Individual> specification =
                individualFilters.getIndividualProfileSpecification();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
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
