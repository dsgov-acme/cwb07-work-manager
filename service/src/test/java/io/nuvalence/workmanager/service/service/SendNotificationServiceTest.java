package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

class SendNotificationServiceTest {

    @Mock private PublisherProperties publisherProperties;

    @Mock private EventGateway eventGateway;

    @InjectMocks private SendNotificationService sendNotificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendNotification_shouldPublishEvent() {
        Map<String, String> properties = new HashMap<>();
        properties.put("transactionId", "id");
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .subjectUserId(UUID.randomUUID().toString())
                        .build();

        when(publisherProperties.getFullyQualifiedTopicName(any()))
                .thenReturn(Optional.of("notification-topic"));

        sendNotificationService.sendTransactionNotification(transaction, "templateKey", properties);

        verify(eventGateway, times(1)).publishEvent(any(), eq("notification-topic"));
    }

    @Test
    void sendNotification_shouldThrowNotFoundException_whenTopicNotFound() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .subjectUserId(UUID.randomUUID().toString())
                        .build();

        Map<String, String> properties = Collections.singletonMap("key", "value");

        when(publisherProperties.getFullyQualifiedTopicName(any())).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () ->
                        sendNotificationService.sendTransactionNotification(
                                transaction, "templateKey", properties));
    }

    @Test
    void sendDirectNotification_shouldPublishEvent() {

        ProfileInvitation profileInvitation = new ProfileInvitation();
        profileInvitation.setId(UUID.randomUUID());
        profileInvitation.setEmail("test@example.com");
        profileInvitation.setType(ProfileType.INDIVIDUAL);

        when(publisherProperties.getFullyQualifiedTopicName(any()))
                .thenReturn(Optional.of("notification-topic"));

        sendNotificationService.sendDirectNotification(profileInvitation, "Test User", "template");

        verify(eventGateway, times(1)).publishEvent(any(), eq("notification-topic"));
    }

    @Test
    void sendDirectNotification_shouldThrowNotFoundException_whenTopicNotFound() {

        ProfileInvitation profileInvitation = new ProfileInvitation();
        profileInvitation.setId(UUID.randomUUID());
        profileInvitation.setEmail("test@example.com");
        profileInvitation.setType(ProfileType.INDIVIDUAL);

        when(publisherProperties.getFullyQualifiedTopicName(any())).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () ->
                        sendNotificationService.sendDirectNotification(
                                profileInvitation, "Test User", "template"));
    }
}
