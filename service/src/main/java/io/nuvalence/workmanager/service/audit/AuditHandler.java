package io.nuvalence.workmanager.service.audit;

import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;

/**
 * Implementation of a specific audit event type.
 *
 * @param <S> Type of subject this event audits.
 */
public interface AuditHandler<S extends UpdateTrackedEntity> {
    void handlePreUpdateState(S subject);

    void handlePostUpdateState(S subject);

    void publishAuditEvent(String originatorId);
}
