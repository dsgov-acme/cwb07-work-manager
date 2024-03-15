package io.nuvalence.workmanager.service.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class SchemaFiltersTest {

    @Mock private CriteriaQuery<SchemaRow> criteriaQuery;
    @Mock private Root<SchemaRow> root;
    @Mock private CriteriaBuilder criteriaBuilder;
    @Mock private Path<String> pathExpression;
    @Mock private Expression<String> lowerExpression;
    @Mock private CriteriaQuery<?> query;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSchemaSpecWithValue() {

        SchemaFilters filters = SchemaFilters.builder().name("TESTNAME").build();

        var firstPredicate = mock(Predicate.class);
        var finalPredicate = mock(Predicate.class);

        when(root.<String>get("name")).thenReturn(pathExpression);
        when(criteriaBuilder.lower(pathExpression)).thenReturn(lowerExpression);
        when(criteriaBuilder.like(lowerExpression, "%" + "TESTNAME".toLowerCase(Locale.ROOT) + "%"))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.or(any())).thenReturn(finalPredicate);

        ArgumentCaptor<Predicate> captor = ArgumentCaptor.forClass(Predicate.class);

        // Execute
        Specification<SchemaRow> specification = filters.getSchemaSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // verify predicate returned is result of the anding operation
        assertEquals(finalPredicate, result);

        // verify the like expression is evaluated as case insensitive
        verify(criteriaBuilder).like(lowerExpression, "%testname%");

        // verify the right predicate is passed after proper string 'like' operations
        verify(criteriaBuilder).or(captor.capture());
        var value = captor.getValue();
        assertEquals(firstPredicate, value);
    }

    @Test
    void testSchemaSpecWithBlankValue() {
        // blank string test
        SchemaFilters filters = SchemaFilters.builder().name("").build();
        commonBlankSearchTest(filters);

        // null test
        filters = SchemaFilters.builder().name(null).build();
        commonBlankSearchTest(filters);
    }

    private void commonBlankSearchTest(SchemaFilters filters) {

        // Execute
        Specification<SchemaRow> specification = filters.getSchemaSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // verify final predicate is still returned
        assertNull(result);

        // verify the like expression is not included
        verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));

        // verify criteriabuilder is 'anding' to nothing. Which means no predicate/filter is
        // provided.
        verify(criteriaBuilder, atMost(2)).and();
    }
}
