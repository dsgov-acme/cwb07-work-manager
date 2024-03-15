package io.nuvalence.workmanager.service.domain;

import java.time.OffsetDateTime;

/**
 * Defines common attributes for tracking created by and last updated users and timestamps.
 */
public interface UpdateTrackedEntity {
    String getCreatedBy();

    String getLastUpdatedBy();

    OffsetDateTime getCreatedTimestamp();

    OffsetDateTime getLastUpdatedTimestamp();

    void setCreatedBy(String createdBy);

    void setLastUpdatedBy(String lastUpdatedBy);

    void setCreatedTimestamp(OffsetDateTime createdTimestamp);

    void setLastUpdatedTimestamp(OffsetDateTime lastUpdatedTimestamp);
}
