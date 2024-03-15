package io.nuvalence.workmanager.service.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Class with basic sort attributes for filters.
 */
@Getter
@Setter
@AllArgsConstructor
@SuppressWarnings("checkstyle:CyclomaticComplexity")
abstract class BaseFilters {
    private String sortBy;
    private String sortOrder;
    private Integer pageNumber;
    private Integer pageSize;

    /**
     * Generates a common Pagination object for all lookups that use specifications pattern.
     *
     * @return Pagination object
     */
    public PageRequest getPageRequest() {
        Sort sort;

        if (sortBy.equals("type")) {
            sortBy = "type.name";
        }

        if (sortOrder.equalsIgnoreCase("desc")) {
            sort = Sort.by(Sort.Direction.DESC, sortBy);
        } else {
            sort = Sort.by(Sort.Direction.ASC, sortBy);
        }

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
