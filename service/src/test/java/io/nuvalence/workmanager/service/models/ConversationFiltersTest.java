package io.nuvalence.workmanager.service.models;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class ConversationFiltersTest {

    @InjectMocks private ConversationFilters conversationFilters;

    @Mock private Root<Conversation> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder criteriaBuilder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetConversationByFiltersWithEntityReference() {
        conversationFilters =
                ConversationFilters.builder()
                        .referenceType("TRANSACTION")
                        .referenceId(UUID.randomUUID())
                        .build();

        Path<Object> typePath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);

        // Mocking the path for entityReference type
        when(root.get("entityReference")).thenReturn(typePath);
        when(root.get("entityReference").get("entityType")).thenReturn(typePath);
        when(criteriaBuilder.equal(typePath, EntityType.TRANSACTION))
                .thenReturn(mock(Predicate.class));

        // Mocking the path for entityReference id
        when(root.get("entityReference")).thenReturn(idPath);
        when(root.get("entityReference").get("entityId")).thenReturn(idPath);
        when(criteriaBuilder.equal(idPath, conversationFilters.getReferenceId()))
                .thenReturn(mock(Predicate.class));

        // Mocking the final predicate
        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<Conversation> specification = conversationFilters.getConversationByFilters();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testConversationByFiltersWithNullValues() {
        // Given
        conversationFilters = new ConversationFilters(null, null, null, null, null, null);

        // Execute
        Specification<Conversation> specification = conversationFilters.getConversationByFilters();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
        verify(criteriaBuilder, atMost(2)).and();
    }
}
