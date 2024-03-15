package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.generated.models.AllMessagesConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import io.nuvalence.workmanager.service.service.MessageService;
import lombok.Setter;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        uses = {MessageMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class ConversationMapper {

    @Autowired @Setter protected MessageService messageService;

    @Autowired @Setter protected MessageMapper messageMapper;

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(
            target = "originalMessage",
            expression = "java(getOriginalMessageModel(conversation.getId()))")
    @Mapping(
            target = "totalMessages",
            expression = "java(countByConversationId(conversation.getId()))")
    public abstract ConversationResponseModel conversationToResponseModel(
            Conversation conversation);

    public abstract Conversation createModelToConversation(
            ConversationCreateModel conversationCreateModel);

    @Mapping(target = "messages", source = "replies")
    @Mapping(
            target = "totalMessages",
            expression = "java(countByConversationId(conversation.getId()))")
    @Mapping(
            target = "originalMessage",
            expression = "java(getOriginalMessageModel(conversation.getId()))")
    public abstract AllMessagesConversationResponseModel conversationToAllMessagesResponseModel(
            Conversation conversation);

    protected int countByConversationId(final UUID conversationId) {
        return messageService.countByConversationId(conversationId);
    }

    protected ResponseMessageModel getOriginalMessageModel(UUID conversationId) {
        return messageMapper.messageToResponseModel(
                messageService
                        .getOriginalMessageByConversationId(conversationId)
                        .orElseThrow(() -> new RuntimeException("Original message not found")));
    }
}
