package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.Profile;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.generated.controllers.ConversationsApiDelegate;
import io.nuvalence.workmanager.service.generated.models.AllMessagesConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.CreateMessageModel;
import io.nuvalence.workmanager.service.generated.models.PageConversationsResponseModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import io.nuvalence.workmanager.service.mapper.ConversationMapper;
import io.nuvalence.workmanager.service.mapper.MessageMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.models.ConversationFilters;
import io.nuvalence.workmanager.service.models.MessageFilters;
import io.nuvalence.workmanager.service.service.CommonProfileService;
import io.nuvalence.workmanager.service.service.ConversationService;
import io.nuvalence.workmanager.service.service.EntityReferenceService;
import io.nuvalence.workmanager.service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Implementation of the ConversationsApiDelegate interface.
 * Handles API requests related to conversations.
 */
@Service
@RequiredArgsConstructor
public class ConversationsApiDelegateImpl implements ConversationsApiDelegate {
    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final MessageMapper messageMapper;
    private final AuthorizationHandler authorizationHandler;

    private final CommonProfileService commonProfileService;
    private final EntityReferenceService entityReferenceService;
    private final MessageService messageService;

    private static final String CREATE_CONVERSATION_ACTION = "create-conversations";
    private static final String VIEW_CONVERSATION_ACTION = "view-conversations";
    private static final String REPLY_CONVERSATION_ACTION = "reply-conversations";
    private static final String PROFILE_HEADER_NOT_FOUND = "X-Application-Profile-ID not found";

    @Override
    public ResponseEntity<ConversationResponseModel> postConversation(
            ConversationCreateModel conversationCreateModel, final UUID xapplicationProfileID) {

        // access verification
        Profile requestProfile =
                validateAccessAndGetProfile(CREATE_CONVERSATION_ACTION, xapplicationProfileID);
        // (access is guaranteed after this point)

        // data relationships
        Conversation conversation =
                conversationMapper.createModelToConversation(conversationCreateModel);

        EntityReference entityReference = conversation.getEntityReference();

        entityReferenceService.validateEntityReference(entityReference, xapplicationProfileID);

        // message creation and response
        Message message = messageMapper.createModelToMessage(conversationCreateModel.getMessage());
        message.setOriginalMessage(true);
        conversation.setReplies(List.of(message));

        message.setSender(conversationService.createSenderFromCurrentUser(requestProfile));

        Conversation savedConversation = conversationService.saveConversation(conversation);

        ConversationResponseModel conversationResponseModel =
                conversationMapper.conversationToResponseModel(savedConversation);

        return ResponseEntity.ok(conversationResponseModel);
    }

    @Override
    public ResponseEntity<PageConversationsResponseModel> getConversations(
            String referenceType,
            UUID referenceId,
            UUID xApplicationProfileID,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        validateAccessAndGetProfile(VIEW_CONVERSATION_ACTION, xApplicationProfileID);

        List<EntityReference> references =
                entityReferenceService.findByEntityIdAndEntityType(
                        referenceId, EntityType.valueOf(referenceType));
        if (references.isEmpty()) {
            references = new ArrayList<>();
            references.add(
                    EntityReference.builder()
                            .entityId(referenceId)
                            .type(EntityType.valueOf(referenceType))
                            .build());
        }

        references.forEach(
                entityReference ->
                        entityReferenceService.validateEntityReference(
                                entityReference, xApplicationProfileID));

        Page<ConversationResponseModel> results =
                conversationService
                        .getConversationByFilters(
                                ConversationFilters.builder()
                                        .referenceType(referenceType)
                                        .referenceId(referenceId)
                                        .pageNumber(pageNumber)
                                        .sortBy(sortBy)
                                        .pageSize(pageSize)
                                        .sortOrder(sortOrder)
                                        .build())
                        .map(conversationMapper::conversationToResponseModel);

        PageConversationsResponseModel response = new PageConversationsResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ResponseMessageModel> createMessage(
            UUID conversationId,
            CreateMessageModel createMessageModel,
            UUID xapplicationProfileID) {

        Profile requestProfile =
                validateAccessAndGetProfile(REPLY_CONVERSATION_ACTION, xapplicationProfileID);

        Conversation conversation =
                conversationService
                        .getConversationById(conversationId)
                        .orElseThrow(() -> new NotFoundException("Conversation not found"));

        EntityReference entityReference = conversation.getEntityReference();
        entityReferenceService.validateEntityReference(entityReference, xapplicationProfileID);

        Message message = messageMapper.createModelToMessage(createMessageModel);
        message.setSender(conversationService.createSenderFromCurrentUser(requestProfile));
        message.setConversation(conversation);
        Message responseMessage = messageService.saveMessage(message);

        ResponseMessageModel responseModel = messageMapper.messageToResponseModel(responseMessage);
        return ResponseEntity.status(HttpStatus.OK).body(responseModel);
    }

    @Override
    public ResponseEntity<AllMessagesConversationResponseModel> getConversation(
            UUID conversationId,
            UUID xApplicationProfileID,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        validateAccessAndGetProfile(VIEW_CONVERSATION_ACTION, xApplicationProfileID);

        Optional<Conversation> optionalConversation =
                conversationService.getConversationById(conversationId);
        if (optionalConversation.isEmpty()) {
            throw new NotFoundException("Conversation not found");
        }
        Conversation conversation = optionalConversation.get();

        entityReferenceService.validateEntityReference(
                conversation.getEntityReference(), xApplicationProfileID);

        Page<Message> messages =
                messageService.getMessageByFilters(
                        MessageFilters.builder()
                                .conversationId(conversationId)
                                .pageNumber(pageNumber)
                                .sortBy(sortBy)
                                .pageSize(pageSize)
                                .sortOrder(sortOrder)
                                .build());
        conversation.setReplies(messages.toList());

        AllMessagesConversationResponseModel responseModel =
                conversationMapper.conversationToAllMessagesResponseModel(conversation);
        responseModel.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(messages));

        return ResponseEntity.ok(responseModel);
    }

    /**
     * Validates access to wanted profile even if null, based on Cerbos rules.
     *
     * @param action cerbos action to validate
     * @param profileId can be null. And access is still verified over cerbos rules.
     * @return Profile found, or null if profileId is null and general access to action is granted
     *
     * @throws NotFoundException if profileId is not null and not found
     * @throws ForbiddenException if access is not allowed either to the specific profile
     * wanted or to the general action based on cerbos rules
     */
    private @Nullable Profile validateAccessAndGetProfile(@NotNull String action, UUID profileId) {

        Profile requestProfile = null;
        if (profileId != null) {
            requestProfile =
                    commonProfileService
                            .getProfileById(profileId)
                            .filter(
                                    p ->
                                            authorizationHandler.isAllowedForInstance(
                                                    VIEW_CONVERSATION_ACTION, p))
                            .orElseThrow(() -> new NotFoundException(PROFILE_HEADER_NOT_FOUND));

            if (!authorizationHandler.isAllowedForInstance(action, requestProfile)) {
                throw new ForbiddenException("Forbidden action on this profile");
            }

        } else if (!(authorizationHandler.isAllowed(action, Individual.class)
                && authorizationHandler.isAllowed(action, Employer.class))) {
            throw new ForbiddenException("Please provide X-Application-Profile-ID header");
        }

        return requestProfile;
    }
}
