package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * Document provided repository.
 */
public interface CustomerProvidedDocumentRepository
        extends CrudRepository<CustomerProvidedDocument, UUID> {}
