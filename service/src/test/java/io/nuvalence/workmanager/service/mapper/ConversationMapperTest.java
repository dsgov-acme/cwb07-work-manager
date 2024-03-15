package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageSender;
import io.nuvalence.workmanager.service.generated.models.AllMessagesConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.MessageSenderModel;
import io.nuvalence.workmanager.service.generated.models.ReferencedEntityModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import io.nuvalence.workmanager.service.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ConversationMapperTest {

    @Mock private MessageMapper messageMapper;
    @Mock private MessageService messageService;

    @InjectMocks private ConversationMapper mapper = new ConversationMapperImpl(messageMapper);

    @Test
    void testConversationToResponseModel() {
        when(messageService.countByConversationId(any())).thenReturn(0);
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createOriginalMessage()));

        // Create a sample Conversation instance
        Conversation conversation = new Conversation();
        conversation.setSubject("subject");
        EntityReference entityReference =
                EntityReference.builder()
                        .id(UUID.randomUUID())
                        .type(EntityType.TRANSACTION)
                        .entityId(UUID.randomUUID())
                        .build();
        conversation.setEntityReference(entityReference);
        conversation.addReply(Message.builder().originalMessage(true).build());

        // Call the mapper method
        ConversationResponseModel responseModel = mapper.conversationToResponseModel(conversation);

        // Assertions
        assertNotNull(responseModel);
    }

    @Test
    void testCreateModelToConversation() {
        // Create a sample ConversationCreateModel instance
        ConversationCreateModel createModel = new ConversationCreateModel();
        createModel.setSubject("subject");
        // Set necessary properties on createModel

        // Call the mapper method
        Conversation conversation = mapper.createModelToConversation(createModel);
        // Assertions
        assertNotNull(conversation);
    }

    @Test
    void testConversationToAllMessagesResponseModel() {
        when(messageService.countByConversationId(any())).thenReturn(0);
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createOriginalMessage()));

        AllMessagesConversationResponseModel responseModel =
                mapper.conversationToAllMessagesResponseModel(createConversation());

        assertEquals(createAllMessagesConversationResponseModel(), responseModel);
    }

    private Conversation createConversation() {
        Conversation conversation = new Conversation();
        conversation.setSubject("subject");
        EntityReference entityReference =
                EntityReference.builder()
                        .id(UUID.fromString("dae3bd18-67c5-4946-8916-1f02e5e3dcff"))
                        .type(EntityType.TRANSACTION)
                        .entityId(UUID.fromString("dae3bd18-67c5-4946-8916-1f02e5e3dcff"))
                        .build();
        conversation.setEntityReference(entityReference);

        MessageSender sender =
                MessageSender.builder()
                        .id(UUID.fromString("e3e3bd18-67c5-4946-8916-1f02e5e3dcff"))
                        .userId(UUID.fromString("29df978b-3a66-4f6e-ab45-affb7010f93e"))
                        .displayName("displayName")
                        .userType("public")
                        .profileId(UUID.fromString("ed411f14-9b4a-484e-90b4-c5d18c02d8a7"))
                        .profileType(ProfileType.INDIVIDUAL)
                        .build();

        conversation.setReplies(List.of());
        return conversation;
    }

    private AllMessagesConversationResponseModel createAllMessagesConversationResponseModel() {
        AllMessagesConversationResponseModel responseModel =
                new AllMessagesConversationResponseModel();
        responseModel.setSubject("subject");
        ReferencedEntityModel entityReferenceModel = new ReferencedEntityModel();
        entityReferenceModel.setEntityId(UUID.fromString("dae3bd18-67c5-4946-8916-1f02e5e3dcff"));
        entityReferenceModel.setType(EntityType.TRANSACTION.getValue());
        responseModel.setEntityReference(entityReferenceModel);

        List<ResponseMessageModel> messages = List.of();
        responseModel.setMessages(messages);
        responseModel.setTotalMessages(0);

        return responseModel;
    }

    private MessageSenderModel createSenderModel() {
        MessageSenderModel senderModel = new MessageSenderModel();
        senderModel.setId(UUID.fromString("e3e3bd18-67c5-4946-8916-1f02e5e3dcff"));
        senderModel.setUserId(UUID.fromString("29df978b-3a66-4f6e-ab45-affb7010f93e"));
        senderModel.setDisplayName("displayName");
        senderModel.setUserType("public");
        senderModel.setProfileId(UUID.fromString("ed411f14-9b4a-484e-90b4-c5d18c02d8a7"));
        senderModel.setProfileType("INDIVIDUAL");

        return senderModel;
    }

    private Message createOriginalMessage() {
        return Message.builder()
                .id(UUID.fromString("eb9e72e1-274a-4fad-bb33-1ee2c6369cd9"))
                .originalMessage(true)
                .sender(createSender())
                .body("body1")
                .build();
    }

    private MessageSender createSender() {
        return MessageSender.builder()
                .id(UUID.fromString("e3e3bd18-67c5-4946-8916-1f02e5e3dcff"))
                .userId(UUID.fromString("29df978b-3a66-4f6e-ab45-affb7010f93e"))
                .profileId(UUID.fromString("ed411f14-9b4a-484e-90b4-c5d18c02d8a7"))
                .displayName("displayName")
                .userType("public")
                .profileId(UUID.fromString("ed411f14-9b4a-484e-90b4-c5d18c02d8a7"))
                .profileType(ProfileType.INDIVIDUAL)
                .build();
    }
}
