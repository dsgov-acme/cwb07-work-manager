package io.nuvalence.workmanager.service.models;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.record.Record;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class RecordFiltersTest {
    @Mock private Root<Record> root;

    @Mock private CriteriaBuilder criteriaBuilder;

    @Mock private Path<String> pathExpression;

    @Mock private Expression<String> lowerExpression;

    @Mock private CriteriaQuery<?> query;

    @Captor private ArgumentCaptor<Predicate> predicateCaptor;

    @InjectMocks private RecordFilters recordFilters;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecordSpecificationWithRecordDefinitionKey() {
        // Given
        recordFilters = new RecordFilters("KEY", null, null, null, null, null, null);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("recordDefinitionKey")).thenReturn(pathExpression);
        when(criteriaBuilder.lower(pathExpression)).thenReturn(lowerExpression);
        when(criteriaBuilder.like(lowerExpression, "%" + "KEY".toLowerCase(Locale.ROOT) + "%"))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute
        Specification<Record> specification = recordFilters.getRecordSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertEquals(finalPredicate, result);
        verify(criteriaBuilder).like(lowerExpression, "%key%");
        verify(criteriaBuilder).and(predicateCaptor.capture());

        var value = predicateCaptor.getValue();
        Assertions.assertEquals(firstPredicate, value);
    }

    @Test
    void testRecordSpecificationWithExternalId() {
        // Given
        recordFilters = new RecordFilters(null, null, "EXTID", null, null, null, null);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("externalId")).thenReturn(pathExpression);
        when(criteriaBuilder.lower(pathExpression)).thenReturn(lowerExpression);
        when(criteriaBuilder.like(lowerExpression, "%" + "EXTID".toLowerCase(Locale.ROOT) + "%"))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute
        Specification<Record> specification = recordFilters.getRecordSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertEquals(finalPredicate, result);
        verify(criteriaBuilder).like(lowerExpression, "%extid%");
        verify(criteriaBuilder).and(predicateCaptor.capture());

        var value = predicateCaptor.getValue();
        Assertions.assertEquals(firstPredicate, value);
    }

    @Test
    void testRecordSpecificationWithStatus() {
        // Given
        List<String> statusList = Arrays.asList("status1", "status2");
        recordFilters = new RecordFilters(null, statusList, null, null, null, null, null);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("status")).thenReturn(pathExpression);
        when(pathExpression.in(statusList)).thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute
        Specification<Record> specification = recordFilters.getRecordSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertEquals(finalPredicate, result);
        verify(root).get("status");
        verify(pathExpression).in(statusList);
        verify(criteriaBuilder).and(predicateCaptor.capture());

        var value = predicateCaptor.getValue();
        Assertions.assertEquals(firstPredicate, value);
    }

    @Test
    void testRecordSpecificationWithExternalIdAndStatus() {
        // Given
        List<String> statusList = Arrays.asList("status1", "status2");
        recordFilters = new RecordFilters(null, statusList, "EXTID", null, null, null, null);
        Predicate externalIdPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<String>get("status")).thenReturn(pathExpression);
        when(pathExpression.in(statusList)).thenReturn(statusPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Simulate the case where "status" property is not found
        when(root.<String>get("externalId")).thenReturn(pathExpression);
        when(criteriaBuilder.lower(pathExpression)).thenReturn(lowerExpression);
        when(criteriaBuilder.like(lowerExpression, "%" + "EXTID".toLowerCase(Locale.ROOT) + "%"))
                .thenReturn(externalIdPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        when(pathExpression.in(statusList)).thenReturn(statusPredicate);
        when(criteriaBuilder.and(externalIdPredicate, statusPredicate)).thenReturn(finalPredicate);

        // Execute
        Specification<Record> specification = recordFilters.getRecordSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertEquals(finalPredicate, result);
        verify(criteriaBuilder).like(lowerExpression, "%extid%");
        verify(root).get("status"); // make sure it's called
        verify(pathExpression).in(statusList);
    }

    @Test
    void testRecordSpecificationWithNullValues() {
        // Given
        recordFilters = new RecordFilters(null, null, null, null, null, null, null);

        // Execute
        Specification<Record> specification = recordFilters.getRecordSpecification();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
        verify(criteriaBuilder, atMost(2)).and();
    }
}
