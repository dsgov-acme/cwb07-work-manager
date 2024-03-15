package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository
        extends JpaRepository<Message, UUID>, JpaSpecificationExecutor<Message> {
    Optional<Message> findByConversationIdAndOriginalMessage(
            UUID conversationId, Boolean originalMessage);

    int countByConversationId(UUID conversationId);
}
