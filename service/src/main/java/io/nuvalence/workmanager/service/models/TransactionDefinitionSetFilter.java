package io.nuvalence.workmanager.service.models;

import lombok.Getter;
import lombok.Setter;

/**
 * The filters to filter the transaction sets.
 */
@Getter
@Setter
public class TransactionDefinitionSetFilter extends BaseFilters {

    /**
     * The key to filter by.
     */
    private String key;

    /**
     * Instantiates a new Transaction definition set filter.
     *
     * @param sortOrder  the sort order
     * @param pageNumber the page number
     * @param pageSize   the page size
     */
    public TransactionDefinitionSetFilter(String sortOrder, Integer pageNumber, Integer pageSize) {
        super("key", sortOrder, pageNumber, pageSize);
    }
}
