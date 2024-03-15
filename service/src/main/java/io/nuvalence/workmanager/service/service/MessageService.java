package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.models.MessageFilters;
import io.nuvalence.workmanager.service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {
    private final MessageRepository repository;

    public Page<Message> getMessageByFilters(final MessageFilters filters) {
        return repository.findAll(filters.getMessagesByFilters(), filters.getPageRequest());
    }

    public Message saveMessage(final Message message) {
        return repository.save(message);
    }

    public Optional<Message> getOriginalMessageByConversationId(final UUID conversationId) {
        return repository.findByConversationIdAndOriginalMessage(conversationId, true);
    }

    public int countByConversationId(final UUID conversationId) {
        return repository.countByConversationId(conversationId);
    }
}
