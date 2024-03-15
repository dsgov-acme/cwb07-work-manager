package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.AllowedLink;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.repository.AllowedLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AllowedLinkServiceTest {
    @Mock private AllowedLinkRepository repository;

    @Mock private TransactionLinkTypeService transactionLinkTypeService;

    private AllowedLinkService allowedLinkService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        allowedLinkService = new AllowedLinkService(repository, transactionLinkTypeService);
    }

    @Test
    void testGetAllowedLinksByDefinitionKey() {
        String definitionKey = "myDefinitionKey";

        // Mocking the repository
        List<AllowedLink> expectedLinks = createExpectedLinks();
        when(repository.findByDefinitionKey(definitionKey)).thenReturn(expectedLinks);

        // Call the method under test
        List<AllowedLink> actualLinks =
                allowedLinkService.getAllowedLinksByDefinitionKey(definitionKey);

        // Verify the result
        assertEquals(expectedLinks, actualLinks);
    }

    @Test
    void testSaveAllowedLink() {
        // Test data
        AllowedLink allowedLink =
                AllowedLink.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionKey("myDefinitionKey")
                        .build();

        UUID transactionLinkTypeId = UUID.randomUUID();
        TransactionLinkType transactionLinkType = new TransactionLinkType();
        when(transactionLinkTypeService.getTransactionLinkTypeById(transactionLinkTypeId))
                .thenReturn(Optional.of(transactionLinkType));

        when(repository.save(any(AllowedLink.class))).then(AdditionalAnswers.returnsFirstArg());

        // Call the method under test
        AllowedLink savedLink =
                allowedLinkService.saveAllowedLink(allowedLink, transactionLinkTypeId);

        // Verify the result
        assertEquals(allowedLink, savedLink);
        assertEquals(transactionLinkType, savedLink.getTransactionLinkType());

        // Verify that the repository save method was called with the expected arguments
        verify(repository).save(allowedLink);
    }

    private List<AllowedLink> createExpectedLinks() {
        List<AllowedLink> links = new ArrayList<>();

        AllowedLink link1 =
                AllowedLink.builder()
                        .id(UUID.fromString("e6c6393e-86a9-4a8a-a3b6-12b7d39d6601"))
                        .transactionDefinitionKey("key1")
                        .transactionLinkType(new TransactionLinkType())
                        .build();

        AllowedLink link2 =
                AllowedLink.builder()
                        .id(UUID.fromString("eaf38c51-1c7d-4a57-b7a1-6f57ef1616d2"))
                        .transactionDefinitionKey("key2")
                        .transactionLinkType(new TransactionLinkType())
                        .build();

        links.add(link1);
        links.add(link2);

        return links;
    }
}
