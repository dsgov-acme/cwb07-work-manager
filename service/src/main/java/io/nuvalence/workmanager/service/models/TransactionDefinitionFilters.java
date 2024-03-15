package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

/**
 * Filters for Transaction Definitions.
 */
public class TransactionDefinitionFilters {

    private static final String DEFAULT_SORT_BY = "createdTimestamp";
    private final String name;
    private final String sortBy;
    private final String sortOrder;
    private final Integer pageNumber;
    private final Integer pageSize;

    /**
     * Constructor.
     *
     * @param name name
     * @param sortBy sort by
     * @param sortOrder sort order
     * @param pageNumber page number
     * @param pageSize page size
     */
    public TransactionDefinitionFilters(
            String name, String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
        this.name = name;
        this.sortBy = sortBy == null ? DEFAULT_SORT_BY : sortBy;
        this.sortOrder = sortOrder;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * Generates a  Specification object for all transaction definition lookups.
     *
     * @return Specification object
     */
    public Specification<TransactionDefinition> getTransactionDefinitionSpecifications() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + this.name.toLowerCase(Locale.ROOT) + "%"));
            }

            return !predicates.isEmpty()
                    ? criteriaBuilder.or(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }

    /**
     * Generates Pagination object.
     *
     * @return Pagination object
     */
    public PageRequest getPageRequest() {
        Sort sort;

        if (sortOrder.equalsIgnoreCase("desc")) {
            sort = Sort.by(Sort.Direction.DESC, this.sortBy);
        } else {
            sort = Sort.by(Sort.Direction.ASC, this.sortBy);
        }

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
