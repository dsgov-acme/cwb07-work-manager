package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.repository.EntityReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@Service
@RequiredArgsConstructor
public class EntityReferenceService {
    private final EntityReferenceRepository entityReferenceRepository;
    private final CommonProfileService commonProfileService;
    private final TransactionService transactionService;

    public List<EntityReference> findByEntityIdAndEntityType(UUID entityId, EntityType entityType) {
        return entityReferenceRepository.findByEntityIdAndType(entityId, entityType);
    }

    /**
     * Verifies the entity reference exists and is associated with the xapplicationProfileID if provided.
     *
     * @param entityReference entity reference to validate
     * @param profileId Profile to check association. (Access level must be cerbos-verified
     * before calling this method)
     *
     * @throws NotFoundException if entity reference is not found
     * @throws ProvidedDataException if entity reference is not associated with the profile provided
     */
    public void validateEntityReference(@NotNull EntityReference entityReference, UUID profileId) {

        switch (entityReference.getType()) {
            case EMPLOYER:
                if (profileId == null) {
                    commonProfileService
                            .getProfileById(entityReference.getEntityId())
                            .orElseThrow(() -> new NotFoundException("Employer entity not found"));

                } else if (!entityReference.getEntityId().equals(profileId)) {
                    throw new ForbiddenException(
                            "X-Application-Profile-ID header provided is not associated with the"
                                    + " EMPLOYER entity");
                }
                break;

            case TRANSACTION:
                Transaction transaction =
                        transactionService
                                .getTransactionById(entityReference.getEntityId())
                                .orElseThrow(
                                        () ->
                                                new NotFoundException(
                                                        "Transaction entity not found"));

                if (profileId != null && !profileId.equals(transaction.getSubjectProfileId())) {
                    transaction.getAdditionalParties().stream()
                            .filter(p -> profileId.equals(p.getProfileId()))
                            .findAny()
                            .orElseThrow(
                                    () ->
                                            new ForbiddenException(
                                                    "X-Application-Profile-ID header provided is"
                                                            + " not associated with the TRANSACTION"
                                                            + " entity"));
                }
                break;

            default:
                throw new ProvidedDataException("Invalid entity type");
        }
    }
}
