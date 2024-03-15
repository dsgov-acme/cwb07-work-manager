package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.Note;
import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/**
 * Filters to get transaction notes by.
 */
@Getter
@Setter
public class TransactionNoteFilters extends BaseFilters {
    protected OffsetDateTime startDate;
    protected OffsetDateTime endDate;
    protected String type;
    protected String createdBy;
    protected UUID transactionId;
    protected Boolean includeDeleted;

    @Builder
    TransactionNoteFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            String type,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            UUID transactionId,
            Boolean includeDeleted) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.type = type;
        this.setStartDate(startDate);
        this.setEndDate(endDate);
        this.setTransactionId(transactionId);
        this.setIncludeDeleted(includeDeleted);
    }

    /**
     * Creates a specification to filter transaction notes.
     *
     * @return A given specification for querying the db.
     */
    public Specification<TransactionNote> getTransactionSpecifications() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<NoteType, Note> noteTypeNoteJoin = root.join("type");

            if (getStartDate() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTimestamp"), getStartDate()));
            }

            if (getEndDate() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTimestamp"), getEndDate()));
            }

            if (getCreatedBy() != null && !getCreatedBy().isEmpty()) {
                predicates.add(root.get("createdBy").in(getCreatedBy()));
            }

            if (getTransactionId() != null) {
                predicates.add(root.get("transactionId").in(getTransactionId()));
            }

            if (StringUtils.isNotBlank(getType())) {
                predicates.add(criteriaBuilder.equal(noteTypeNoteJoin.get("name"), getType()));
            }

            if (getIncludeDeleted() == null || !getIncludeDeleted()) {
                predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
