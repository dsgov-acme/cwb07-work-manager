package io.nuvalence.workmanager.service.service;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.DirectNotificationEvent;
import io.nuvalence.events.event.Event;
import io.nuvalence.events.event.NotificationEvent;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.events.EventFactory;
import io.nuvalence.workmanager.service.events.PublisherTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

/**
 * Manages the communication with notification service.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SendNotificationService {

    private static final String PORTAL_URL = "portal-url";

    private final PublisherProperties publisherProperties;
    private final EventGateway eventGateway;

    @Value("${dashboard.url}")
    private String dashboardUrl;

    @Value("${invitation.individual.claim.url}")
    private String individualProfileClaimUrl;

    @Value("${invitation.employer.claim.url}")
    private String employerProfileClaimUrl;

    private Map<String, String> createNotificationParameterMap(
            Map<String, String> camundaPropertyMap, Transaction transaction) {
        Map<String, String> propertyMap = new HashMap<>();
        camundaPropertyMap.forEach(
                (key, value) -> {
                    try {
                        if (key.equals(PORTAL_URL)) {
                            propertyMap.put(PORTAL_URL, dashboardUrl);
                        } else {
                            propertyMap.put(
                                    key, PropertyUtils.getProperty(transaction, value).toString());
                        }
                    } catch (Exception e) {
                        log.error("Error occurred getting value for property", e);
                    }
                });

        return propertyMap;
    }

    /**
     * Sends a notification request.
     *
     * @param transaction Transaction whose notification is to be sent.
     * @param notificationKey Key for the message template.
     * @param properties Properties to complete message template.
     */
    public void sendTransactionNotification(
            Transaction transaction, String notificationKey, Map<String, String> properties) {

        NotificationEvent notificationEvent =
                EventFactory.createNotificationEvent(
                        UUID.fromString(transaction.getSubjectUserId()),
                        notificationKey,
                        createNotificationParameterMap(properties, transaction));

        sendNotification(notificationEvent);
    }

    /**
     * It sends a direct notification request.
     *
     * @param profileInvitation Profile invitation.
     * @param profileDisplayName Profile display name.
     * @param templateKey Key for the message template.
     */
    public void sendDirectNotification(
            ProfileInvitation profileInvitation, String profileDisplayName, String templateKey) {

        String claimUrl =
                profileInvitation.getType().equals(ProfileType.INDIVIDUAL)
                        ? individualProfileClaimUrl
                        : employerProfileClaimUrl;

        Map<String, String> properties =
                new HashMap<>(
                        Map.ofEntries(
                                Map.entry(PORTAL_URL, claimUrl + "/" + profileInvitation.getId()),
                                Map.entry("profile-display-name", profileDisplayName),
                                Map.entry("invitation-id", profileInvitation.getId().toString())));

        DirectNotificationEvent notificationEvent =
                EventFactory.createDirectNotificationEvent(
                        "email", profileInvitation.getEmail(), templateKey, properties);

        sendNotification(notificationEvent);
    }

    private void sendNotification(Event notificationEvent) {
        Optional<String> fullyQualifiedTopicNameOptional =
                publisherProperties.getFullyQualifiedTopicName(
                        PublisherTopic.NOTIFICATION_REQUESTS.name());

        if (fullyQualifiedTopicNameOptional.isEmpty()) {
            throw new NotFoundException(
                    "Notification requests topic not found, topic name: "
                            + PublisherTopic.NOTIFICATION_REQUESTS.name());
        }

        eventGateway.publishEvent(notificationEvent, fullyQualifiedTopicNameOptional.get());
    }
}
