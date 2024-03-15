package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Getter
public class MessageFilters extends BaseFilters {

    private final UUID conversationId;

    @Builder
    public MessageFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            UUID conversationId) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.conversationId = conversationId;
    }

    public Specification<Message> getMessagesByFilters() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addConversationId(predicates, criteriaBuilder, root);

            return predicates.isEmpty()
                    ? null
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addConversationId(
            List<Predicate> predicates, CriteriaBuilder criteriaBuilder, Root<Message> root) {
        if (conversationId != null) {
            Predicate conversationIdPredicate =
                    criteriaBuilder.equal(root.get("conversation").get("id"), conversationId);
            predicates.add(conversationIdPredicate);
        }
    }
}
