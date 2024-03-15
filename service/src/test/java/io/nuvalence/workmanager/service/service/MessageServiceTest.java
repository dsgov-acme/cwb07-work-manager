package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.models.MessageFilters;
import io.nuvalence.workmanager.service.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock private MessageRepository repository;

    private MessageService service;

    @BeforeEach
    public void setUp() {
        service = new MessageService(repository);
    }

    @Test
    void testGetMessageByFilters() {
        Message message = Message.builder().build();
        Page<Message> messages = new PageImpl<>(Collections.singletonList(message));

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(messages);

        Page<Message> messagePageResult =
                service.getMessageByFilters(
                        MessageFilters.builder()
                                .sortBy("legalName")
                                .sortOrder("ASC")
                                .pageNumber(0)
                                .pageSize(10)
                                .conversationId(UUID.randomUUID())
                                .build());

        assertEquals(messages, messagePageResult);
    }

    @Test
    void testSaveMessage() {
        Message message = Message.builder().build();
        when(repository.save(message)).thenReturn(message);

        Message savedMessage = service.saveMessage(message);

        assertEquals(message, savedMessage);
    }

    @Test
    void testGetOriginalMessageByConversationId() {
        Message message =
                Message.builder()
                        .conversation(Conversation.builder().id(UUID.randomUUID()).build())
                        .build();
        when(repository.findByConversationIdAndOriginalMessage(
                        message.getConversation().getId(), true))
                .thenReturn(Optional.ofNullable(message));

        Optional<Message> originalMessage =
                service.getOriginalMessageByConversationId(message.getConversation().getId());

        assertTrue(originalMessage.isPresent());
        assertEquals(message, originalMessage.get());
    }

    @Test
    void testCountByConversationId() {
        UUID conversationId = UUID.randomUUID();
        when(repository.countByConversationId(conversationId)).thenReturn(1);

        long count = service.countByConversationId(conversationId);

        assertEquals(1, count);
    }
}
