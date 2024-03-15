package io.nuvalence.workmanager.service.controllers;

import static io.nuvalence.workmanager.service.domain.securemessaging.EntityType.EMPLOYER;
import static io.nuvalence.workmanager.service.domain.securemessaging.EntityType.TRANSACTION;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.Profile;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageSender;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.CreateMessageModel;
import io.nuvalence.workmanager.service.generated.models.MessageSenderModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import io.nuvalence.workmanager.service.repository.ConversationRepository;
import io.nuvalence.workmanager.service.service.CommonProfileService;
import io.nuvalence.workmanager.service.service.ConversationService;
import io.nuvalence.workmanager.service.service.EntityReferenceService;
import io.nuvalence.workmanager.service.service.MessageService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:individual_profile_admin"})
class ConversationsApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private ConversationRepository repository;

    @MockBean private ConversationService conversationService;

    @MockBean private CommonProfileService commonProfileService;

    @MockBean private TransactionService transactionService;

    @MockBean private EntityReferenceService entityReferenceService;

    @MockBean private MessageService messageService;

    private ObjectMapper objectMapper;

    private static final String VIEW_CONVERSATION_ACTION = "view-conversations";

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);

        this.objectMapper = SpringConfig.getMapper();
    }

    @Test
    void postConversation_NoProfileFound() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.empty());

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("X-Application-Profile-ID not found"));
    }

    @Test
    void postConversation_Header_And_NoProfileAccess() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        when(authorizationHandler.isAllowedForInstance("create-conversations", profile))
                .thenReturn(false);

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0]").value("Forbidden action on this profile"));
    }

    @Test
    void postConversation_NoHeader_And_NoGeneralAccess() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        when(authorizationHandler.isAllowed("create-conversations", Individual.class))
                .thenReturn(false);

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel)))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value("Please provide X-Application-Profile-ID header"));
    }

    @Test
    void postConversation_InvalidEntity() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("SOMETHING_ELSE");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value(
                                        "Field entityReference.type is invalid. Validation pattern"
                                                + " that should be followed:"
                                                + " ^(TRANSACTION|EMPLOYER)$"));
    }

    @Test
    void postConversation_EmployerSuccess() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("EMPLOYER");

        UUID xapplicationProfileID = conversationCreateModel.getEntityReference().getEntityId();

        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(xapplicationProfileID);
        when(profile.getProfileType()).thenReturn(ProfileType.EMPLOYER);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("public");
        UUID userId = UUID.randomUUID();

        when(conversationService.createSenderFromCurrentUser(any())).thenCallRealMethod();

        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), eq(null));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(
                        Optional.of(
                                createBaseConversationForPosting(
                                        createSenderForPosting(
                                                userId,
                                                ProfileType.EMPLOYER,
                                                "The User",
                                                "public",
                                                xapplicationProfileID))));
        when(messageService.countByConversationId(any())).thenReturn(1);

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel))
                                            .header(
                                                    "X-Application-Profile-ID",
                                                    xapplicationProfileID))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("EMPLOYER", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());

            MessageSenderModel sender = responseModel.getOriginalMessage().getSender();
            assertEquals(userId, sender.getUserId());
            assertEquals("The User", sender.getDisplayName());
            assertEquals("public", sender.getUserType());
            assertEquals(xapplicationProfileID, sender.getProfileId());
            assertEquals("EMPLOYER", sender.getProfileType());
        }
    }

    @Test
    void postConversation_TransactionSuccessSubject() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(xapplicationProfileID);
        when(profile.getProfileType()).thenReturn(ProfileType.INDIVIDUAL);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        Transaction transaction = mock(Transaction.class);
        when(transaction.getSubjectProfileId()).thenReturn(xapplicationProfileID);
        when(transactionService.getTransactionById(
                        conversationCreateModel.getEntityReference().getEntityId()))
                .thenReturn(Optional.of(transaction));

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("public");
        UUID userId = UUID.randomUUID();

        when(conversationService.createSenderFromCurrentUser(any())).thenCallRealMethod();

        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), eq(null));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(
                        Optional.of(
                                createBaseConversationForPosting(
                                        createSenderForPosting(
                                                userId,
                                                ProfileType.INDIVIDUAL,
                                                "The User",
                                                "public",
                                                xapplicationProfileID))));
        when(messageService.countByConversationId(any())).thenReturn(1);

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel))
                                            .header(
                                                    "X-Application-Profile-ID",
                                                    xapplicationProfileID))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("TRANSACTION", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());

            MessageSenderModel sender = responseModel.getOriginalMessage().getSender();
            assertEquals(userId, sender.getUserId());
            assertEquals("The User", sender.getDisplayName());
            assertEquals("public", sender.getUserType());
            assertEquals(xapplicationProfileID, sender.getProfileId());
            assertEquals("INDIVIDUAL", sender.getProfileType());
        }
    }

    @Test
    void postConversation_TransactionSuccessRelatedParty() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(xapplicationProfileID);
        when(profile.getProfileType()).thenReturn(ProfileType.EMPLOYER);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        Transaction transaction = mock(Transaction.class);
        when(transaction.getSubjectProfileId()).thenReturn(UUID.randomUUID());
        when(transaction.getAdditionalParties())
                .thenReturn(
                        List.of(
                                RelatedParty.builder()
                                        .profileId(xapplicationProfileID)
                                        .type(ProfileType.EMPLOYER)
                                        .build()));
        when(transactionService.getTransactionById(
                        conversationCreateModel.getEntityReference().getEntityId()))
                .thenReturn(Optional.of(transaction));

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("public");
        UUID userId = UUID.randomUUID();

        when(conversationService.createSenderFromCurrentUser(any())).thenCallRealMethod();
        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), eq(null));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(
                        Optional.of(
                                createBaseConversationForPosting(
                                        createSenderForPosting(
                                                userId,
                                                ProfileType.EMPLOYER,
                                                "The User",
                                                "public",
                                                xapplicationProfileID))));
        when(messageService.countByConversationId(any())).thenReturn(1);

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel))
                                            .header(
                                                    "X-Application-Profile-ID",
                                                    xapplicationProfileID))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("TRANSACTION", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());

            MessageSenderModel sender = responseModel.getOriginalMessage().getSender();
            assertEquals(userId, sender.getUserId());
            assertEquals("The User", sender.getDisplayName());
            assertEquals("public", sender.getUserType());
            assertEquals(xapplicationProfileID, sender.getProfileId());
            assertEquals("EMPLOYER", sender.getProfileType());
        }
    }

    @Test
    void postConversation_NoHeader_GeneralAccess_TransactionSuccess() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID transactionId = conversationCreateModel.getEntityReference().getEntityId();

        when(authorizationHandler.isAllowed("create-conversations", Profile.class))
                .thenReturn(true);

        Transaction transaction = mock(Transaction.class);
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("agency");
        UUID userId = UUID.randomUUID();

        when(conversationService.createSenderFromCurrentUser(any())).thenCallRealMethod();
        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), eq(null));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(
                        Optional.of(
                                createBaseConversationForPosting(
                                        createSenderForPosting(
                                                userId, null, "The User", "agency", null))));
        when(messageService.countByConversationId(any())).thenReturn(1);

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel)))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("TRANSACTION", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());

            MessageSenderModel sender = responseModel.getOriginalMessage().getSender();
            assertEquals(userId, sender.getUserId());
            assertEquals("The User", sender.getDisplayName());
            assertEquals("agency", sender.getUserType());
            assertNull(sender.getProfileId());
            assertNull(sender.getProfileType());
        }
    }

    @Test
    void createMessage_Success() throws Exception {

        Conversation storedConversation = createConversation();
        List<Message> messages = new ArrayList<>();
        messages.add(createMessage());
        storedConversation.setReplies(messages);
        when(conversationService.getConversationById(any()))
                .thenReturn(Optional.of(storedConversation));

        when(transactionService.getTransactionById(any()))
                .thenReturn(Optional.of(mock(Transaction.class)));

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);

        when(messageService.saveMessage(any()))
                .thenReturn(
                        Message.builder()
                                .id(UUID.randomUUID())
                                .body("a message here")
                                .sender(
                                        MessageSender.builder()
                                                .userId(UUID.randomUUID())
                                                .userType("agency")
                                                .displayName("A User")
                                                .build())
                                .conversation(storedConversation)
                                .attachments(
                                        List.of(
                                                UUID.fromString(
                                                        "aaaaaaaa-52e9-41d6-8e71-8582d96ea645"),
                                                UUID.fromString(
                                                        "bbbbbbbb-c7a3-4018-9de8-cac8b0f916c1")))
                                .build());

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("agency");
        UUID userId = UUID.randomUUID();

        when(conversationService.createSenderFromCurrentUser(any())).thenCallRealMethod();

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("A User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post(
                                                    "/api/v1/conversations/{conversationId}/messages",
                                                    UUID.randomUUID())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            createBaseMessage())))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ResponseMessageModel responseModel =
                    objectMapper.readValue(response, ResponseMessageModel.class);

            assertEquals("a message here", responseModel.getBody());
            assertEquals(
                    "aaaaaaaa-52e9-41d6-8e71-8582d96ea645", responseModel.getAttachments().get(0));
            assertEquals(
                    "bbbbbbbb-c7a3-4018-9de8-cac8b0f916c1", responseModel.getAttachments().get(1));
            assertEquals("A User", responseModel.getSender().getDisplayName());
        }
    }

    @Test
    void createMessage_ConversationNotFound() throws Exception {

        when(conversationService.getConversationById(any())).thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBaseMessage())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Conversation not found"));
    }

    @Test
    void createMessage_ProfileAccessForbidden() throws Exception {

        UUID xapplicationProfileID = UUID.randomUUID();
        Profile profile = mock(Profile.class);

        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        when(authorizationHandler.isAllowedForInstance("reply-conversations", profile))
                .thenReturn(false);

        // request
        mockMvc.perform(
                        post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBaseMessage()))
                                .header(
                                        "X-Application-Profile-ID",
                                        xapplicationProfileID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0]").value("Forbidden action on this profile"));
    }

    @Test
    void createMessage_ProfileNotFound() throws Exception {

        UUID xapplicationProfileID = UUID.randomUUID();

        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.empty());

        // request
        mockMvc.perform(
                        post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBaseMessage()))
                                .header(
                                        "X-Application-Profile-ID",
                                        xapplicationProfileID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("X-Application-Profile-ID not found"));
    }

    private CreateMessageModel createBaseMessage() throws Exception {
        String jsonConversation =
                """
        {
                "attachments": [
                    "aaaaaaaa-52e9-41d6-8e71-8582d96ea645",
                    "bbbbbbbb-c7a3-4018-9de8-cac8b0f916c1"
                ],
                "body": "a message here"
        }
        """;

        return objectMapper.readValue(jsonConversation, CreateMessageModel.class);
    }

    private ConversationCreateModel createBaseConversation(String entityType) throws Exception {
        String jsonConversation =
                """
        {
            "entityReference": {
                "entityId": "3a6d539a-d484-4938-93f6-915f4dbe6c41",
                "type": "%s"
            },
            "message": {
                "attachments": [
                    "75011fcd-52e9-41d6-8e71-8582d96ea645",
                    "aee9ce24-c7a3-4018-9de8-cac8b0f916c1"
                ],
                "body": "hi there"
            },
            "subject": "hi"
        }
        """
                        .formatted(entityType);

        return objectMapper.readValue(jsonConversation, ConversationCreateModel.class);
    }

    private MessageSender createSenderForPosting(
            UUID userId,
            ProfileType profileType,
            String displayName,
            String userType,
            UUID profileId) {
        return MessageSender.builder()
                .userId(userId)
                .userType(userType)
                .displayName(displayName)
                .profileType(profileType)
                .profileId(profileId)
                .build();
    }

    private Message createBaseConversationForPosting(MessageSender sender) {
        return Message.builder()
                .id(UUID.randomUUID())
                .body("hi there")
                .sender(sender)
                .attachments(
                        List.of(
                                UUID.fromString("75011fcd-52e9-41d6-8e71-8582d96ea645"),
                                UUID.fromString("aee9ce24-c7a3-4018-9de8-cac8b0f916c1")))
                .build();
    }

    @Test
    void getConversations_Failure_NoProfileInHeader_NotFullyAuthorized() throws Exception {
        when(authorizationHandler.isAllowed(VIEW_CONVERSATION_ACTION, Individual.class))
                .thenReturn(false);

        UUID referenceId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .queryParam("referenceType", TRANSACTION.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Please provide X-Application-Profile-ID header")));
    }

    @Test
    void getConversations_Failure_ProfileInHeader_ProfileNotFound() throws Exception {
        UUID referenceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .header("X-Application-Profile-ID", profileId.toString())
                                .queryParam("referenceType", TRANSACTION.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(
                        content().string(containsString(("X-Application-Profile-ID not found"))));
    }

    @Test
    void getConversations_Success_ProfileInHeader_Transaction_Subject() throws Exception {
        UUID referenceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Individual.builder().build()));

        EntityReference entityReference =
                EntityReference.builder()
                        .entityId(referenceId)
                        .type(TRANSACTION)
                        .id(UUID.randomUUID())
                        .build();
        when(entityReferenceService.findByEntityIdAndEntityType(referenceId, TRANSACTION))
                .thenReturn(List.of(entityReference));

        Transaction transaction = Transaction.builder().subjectProfileId(profileId).build();
        when(transactionService.getTransactionById(referenceId))
                .thenReturn(Optional.of(transaction));

        when(conversationService.getConversationByFilters(any()))
                .thenReturn(createConversationPage());

        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), any(UUID.class));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createMessage()));
        when(messageService.countByConversationId(any())).thenReturn(1);
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createMessage()));
        when(messageService.countByConversationId(any())).thenReturn(1);

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .header("X-Application-Profile-ID", profileId.toString())
                                .queryParam("referenceType", TRANSACTION.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].totalMessages").value(1))
                .andExpect(
                        jsonPath("$.items[0].originalMessage.sender.displayName").value("John Doe"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(1))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].originalMessage").exists());
    }

    @Test
    void getConversations_Success_NoProfileInHeader_Transaction() throws Exception {
        UUID referenceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Individual.builder().build()));

        EntityReference entityReference =
                EntityReference.builder()
                        .entityId(referenceId)
                        .type(TRANSACTION)
                        .id(UUID.randomUUID())
                        .build();
        when(entityReferenceService.findByEntityIdAndEntityType(referenceId, TRANSACTION))
                .thenReturn(List.of(entityReference));

        Transaction transaction = Transaction.builder().subjectProfileId(profileId).build();
        when(transactionService.getTransactionById(referenceId))
                .thenReturn(Optional.of(transaction));

        when(conversationService.getConversationByFilters(any()))
                .thenReturn(createConversationPage());

        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), eq(null));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createMessage()));
        when(messageService.countByConversationId(any())).thenReturn(1);

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .queryParam("referenceType", TRANSACTION.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].totalMessages").value(1))
                .andExpect(
                        jsonPath("$.items[0].originalMessage.sender.displayName").value("John Doe"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(1))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].originalMessage").exists());
    }

    @Test
    void getConversations_Success_ProfileInHeader_Employer() throws Exception {
        UUID referenceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Individual.builder().build()));

        EntityReference entityReference =
                EntityReference.builder()
                        .entityId(referenceId)
                        .type(TRANSACTION)
                        .id(UUID.randomUUID())
                        .build();
        when(entityReferenceService.findByEntityIdAndEntityType(referenceId, EntityType.EMPLOYER))
                .thenReturn(List.of(entityReference));

        when(conversationService.getConversationByFilters(any()))
                .thenReturn(createConversationPage());

        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), any(UUID.class));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createMessage()));
        when(messageService.countByConversationId(any())).thenReturn(1);

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .header("X-Application-Profile-ID", profileId.toString())
                                .queryParam("referenceType", EMPLOYER.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].totalMessages").value(1))
                .andExpect(
                        jsonPath("$.items[0].originalMessage.sender.displayName").value("John Doe"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(1))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].originalMessage").exists());
    }

    @Test
    void getConversations_Forbidden_NoEntityReference() throws Exception {
        UUID referenceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Individual.builder().build()));
        when(entityReferenceService.findByEntityIdAndEntityType(referenceId, TRANSACTION))
                .thenReturn(Collections.emptyList());
        doThrow(
                        new ForbiddenException(
                                "X-Application-Profile-ID header provided is"
                                        + " not associated with the TRANSACTION"
                                        + " entity"))
                .when(entityReferenceService)
                .validateEntityReference(any(), eq(profileId));

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .header("X-Application-Profile-ID", profileId.toString())
                                .queryParam("referenceType", TRANSACTION.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "X-Application-Profile-ID header provided is"
                                                        + " not associated with the TRANSACTION"
                                                        + " entity")));
    }

    @Test
    void getConversations_Success_NoProfileInHeader_Employer() throws Exception {
        UUID referenceId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Individual.builder().build()));

        EntityReference entityReference =
                EntityReference.builder()
                        .entityId(profileId)
                        .type(TRANSACTION)
                        .id(UUID.randomUUID())
                        .build();
        when(entityReferenceService.findByEntityIdAndEntityType(referenceId, EntityType.EMPLOYER))
                .thenReturn(List.of(entityReference));

        when(conversationService.getConversationByFilters(any()))
                .thenReturn(createConversationPage());

        doNothing()
                .when(entityReferenceService)
                .validateEntityReference(any(EntityReference.class), eq(null));
        when(messageService.getOriginalMessageByConversationId(any()))
                .thenReturn(Optional.of(createMessage()));
        when(messageService.countByConversationId(any())).thenReturn(1);

        mockMvc.perform(
                        get("/api/v1/conversations")
                                .queryParam("referenceType", EMPLOYER.getValue())
                                .queryParam("referenceId", referenceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].totalMessages").value(1))
                .andExpect(
                        jsonPath("$.items[0].originalMessage.sender.displayName").value("John Doe"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(1))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].originalMessage").exists());
    }

    @Test
    void getConversation_Success() throws Exception {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = createConversation();
        conversation.setId(conversationId);
        when(conversationService.getConversationById(conversationId))
                .thenReturn(Optional.of(conversation));
        when(messageService.getMessageByFilters(any())).thenReturn(createMessagePage());
        when(transactionService.getTransactionById(conversation.getEntityReference().getEntityId()))
                .thenReturn(Optional.of(Transaction.builder().build()));
        when(messageService.getOriginalMessageByConversationId(conversationId))
                .thenReturn(Optional.of(createMessage()));
        when(messageService.countByConversationId(conversationId)).thenReturn(2);

        mockMvc.perform(get("/api/v1/conversations/" + conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.subject").value("subject"))
                .andExpect(jsonPath("$.totalMessages").value(2))
                .andExpect(jsonPath("$.entityReference.type").value(TRANSACTION.getValue()))
                .andExpect(
                        jsonPath("$.entityReference.entityId")
                                .value(conversation.getEntityReference().getEntityId().toString()))
                .andExpect(jsonPath("$.messages").isNotEmpty())
                .andExpect(jsonPath("$.messages[0]").exists())
                .andExpect(jsonPath("$.messages[1]").exists())
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(2))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(2))
                .andExpect(jsonPath("$.originalMessage").exists());
    }

    @Test
    void getConversation_ConversationNotFound() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.getConversationById(conversationId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/conversations/" + conversationId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Conversation not found")));
    }

    private Conversation createConversation() {
        EntityReference entityReference = new EntityReference();
        entityReference.setEntityId(UUID.randomUUID());
        entityReference.setType(TRANSACTION);
        Conversation conversation = new Conversation();
        conversation.setSubject("subject");
        conversation.setEntityReference(entityReference);
        conversation.setReplies(List.of(new Message()));
        return conversation;
    }

    private Message createMessage() {
        Conversation conversation = new Conversation();

        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setSender(createMessageSender());
        message.setConversation(conversation);
        message.setBody("Body");
        message.setAttachments(Arrays.asList(UUID.randomUUID()));
        message.setOriginalMessage(true);
        message.setTimestamp(OffsetDateTime.now());

        return message;
    }

    private MessageSender createMessageSender() {
        MessageSender messageSender = new MessageSender();

        messageSender.setId(UUID.randomUUID());
        messageSender.setUserId(UUID.randomUUID());
        messageSender.setDisplayName("John Doe");
        messageSender.setUserType("public");
        messageSender.setProfileId(UUID.randomUUID());
        messageSender.setProfileType(ProfileType.INDIVIDUAL);

        return messageSender;
    }

    private Page<Conversation> createConversationPage() {
        Conversation conversation = createConversation();
        List<Message> messages = List.of(createMessage());
        conversation.setReplies(messages);
        List<Conversation> conversations = Arrays.asList(conversation);
        return new PageImpl<>(conversations);
    }

    private Page<Message> createMessagePage() {
        List<Message> messages = List.of(createMessage(), createMessage());
        return new PageImpl<>(messages);
    }
}
