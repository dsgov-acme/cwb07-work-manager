package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * The filters to filter the transactions by.
 */
@Getter
@Setter
public abstract class TransactionFilters extends BaseFilters {
    private List<String> transactionDefinitionKeys;
    private String category;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private List<TransactionPriority> priority;
    private List<String> status;
    private List<String> assignedTo;
    private Boolean assignedToMe;
    private String subjectUserId;
    private String externalId;
    private String createdBy;
    private Boolean isCompleted;
    private List<UUID> subjectProfileId;
    private List<UUID> additionalParties;
    private boolean isOrOperatedRelatedParties;

    protected TransactionFilters(
            String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
    }

    public abstract Specification<Transaction> getTransactionSpecifications();

    protected <T> void addInPredicateWithEmptySupport(
            List<Predicate> predicates,
            Expression<T> attributeExpression,
            List<T> attributeValues) {
        if (attributeValues != null) {
            predicates.add(attributeExpression.in(attributeValues));
        }
    }

    protected <T> void addInPredicate(
            List<Predicate> predicates,
            Expression<T> attributeExpression,
            List<T> attributeValues) {
        if (attributeValues != null && !attributeValues.isEmpty()) {
            predicates.add(attributeExpression.in(attributeValues));
        }
    }

    protected void addEqualPredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> attributeExpression,
            Object attributeValue) {
        if (attributeValue != null) {
            predicates.add(criteriaBuilder.equal(attributeExpression, attributeValue));
        }
    }

    protected void addLikePredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> attributeExpression,
            String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            predicates.add(criteriaBuilder.like(attributeExpression, attributeValue + "%"));
        }
    }

    protected void addGreaterThanOrEqualToPredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<OffsetDateTime> attributeExpression,
            OffsetDateTime attributeValue) {
        if (attributeValue != null) {
            predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(attributeExpression, attributeValue));
        }
    }

    protected void addLessThanOrEqualToPredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<OffsetDateTime> attributeExpression,
            OffsetDateTime attributeValue) {
        if (attributeValue != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(attributeExpression, attributeValue));
        }
    }

    protected void addEqualIgnoreCasePredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> attributeExpression,
            String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            predicates.add(
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(attributeExpression),
                            attributeValue.toLowerCase(Locale.ENGLISH)));
        }
    }
}
