package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Profile;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageSender;
import io.nuvalence.workmanager.service.models.ConversationFilters;
import io.nuvalence.workmanager.service.repository.ConversationRepository;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository repository;

    public Conversation saveConversation(final Conversation conversation) {
        return repository.save(conversation);
    }

    public Page<Conversation> getConversationByFilters(final ConversationFilters filters) {
        return repository.findAll(filters.getConversationByFilters(), filters.getPageRequest());
    }

    public Optional<Conversation> getConversationById(UUID conversationId) {
        return repository.findById(conversationId);
    }

    /**
     * Creates a MessageSender object from the current user and associated profile.
     * IMPORTANT: It is responsibility of the clients of this method to ensure that the
     * current user is actually associated to the profile provided. 
     * This is anyway a utility method, and doesn't do any storage of returned information.
     * 
     * @param associatedProfile profile to associate with the sender
     * @return
     */
    public MessageSender createSenderFromCurrentUser(Profile associatedProfile) {

        String userType =
                CurrentUserUtility.getCurrentUser().map(UserToken::getUserType).orElse(null);

        return MessageSender.builder()
                .userId(UUID.fromString(SecurityContextUtility.getAuthenticatedUserId()))
                .displayName(SecurityContextUtility.getAuthenticatedUserName())
                .userType(userType)
                .profileType(associatedProfile != null ? associatedProfile.getProfileType() : null)
                .profileId(associatedProfile != null ? associatedProfile.getId() : null)
                .build();
    }
}
