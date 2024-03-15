package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

/**
 * Record Definition filters.
 */
public class RecordDefinitionFilters extends BaseFilters {

    private final String name;

    /**
     * Constructs a new RecordFilters object using the Builder pattern. This constructor initializes the RecordFilters
     * with the specified parameters and sets the values for sorting, pagination, and filtering of record data.
     *
     * @param name The name to filter records by.
     * @param sortBy The field by which the result set should be sorted.
     * @param sortOrder The order in which the result set should be sorted (ascending or descending).
     * @param pageNumber The page number for pagination.
     * @param pageSize The number of items per page for pagination.
     */
    @Builder
    public RecordDefinitionFilters(
            String name, String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.name = name;
    }

    /**
     * Generates a  Specification object for Record Definition lookups.
     *
     * @return Specification object
     */
    public Specification<RecordDefinition> getRecordDefinitionSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + this.name.toLowerCase(Locale.ROOT) + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
