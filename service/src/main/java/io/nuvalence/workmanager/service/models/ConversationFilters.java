package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Filters for individual profiles.
 */
@Getter
public class ConversationFilters extends BaseFilters {

    private final String referenceType;
    private final UUID referenceId;

    @SuppressWarnings("java:S107")
    @Builder
    public ConversationFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            UUID referenceId,
            String referenceType) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public Specification<Conversation> getConversationByFilters() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addReferenceTypeAndIdPredicate(predicates, criteriaBuilder, root);

            return predicates.isEmpty()
                    ? null
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addReferenceTypeAndIdPredicate(
            List<Predicate> predicates, CriteriaBuilder criteriaBuilder, Root<Conversation> root) {
        if (StringUtils.isNotBlank(referenceType) && referenceId != null) {
            Predicate entityReferencePredicate =
                    criteriaBuilder.and(
                            criteriaBuilder.equal(
                                    root.get("entityReference").get("type"),
                                    EntityType.fromValue(referenceType)),
                            criteriaBuilder.equal(
                                    root.get("entityReference").get("entityId"), referenceId));
            predicates.add(entityReferencePredicate);
        }
    }
}
