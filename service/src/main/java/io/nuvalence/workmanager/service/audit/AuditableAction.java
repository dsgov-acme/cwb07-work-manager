package io.nuvalence.workmanager.service.audit;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Action wrapper that handles updating last updated parameters and recording configured audit events.
 *
 * @param <S> Subject type of action
 */
@Value
public final class AuditableAction<S extends UpdateTrackedEntity> {
    @NonNull private final AuditableActionFunction<S> action;

    private final List<AuditHandler<S>> auditHandlers;

    private final RequestContextTimestamp requestContextTimestamp;

    AuditableAction(
            @NonNull AuditableActionFunction<S> action,
            List<AuditHandler<S>> auditHandlers,
            RequestContextTimestamp requestContextTimestamp) {
        this.action = action;
        this.auditHandlers = auditHandlers;
        this.requestContextTimestamp = requestContextTimestamp;
    }

    /**
     * Executes the configured action and records configured audit events.
     *
     * @param subject subject of action
     * @return updated subject
     * @throws Exception If action function throws an exception
     */
    public S execute(final S subject) throws Exception {
        final String originatorId = SecurityContextUtility.getAuthenticatedUserId();
        final var updateTime = requestContextTimestamp.getCurrentTimestamp();

        auditHandlers.forEach(handler -> handler.handlePreUpdateState(subject));
        final var result = action.execute(subject);
        result.setLastUpdatedTimestamp(updateTime);
        result.setLastUpdatedBy(UserUtility.getCurrentApplicationUserId().orElse(null));
        auditHandlers.forEach(handler -> handler.handlePostUpdateState(result));
        auditHandlers.forEach(handler -> handler.publishAuditEvent(originatorId));

        return result;
    }

    /**
     * Returns a typed builder to create a new AuditableAction.
     *
     * @param type Subject type of AuditableAction to build
     * @param <S> Subject type of AuditableAction to build
     * @return new typed builder
     */
    public static <S extends UpdateTrackedEntity> AuditableActionBuilder<S> builder(
            final Class<S> type) {
        return new AuditableActionBuilder<>(type);
    }

    /**
     * Builds a new instance of an AuditableAction.
     *
     * @param <S> Subject type of AuditableAction to build
     */
    @ToString
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static class AuditableActionBuilder<S extends UpdateTrackedEntity> {
        private AuditableActionFunction<S> action;
        private ArrayList<AuditHandler<S>> auditHandlers;
        private RequestContextTimestamp requestContextTimestamp;
        private Class<S> type;

        AuditableActionBuilder(Class<S> type) {
            this.type = type;
        }

        /**
         * Function executed by the AuditableAction to build.
         *
         * @param action action as AuditableActionFunction
         * @return this builder
         */
        public AuditableActionBuilder<S> action(@NonNull AuditableActionFunction<S> action) {
            this.action = action;
            return this;
        }

        /**
         * Adds an AuditHandler for the AuditableAction to build.
         *
         * @param auditHandler AuditHandler
         * @return this builder
         */
        public AuditableActionBuilder<S> auditHandler(AuditHandler<S> auditHandler) {
            if (this.auditHandlers == null) {
                this.auditHandlers = new ArrayList<>();
            }
            this.auditHandlers.add(auditHandler);
            return this;
        }

        /**
         * Sets the list of AuditHandlers for the AuditableAction to build.
         *
         * @param auditHandlers list of AuditHandlers
         * @return this builder
         */
        public AuditableActionBuilder<S> auditHandlers(
                Collection<? extends AuditHandler<S>> auditHandlers) {
            if (this.auditHandlers == null) {
                this.auditHandlers = new ArrayList<>();
            }
            this.auditHandlers.addAll(auditHandlers);
            return this;
        }

        /**
         * Clears internal list of AuditHandlers in this builder.
         *
         * @return this builder
         */
        public AuditableActionBuilder<S> clearAuditHandlers() {
            if (this.auditHandlers != null) {
                this.auditHandlers.clear();
            }
            return this;
        }

        public AuditableActionBuilder<S> requestContextTimestamp(
                RequestContextTimestamp requestContextTimestamp) {
            this.requestContextTimestamp = requestContextTimestamp;
            return this;
        }

        /**
         * Builds a new AuditableAction based on builder inputs.
         *
         * @return newly constructed AuditableAction
         */
        public AuditableAction<S> build() {
            return new AuditableAction<>(
                    action,
                    auditHandlers == null ? Collections.emptyList() : List.copyOf(auditHandlers),
                    requestContextTimestamp);
        }
    }
}
