package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ConversationRepository
        extends JpaRepository<Conversation, UUID>, JpaSpecificationExecutor<Conversation> {}
