package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.record.Record;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

/**
 * The filters to filter the records by.
 */
@Getter
@Setter
public class RecordFilters extends BaseFilters {
    private String recordDefinitionKey;
    private List<String> status;
    private String externalId;

    /**
     * Constructor.
     *
     * @param recordDefinitionKey the record definition key
     * @param status the status
     * @param externalId the external id
     * @param sortBy the sort by
     * @param sortOrder the sort order
     * @param pageNumber the page number
     * @param pageSize the page size
     */
    public RecordFilters(
            String recordDefinitionKey,
            List<String> status,
            String externalId,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.recordDefinitionKey = recordDefinitionKey;
        this.status = status;
        this.externalId = externalId;
    }

    /**
     * Gets the record specification.
     *
     * @return the record specification
     */
    public Specification<Record> getRecordSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.recordDefinitionKey)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("recordDefinitionKey")),
                                "%" + this.recordDefinitionKey.toLowerCase(Locale.ROOT) + "%"));
            }

            if (StringUtils.isNotBlank(this.externalId)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("externalId")),
                                "%" + this.externalId.toLowerCase(Locale.ROOT) + "%"));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(root.get("status").in(status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
