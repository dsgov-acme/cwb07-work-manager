package io.nuvalence.workmanager.service.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Request scoped bean that provides a common timestamp for all uses performed within a request.
 */
@Component
@RequestScope
public class RequestContextTimestamp {
    private final OffsetDateTime currentTimestamp;

    public RequestContextTimestamp() {
        this.currentTimestamp = OffsetDateTime.now(Clock.systemUTC());
    }

    public OffsetDateTime getCurrentTimestamp() {
        return currentTimestamp;
    }
}
