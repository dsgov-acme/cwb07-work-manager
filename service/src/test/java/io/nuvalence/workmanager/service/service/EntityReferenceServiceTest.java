package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class EntityReferenceServiceTest {

    @Mock private CommonProfileService commonProfileService;

    @Mock private TransactionService transactionService;

    @InjectMocks private EntityReferenceService entityReferenceService;

    private static final UUID entityId = UUID.randomUUID();
    private static final UUID profileId = UUID.randomUUID();
    private static final UUID subjectProfileId = UUID.randomUUID();
    private static final UUID additionalPartyProfileId = UUID.randomUUID();

    @Test
    void validateEntityReference_EmployerExists_ProfileIdNull() {
        when(commonProfileService.getProfileById(entityId))
                .thenReturn(Optional.of(Employer.builder().build()));
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.EMPLOYER).entityId(entityId).build();

        assertDoesNotThrow(
                () -> entityReferenceService.validateEntityReference(entityReference, null));
    }

    @Test
    void validateEntityReference_EmployerExists_ProfileIdMatches() {
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.EMPLOYER).entityId(entityId).build();

        assertDoesNotThrow(
                () -> entityReferenceService.validateEntityReference(entityReference, entityId));
    }

    @Test
    void validateEntityReference_EmployerExists_ProfileIdDoesNotMatch() {
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.EMPLOYER).entityId(entityId).build();

        assertThrows(
                ForbiddenException.class,
                () -> entityReferenceService.validateEntityReference(entityReference, profileId));
    }

    @Test
    void validateEntityReference_TransactionExists_ProfileIdNull() {
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.TRANSACTION).entityId(entityId).build();
        Transaction transaction = new Transaction();
        transaction.setSubjectProfileId(subjectProfileId);
        when(transactionService.getTransactionById(entityId)).thenReturn(Optional.of(transaction));

        assertDoesNotThrow(
                () -> entityReferenceService.validateEntityReference(entityReference, null));
    }

    @Test
    void validateEntityReference_TransactionExists_ProfileIdMatchesSubject() {
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.TRANSACTION).entityId(entityId).build();
        Transaction transaction = new Transaction();
        transaction.setSubjectProfileId(subjectProfileId);
        when(transactionService.getTransactionById(entityId)).thenReturn(Optional.of(transaction));

        assertDoesNotThrow(
                () ->
                        entityReferenceService.validateEntityReference(
                                entityReference, subjectProfileId));
    }

    @Test
    void validateEntityReference_TransactionExists_ProfileIdMatchesAdditionalParty() {
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.TRANSACTION).entityId(entityId).build();
        Transaction transaction = new Transaction();
        transaction.setSubjectProfileId(subjectProfileId);
        transaction.setAdditionalParties(
                List.of(RelatedParty.builder().profileId(additionalPartyProfileId).build()));
        when(transactionService.getTransactionById(entityId)).thenReturn(Optional.of(transaction));

        assertDoesNotThrow(
                () ->
                        entityReferenceService.validateEntityReference(
                                entityReference, additionalPartyProfileId));
    }

    @Test
    void validateEntityReference_TransactionExists_ProfileIdDoesNotMatch() {
        EntityReference entityReference =
                EntityReference.builder().type(EntityType.TRANSACTION).entityId(entityId).build();
        Transaction transaction = new Transaction();
        transaction.setSubjectProfileId(subjectProfileId);
        transaction.setAdditionalParties(new ArrayList<>());
        when(transactionService.getTransactionById(entityId)).thenReturn(Optional.of(transaction));

        assertThrows(
                ForbiddenException.class,
                () -> entityReferenceService.validateEntityReference(entityReference, profileId));
    }
}
