package io.nuvalence.workmanager.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.RoleReportingEvent;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.events.EventFactory;
import io.nuvalence.workmanager.service.events.PublisherTopic;
import io.nuvalence.workmanager.service.usermanagementapi.models.ApplicationRoles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

/**
 * ApplicationRoleConfig class.
 */
@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class ApplicationRoleConfig {

    private static final String CONFIG = "roles.json";
    private final EventGateway eventGateway;
    private final PublisherProperties publisherProperties;

    /**
     * Publishes roles to User Management from JSON file.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publishRoles() {
        try {
            final Resource rolesResource = new ClassPathResource(CONFIG);

            if (!rolesResource.exists()) {
                throw new UnexpectedException("Role configuration file does not exist.");
            }

            try (final InputStream fileStream = rolesResource.getInputStream()) {
                ApplicationRoles roles =
                        new ObjectMapper().readValue(fileStream, ApplicationRoles.class);

                RoleReportingEvent event = EventFactory.createRoleReportingEvent(roles);

                Optional<String> fullyQualifiedTopicNameOptional =
                        publisherProperties.getFullyQualifiedTopicName(
                                PublisherTopic.APPLICATION_ROLE_REPORTING.name());

                if (!fullyQualifiedTopicNameOptional.isPresent()) {
                    throw new UnexpectedException(
                            "Roles reporting topic not configured, topic name: "
                                    + PublisherTopic.APPLICATION_ROLE_REPORTING.name());
                }

                eventGateway.publishEvent(event, fullyQualifiedTopicNameOptional.get());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
