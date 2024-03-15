package io.nuvalence.workmanager.service.models;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class MessageFiltersTest {

    @Mock private Root<Message> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder criteriaBuilder;

    @InjectMocks private MessageFilters messageFilters;

    @Test
    void testGetConversationByFiltersWithEntityReference() {
        messageFilters = messageFilters.builder().conversationId(UUID.randomUUID()).build();

        Path<Object> idPath = mock(Path.class);

        // Mocking the path for conversation id
        when(root.get("conversation")).thenReturn(idPath);
        when(root.get("conversation").get("id")).thenReturn(idPath);
        when(criteriaBuilder.equal(idPath, messageFilters.getConversationId()))
                .thenReturn(mock(Predicate.class));

        // Mocking the final predicate
        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);
        Specification<Message> specification = messageFilters.getMessagesByFilters();

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        Assertions.assertNotNull(specification);
        Assertions.assertNotNull(result);

        Assertions.assertEquals(finalPredicate, result);
    }

    @Test
    void testConversationByFiltersWithNullValues() {
        // Given
        messageFilters = new MessageFilters(null, null, null, null, null);

        // Execute
        Specification<Message> specification = messageFilters.getMessagesByFilters();
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Verify
        Assertions.assertNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
        verify(criteriaBuilder, atMost(2)).and();
    }
}
