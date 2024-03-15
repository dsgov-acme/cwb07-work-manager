package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.AllowedLink;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionDefinitionException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLink;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkNotAllowedException;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.generated.models.LinkedTransaction;
import io.nuvalence.workmanager.service.repository.TransactionLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionLinkServiceTest {

    private static final String NOT_FOUND_DEF_KEY = "NOT_FOUND_DEF_KEY";

    private TransactionDefinitionService transactionDefinitionService;
    private AllowedLinkService allowedLinkService;
    private TransactionService transactionService;
    private TransactionLinkRepository repository;
    private TransactionLinkTypeService transactionLinkTypeService;

    private TransactionLinkService transactionLinkService;

    @BeforeEach
    void setUp() {

        transactionDefinitionService = mock(TransactionDefinitionService.class);
        allowedLinkService = mock(AllowedLinkService.class);
        transactionService = mock(TransactionService.class);
        repository = mock(TransactionLinkRepository.class);
        transactionLinkTypeService = mock(TransactionLinkTypeService.class);

        transactionLinkService =
                new TransactionLinkService(
                        transactionDefinitionService,
                        allowedLinkService,
                        transactionService,
                        repository,
                        transactionLinkTypeService);
    }

    @Test
    void shouldCreateInstance() throws Exception {

        assertNotNull(transactionLinkService);

        assertNotNull(transactionDefinitionService);
        assertNotNull(allowedLinkService);
        assertNotNull(transactionService);
        assertNotNull(repository);
        assertNotNull(transactionLinkTypeService);
    }

    @Test
    void saveTransactionLink_NullFromTransaction() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();
        String defKey = "anyKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        false,
                        toTransactionId,
                        true,
                        defKey,
                        defKey,
                        null,
                        null);

        // checks
        assertThrows(
                MissingTransactionException.class,
                () -> {
                    transactionLinkService.saveTransactionLink(
                            transactionLink, savingTransactionLinkTypeId);
                });

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verifyNoMoreInteractions(transactionService);

        // never calls
        verify(transactionDefinitionService, never()).getTransactionDefinitionByKey(anyString());
        verify(allowedLinkService, never()).getAllowedLinksByDefinitionKey(anyString());
        verify(transactionLinkTypeService, never()).getTransactionLinkTypeById(any(UUID.class));

        // never save
        verify(repository, never()).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_NullToTransaction() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();
        String defKey = "anyKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        false,
                        defKey,
                        defKey,
                        null,
                        null);

        // checks
        assertThrows(
                MissingTransactionException.class,
                () -> {
                    transactionLinkService.saveTransactionLink(
                            transactionLink, savingTransactionLinkTypeId);
                });

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verify(transactionService, times(1)).getTransactionById(toTransactionId);
        verifyNoMoreInteractions(transactionService);

        // never calls
        verify(transactionDefinitionService, never()).getTransactionDefinitionByKey(anyString());
        verify(allowedLinkService, never()).getAllowedLinksByDefinitionKey(anyString());
        verify(transactionLinkTypeService, never()).getTransactionLinkTypeById(any(UUID.class));

        // never save
        verify(repository, never()).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_FailForSameTransaction() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        String singleTransactionIdString = UUID.randomUUID().toString();

        UUID fromTransactionId = UUID.fromString(singleTransactionIdString);
        UUID toTransactionId = UUID.fromString(singleTransactionIdString);

        String defKey = "anyKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        false,
                        defKey,
                        defKey,
                        null,
                        null);

        // checks
        assertThrows(
                TransactionLinkNotAllowedException.class,
                () -> {
                    transactionLinkService.saveTransactionLink(
                            transactionLink, savingTransactionLinkTypeId);
                });

        // never calls
        verify(transactionService, never()).getTransactionById(any());
        verify(transactionDefinitionService, never()).getTransactionDefinitionByKey(anyString());
        verify(allowedLinkService, never()).getAllowedLinksByDefinitionKey(anyString());
        verify(transactionLinkTypeService, never()).getTransactionLinkTypeById(any(UUID.class));

        // never save
        verify(repository, never()).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_SaveWithSameDefinition() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();

        String singleDefKey = "singleDefKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        true,
                        singleDefKey,
                        singleDefKey,
                        null,
                        null);

        var result =
                transactionLinkService.saveTransactionLink(
                        transactionLink, savingTransactionLinkTypeId);

        // checks
        assertEquals(savingTransactionLinkTypeId, result.getTransactionLinkType().getId());

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verify(transactionService, times(1)).getTransactionById(toTransactionId);
        verifyNoMoreInteractions(transactionService);

        verify(transactionLinkTypeService, times(1))
                .getTransactionLinkTypeById(savingTransactionLinkTypeId);

        // never calls
        verify(transactionDefinitionService, never()).getTransactionDefinitionByKey(anyString());
        verify(allowedLinkService, never()).getAllowedLinksByDefinitionKey(anyString());

        // check save
        verify(repository, times(1)).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_SaveWithDifferentDefinition_noFromDef() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();

        String fromDefKey = NOT_FOUND_DEF_KEY;
        String toDefKey = "toDefKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        true,
                        fromDefKey,
                        toDefKey,
                        null,
                        null);

        // checks
        assertThrows(
                MissingTransactionDefinitionException.class,
                () -> {
                    transactionLinkService.saveTransactionLink(
                            transactionLink, savingTransactionLinkTypeId);
                });

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verify(transactionService, times(1)).getTransactionById(toTransactionId);
        verifyNoMoreInteractions(transactionService);

        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(fromDefKey);

        // never calls
        verify(transactionLinkTypeService, never())
                .getTransactionLinkTypeById(savingTransactionLinkTypeId);
        verify(allowedLinkService, never()).getAllowedLinksByDefinitionKey(anyString());

        // never save
        verify(repository, never()).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_SaveWithDifferentDefinition_noToDef() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();

        String fromDefKey = "fromDefKey";
        String toDefKey = NOT_FOUND_DEF_KEY;

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        true,
                        fromDefKey,
                        toDefKey,
                        null,
                        null);

        // checks
        assertThrows(
                MissingTransactionDefinitionException.class,
                () -> {
                    transactionLinkService.saveTransactionLink(
                            transactionLink, savingTransactionLinkTypeId);
                });

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verify(transactionService, times(1)).getTransactionById(toTransactionId);
        verifyNoMoreInteractions(transactionService);

        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(fromDefKey);
        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(toDefKey);
        verifyNoMoreInteractions(transactionDefinitionService);

        // never calls
        verify(transactionLinkTypeService, never())
                .getTransactionLinkTypeById(savingTransactionLinkTypeId);
        verify(allowedLinkService, never()).getAllowedLinksByDefinitionKey(anyString());

        // never save
        verify(repository, never()).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_SaveWithDifferentDefinition_MatchingTypes() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();
        UUID singleLinkerTypeId = UUID.randomUUID();

        String fromDefKey = "fromDefKey";
        String toDefKey = "toDefKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        true,
                        fromDefKey,
                        toDefKey,
                        singleLinkerTypeId,
                        singleLinkerTypeId);

        var result =
                transactionLinkService.saveTransactionLink(
                        transactionLink, savingTransactionLinkTypeId);

        // checks
        assertEquals(savingTransactionLinkTypeId, result.getTransactionLinkType().getId());

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verify(transactionService, times(1)).getTransactionById(toTransactionId);
        verifyNoMoreInteractions(transactionService);

        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(fromDefKey);
        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(toDefKey);
        verifyNoMoreInteractions(transactionDefinitionService);

        verify(transactionLinkTypeService, times(1))
                .getTransactionLinkTypeById(savingTransactionLinkTypeId);

        verify(allowedLinkService, times(1)).getAllowedLinksByDefinitionKey(fromDefKey);
        verify(allowedLinkService, times(1)).getAllowedLinksByDefinitionKey(toDefKey);
        verifyNoMoreInteractions(allowedLinkService);

        // check save
        verify(repository, times(1)).save(any(TransactionLink.class));
    }

    @Test
    void saveTransactionLink_SaveWithDifferentDefinition_MismatchingTypes() throws Exception {

        UUID savingTransactionLinkTypeId = UUID.randomUUID();

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();
        UUID fromLinkerTypeId = UUID.randomUUID();
        UUID toLinkerTypeId = UUID.randomUUID();

        String fromDefKey = "fromDefKey";
        String toDefKey = "toDefKey";

        TransactionLink transactionLink =
                configSaveTransactionLinkTest(
                        savingTransactionLinkTypeId,
                        fromTransactionId,
                        true,
                        toTransactionId,
                        true,
                        fromDefKey,
                        toDefKey,
                        fromLinkerTypeId,
                        toLinkerTypeId);
        // checks
        assertThrows(
                TransactionLinkNotAllowedException.class,
                () -> {
                    transactionLinkService.saveTransactionLink(
                            transactionLink, savingTransactionLinkTypeId);
                });

        verify(transactionService, times(1)).getTransactionById(fromTransactionId);
        verify(transactionService, times(1)).getTransactionById(toTransactionId);
        verifyNoMoreInteractions(transactionService);

        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(fromDefKey);
        verify(transactionDefinitionService, times(1)).getTransactionDefinitionByKey(toDefKey);
        verifyNoMoreInteractions(transactionDefinitionService);

        verify(transactionLinkTypeService, times(1))
                .getTransactionLinkTypeById(savingTransactionLinkTypeId);

        verify(allowedLinkService, times(1)).getAllowedLinksByDefinitionKey(fromDefKey);
        verify(allowedLinkService, times(1)).getAllowedLinksByDefinitionKey(toDefKey);
        verifyNoMoreInteractions(allowedLinkService);

        // never save
        verify(repository, never()).save(any(TransactionLink.class));
    }

    @Test
    void testGetLinkedTransactionsByTransactionId() {

        // start end
        TransactionLinkType startType = new TransactionLinkType();
        String fromPreviousDescription = "From previous";
        String toLinkerDescription = "To linker";
        startType.setFromDescription(fromPreviousDescription);
        startType.setToDescription(toLinkerDescription);

        UUID previousTransactionId = UUID.randomUUID();
        UUID linkerTransactionId = UUID.randomUUID();

        TransactionLink tlFromPreviousToLinker = new TransactionLink();
        tlFromPreviousToLinker.setFromTransactionId(previousTransactionId);
        tlFromPreviousToLinker.setToTransactionId(linkerTransactionId);
        tlFromPreviousToLinker.setTransactionLinkType(startType);

        // finish end
        TransactionLinkType finishType = new TransactionLinkType();
        String fromLinkerDescription = "From linker";
        String toNextDescription = "To next";
        finishType.setFromDescription(fromLinkerDescription);
        finishType.setToDescription(toNextDescription);

        UUID nextTransactionId = UUID.randomUUID();

        TransactionLink tlFromLinkerToRandom = new TransactionLink();
        tlFromLinkerToRandom.setFromTransactionId(linkerTransactionId);
        tlFromLinkerToRandom.setToTransactionId(nextTransactionId);
        tlFromLinkerToRandom.setTransactionLinkType(finishType);

        when(repository.getTransactionLinksById(linkerTransactionId))
                .thenReturn(Arrays.asList(tlFromPreviousToLinker, tlFromLinkerToRandom));

        // test
        List<LinkedTransaction> linkedTransactions =
                transactionLinkService.getLinkedTransactionsById(linkerTransactionId);

        // check

        verify(repository, times(1)).getTransactionLinksById(linkerTransactionId);
        verifyNoMoreInteractions(repository);

        assertEquals(2, linkedTransactions.size());

        LinkedTransaction linkedTransaction1 = linkedTransactions.get(0);
        assertEquals(previousTransactionId, linkedTransaction1.getLinkedTransactionId());
        assertEquals(toLinkerDescription, linkedTransaction1.getDescription());

        LinkedTransaction linkedTransaction2 = linkedTransactions.get(1);
        assertEquals(nextTransactionId, linkedTransaction2.getLinkedTransactionId());
        assertEquals(fromLinkerDescription, linkedTransaction2.getDescription());
    }

    @Test
    void testGetLinkedTransactionsByTransactionId_NoLinksFound() {

        UUID linkerTransactionId = UUID.randomUUID();

        when(repository.getTransactionLinksById(linkerTransactionId))
                .thenReturn(new ArrayList<TransactionLink>());

        // test
        List<LinkedTransaction> linkedTransactions =
                transactionLinkService.getLinkedTransactionsById(linkerTransactionId);

        // check
        verify(repository, times(1)).getTransactionLinksById(linkerTransactionId);
        verifyNoMoreInteractions(repository);

        assertEquals(0, linkedTransactions.size());
    }

    @Test
    void testRemoveTransactionLinks() {
        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();
        UUID transactionLinkTypeId = UUID.randomUUID();

        TransactionLink transactionLink1 = new TransactionLink();
        transactionLink1.setId(UUID.randomUUID());
        TransactionLink transactionLink2 = new TransactionLink();
        transactionLink2.setId(UUID.randomUUID());

        when(repository.getTransactionLinkByFromAndToAndType(
                        fromTransactionId, toTransactionId, transactionLinkTypeId))
                .thenReturn(Arrays.asList(transactionLink1, transactionLink2));

        transactionLinkService.removeTransactionLink(
                transactionLinkTypeId, fromTransactionId, toTransactionId);

        verify(repository, times(1))
                .getTransactionLinkByFromAndToAndType(
                        fromTransactionId, toTransactionId, transactionLinkTypeId);

        verify(repository).delete(transactionLink1);
        verify(repository).delete(transactionLink2);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void testRemoveTransactionLinks_NotFound() {

        UUID fromTransactionId = UUID.randomUUID();
        UUID toTransactionId = UUID.randomUUID();
        UUID transactionLinkTypeId = UUID.randomUUID();

        when(repository.getTransactionLinkByFromAndToAndType(
                        fromTransactionId, toTransactionId, transactionLinkTypeId))
                .thenReturn(new ArrayList<TransactionLink>());

        assertThrows(
                NotFoundException.class,
                () -> {
                    transactionLinkService.removeTransactionLink(
                            transactionLinkTypeId, fromTransactionId, toTransactionId);
                });

        verify(repository, times(1))
                .getTransactionLinkByFromAndToAndType(
                        fromTransactionId, toTransactionId, transactionLinkTypeId);

        verify(repository, never()).delete(any(TransactionLink.class));
    }

    private TransactionLink configSaveTransactionLinkTest(
            UUID savingTransactionLinkTypeId,
            UUID fromTransactionId,
            boolean fromTransactionExists,
            UUID toTransactionId,
            boolean toTransactionExists,
            String fromDefKey,
            String toDefKey,
            UUID fromTransactionLinkTypeId,
            UUID toTransactionLinkTypeId) {

        TransactionLink savingTransactionLink = new TransactionLink();
        TransactionLinkType savingTransactionLinkType = new TransactionLinkType();
        savingTransactionLinkType.setId(savingTransactionLinkTypeId);

        savingTransactionLink.setFromTransactionId(fromTransactionId);
        savingTransactionLink.setToTransactionId(toTransactionId);

        Transaction fromTransaction =
                Transaction.builder()
                        .id(fromTransactionId)
                        .transactionDefinitionKey(fromDefKey)
                        .subjectProfileId(UUID.randomUUID())
                        .subjectProfileType(ProfileType.INDIVIDUAL)
                        .additionalParties(Collections.emptyList())
                        .build();

        Transaction toTransaction =
                Transaction.builder()
                        .id(toTransactionId)
                        .transactionDefinitionKey(toDefKey)
                        .subjectProfileId(UUID.randomUUID())
                        .subjectProfileType(ProfileType.INDIVIDUAL)
                        .additionalParties(Collections.emptyList())
                        .build();

        when(transactionService.getTransactionById(fromTransactionId))
                .thenReturn(
                        fromTransactionExists ? Optional.of(fromTransaction) : Optional.empty());

        when(transactionService.getTransactionById(toTransactionId))
                .thenReturn(toTransactionExists ? Optional.of(toTransaction) : Optional.empty());

        TransactionLinkType fromTransactionLinkType = new TransactionLinkType();
        fromTransactionLinkType.setId(fromTransactionLinkTypeId);

        TransactionLinkType toTransactionLinkType = new TransactionLinkType();
        toTransactionLinkType.setId(toTransactionLinkTypeId);

        TransactionDefinition fromDefinition = new TransactionDefinition();
        fromDefinition.setKey(fromDefKey);

        TransactionDefinition toDefinition = new TransactionDefinition();
        toDefinition.setKey(toDefKey);

        AllowedLink fromAllowedLink = new AllowedLink();
        fromAllowedLink.setTransactionLinkType(fromTransactionLinkType);

        AllowedLink toAllowedLink = new AllowedLink();
        toAllowedLink.setTransactionLinkType(toTransactionLinkType);

        when(transactionLinkTypeService.getTransactionLinkTypeById(savingTransactionLinkTypeId))
                .thenReturn(Optional.of(savingTransactionLinkType));

        when(repository.save(savingTransactionLink)).thenReturn(savingTransactionLink);

        when(transactionDefinitionService.getTransactionDefinitionByKey(fromDefKey))
                .thenReturn(
                        fromDefKey.equals(NOT_FOUND_DEF_KEY)
                                ? Optional.empty()
                                : Optional.of(fromDefinition));

        when(transactionDefinitionService.getTransactionDefinitionByKey(toDefKey))
                .thenReturn(
                        toDefKey.equals(NOT_FOUND_DEF_KEY)
                                ? Optional.empty()
                                : Optional.of(toDefinition));

        when(allowedLinkService.getAllowedLinksByDefinitionKey(fromDefinition.getKey()))
                .thenReturn(Collections.singletonList(fromAllowedLink));
        when(allowedLinkService.getAllowedLinksByDefinitionKey(toDefinition.getKey()))
                .thenReturn(Collections.singletonList(toAllowedLink));

        return savingTransactionLink;
    }
}
