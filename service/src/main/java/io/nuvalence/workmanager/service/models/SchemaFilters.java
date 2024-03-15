package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

/** Schema filters. */
@Getter
public class SchemaFilters extends BaseFilters {

    private final String name;
    private final String key;

    /**
     * Constructs a new SchemaFilters object using the Builder pattern. This constructor initializes the SchemaFilters
     * with the specified parameters and sets the values for sorting, pagination, and filtering of schema data.
     *
     * @param name The name to filter schemas by.
     * @param key The schema key to filter schemas by.
     * @param sortBy The field by which the result set should be sorted.
     * @param sortOrder The order in which the result set should be sorted (ascending or descending).
     * @param pageNumber The page number for pagination.
     * @param pageSize The number of items per page for pagination.
     */
    @Builder
    public SchemaFilters(
            String name,
            String key,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.name = name;
        this.key = key;
    }

    /**
     * Generates a  Specification for Schema lookups.
     *
     * @return Specification object
     */
    public Specification<SchemaRow> getSchemaSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + this.name.toLowerCase(Locale.ROOT) + "%"));
            }

            if (StringUtils.isNotBlank(this.key)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("key")),
                                "%" + this.key.toLowerCase(Locale.ROOT) + "%"));
            }

            return !predicates.isEmpty()
                    ? criteriaBuilder.or(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }
}
