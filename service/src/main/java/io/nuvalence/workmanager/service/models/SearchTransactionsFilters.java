package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * The filters to filter the transactions by.
 */
@Getter
@Setter
public class SearchTransactionsFilters extends TransactionFilters {

    /**
     * Builder for TransactionFilters.
     *
     * @param transactionDefinitionKeys The transaction definition keys to filter transactions by
     * @param category                  The transaction category
     * @param startDate                 The start date to filter transactions by
     * @param endDate                   The end date to filter transactions by
     * @param priority                  The priority to filter transactions by
     * @param status                    The status to filter transactions by
     * @param assignedTo                The assigned user to filter transactions by
     * @param assignedToMe              Filter transactions assigned to yourself
     * @param subjectUserId             The subjectUserId to filter transactions by
     * @param externalId                The externalId to filter transactions by
     * @param createdBy                 The createdBy user id to filter transactions by
     * @param subjectProfileId          The subjectProfileId to filter transactions by
     * @param additionalParties         The additionalParties to filter transactions by
     * @param sortBy                    The column to filter transactions by
     * @param sortOrder                 The order to filter transactions by
     * @param pageNumber                The number of the pages to get transactions
     * @param pageSize                  The number of transactions per pageNumber
     */
    @Builder
    public SearchTransactionsFilters(
            List<String> transactionDefinitionKeys,
            String category,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<TransactionPriority> priority,
            List<String> status,
            List<String> assignedTo,
            Boolean assignedToMe,
            String externalId,
            String createdBy,
            String subjectUserId,
            List<UUID> subjectProfileId,
            List<UUID> additionalParties,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.setTransactionDefinitionKeys(transactionDefinitionKeys);
        this.setCategory(category);
        this.setStartDate(startDate);
        this.setEndDate(endDate);
        this.setPriority(priority);
        this.setStatus(status);
        this.setAssignedTo(assignedTo);
        this.setAssignedToMe(assignedToMe);
        this.setSubjectUserId(subjectUserId);
        this.setExternalId(externalId);
        this.setCreatedBy(createdBy);
        this.setSubjectProfileId(subjectProfileId);
        this.setAdditionalParties(additionalParties);
    }

    @Override
    public Specification<Transaction> getTransactionSpecifications() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> orPredicates = new ArrayList<>();
            List<Predicate> andPredicates = new ArrayList<>();
            Join<TransactionDefinition, Transaction> transactionDefinitionJoin =
                    root.join("transactionDefinition");

            addInPredicateWithEmptySupport(
                    andPredicates,
                    root.get("transactionDefinitionKey"),
                    getTransactionDefinitionKeys());

            addLikePredicate(
                    andPredicates,
                    criteriaBuilder,
                    transactionDefinitionJoin.get("category"),
                    getCategory());

            addGreaterThanOrEqualToPredicate(
                    andPredicates, criteriaBuilder, root.get("createdTimestamp"), getStartDate());

            addLessThanOrEqualToPredicate(
                    andPredicates, criteriaBuilder, root.get("createdTimestamp"), getEndDate());

            addInPredicate(andPredicates, root.get("priority"), getPriority());
            addInPredicate(andPredicates, root.get("status"), getStatus());
            addInPredicate(andPredicates, root.get("assignedTo"), getAssignedTo());

            if (getSubjectProfileId() != null
                    && !getSubjectProfileId().isEmpty()
                    && getAdditionalParties() != null
                    && !getAdditionalParties().isEmpty()
                    && this.isOrOperatedRelatedParties()) {
                addRelatedParties(root, orPredicates);
            } else {
                addRelatedParties(root, andPredicates);
            }

            addEqualPredicate(
                    andPredicates, criteriaBuilder, root.get("subjectUserId"), getSubjectUserId());

            addEqualIgnoreCasePredicate(
                    andPredicates, criteriaBuilder, root.get("externalId"), getExternalId());

            Predicate finalPredicate = criteriaBuilder.and(andPredicates.toArray(new Predicate[0]));
            if (orPredicates != null && !orPredicates.isEmpty()) {
                Predicate orPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
                finalPredicate = criteriaBuilder.and(orPredicate, finalPredicate);
            }

            return finalPredicate;
        };
    }

    private void addRelatedParties(Root<Transaction> root, List<Predicate> predicates) {
        addInPredicate(predicates, root.get("subjectProfileId"), getSubjectProfileId());
        if (getAdditionalParties() != null && !getAdditionalParties().isEmpty()) {
            Join<Transaction, RelatedParty> join = root.join("additionalParties", JoinType.LEFT);
            addInPredicate(predicates, join.get("profileId"), getAdditionalParties());
        }
    }
}
