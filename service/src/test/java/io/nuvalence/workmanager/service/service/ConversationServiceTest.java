package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.models.ConversationFilters;
import io.nuvalence.workmanager.service.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.UUID;

class ConversationServiceTest {

    @Mock private ConversationRepository repository;

    @InjectMocks private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveConversation() {
        Conversation conversation = new Conversation();
        when(repository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation savedConversation = conversationService.saveConversation(new Conversation());
        assertNotNull(savedConversation);
        assertEquals(conversation, savedConversation);

        verify(repository, times(1)).save(any(Conversation.class));
    }

    @Test
    void getConversationsByFilters() {
        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();
        Page<Conversation> conversationPage =
                new PageImpl<>(Collections.singletonList(conversation));

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(conversationPage);

        Page<Conversation> conversationPageResult =
                conversationService.getConversationByFilters(
                        ConversationFilters.builder()
                                .sortBy("legalName")
                                .sortOrder("ASC")
                                .pageNumber(0)
                                .pageSize(10)
                                .build());

        assertEquals(conversationPage, conversationPageResult);
    }
}
