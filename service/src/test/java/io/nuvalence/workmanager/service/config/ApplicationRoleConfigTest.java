package io.nuvalence.workmanager.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.RoleReportingEvent;
import io.nuvalence.events.event.service.EventGateway;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ApplicationRoleConfigTest {

    @Mock private EventGateway mockEventGateway;
    @Mock private PublisherProperties mockPublisherProperties;
    private ApplicationRoleConfig applicationRoleConfig;

    private static final String TOPIC_KEY = "APPLICATION_ROLE_REPORTING";

    @BeforeEach
    void setUp() {
        openMocks(this);
        applicationRoleConfig =
                new ApplicationRoleConfig(mockEventGateway, mockPublisherProperties);
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testPublishRoles_NotConfiguredTopic(CapturedOutput output) {

        when(mockPublisherProperties.getFullyQualifiedTopicName(TOPIC_KEY))
                .thenReturn(Optional.empty());

        // test
        applicationRoleConfig.publishRoles();

        verify(mockEventGateway, never()).publishEvent(any(), anyString());

        assertTrue(
                output.getOut()
                        .lines()
                        .anyMatch(
                                line ->
                                        line.contains("ERROR")
                                                && line.contains(
                                                        "Roles reporting topic not configured,"
                                                                + " topic name: "
                                                                + TOPIC_KEY)));
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void testPublishRoles_Success() {

        String topicName = "projects/dsgov-demo/topics/THE_CONFIGURED_TOPIC";
        when(mockPublisherProperties.getFullyQualifiedTopicName(TOPIC_KEY))
                .thenReturn(Optional.of(topicName));

        OffsetDateTime before = OffsetDateTime.now();

        ArgumentCaptor<RoleReportingEvent> eventCaptor =
                ArgumentCaptor.forClass(RoleReportingEvent.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        // test
        applicationRoleConfig.publishRoles();

        verify(mockEventGateway).publishEvent(eventCaptor.capture(), topicCaptor.capture());

        assertEquals(topicName, topicCaptor.getValue());
        var capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);

        assertTrue(capturedEvent.getRoles().size() > 12);

        assertEquals("work-manager", capturedEvent.getMetadata().getOriginatorId());
        assertEquals("RoleReportingEvent", capturedEvent.getMetadata().getType());
        assertTrue(StringUtils.isNotBlank(capturedEvent.getMetadata().getCorrelationId()));

        assertEquals(
                "wm:transaction-submitter", capturedEvent.getRoles().get(0).getApplicationRole());
        assertEquals("wm:transaction-admin", capturedEvent.getRoles().get(1).getApplicationRole());

        assertEquals(3, capturedEvent.getRoles().get(1).getCapabilities().size());
        assertEquals(
                "transaction-management-read",
                capturedEvent.getRoles().get(1).getCapabilities().get(0));
        assertEquals(
                "transaction-management-write",
                capturedEvent.getRoles().get(1).getCapabilities().get(1));
        assertEquals(
                "transaction-conversation-create",
                capturedEvent.getRoles().get(1).getCapabilities().get(2));

        assertEquals(
                "wm:transaction-config-admin",
                capturedEvent.getRoles().get(2).getApplicationRole());

        assertEquals(4, capturedEvent.getRoles().get(2).getCapabilities().size());
        assertEquals("admin-console", capturedEvent.getRoles().get(2).getCapabilities().get(0));
        assertEquals(
                "transaction-config", capturedEvent.getRoles().get(2).getCapabilities().get(1));
        assertEquals(
                "transaction-set-config", capturedEvent.getRoles().get(2).getCapabilities().get(2));
        assertEquals("schema-config", capturedEvent.getRoles().get(2).getCapabilities().get(3));

        OffsetDateTime after = OffsetDateTime.now();
        var metadataTime = capturedEvent.getMetadata().getTimestamp();

        assertTrue(
                (metadataTime.isAfter(before) || metadataTime.isEqual(before))
                        && (metadataTime.isBefore(after) || metadataTime.isEqual(after)));
    }
}
