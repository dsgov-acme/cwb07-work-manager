package io.nuvalence.workmanager.service.utils.api;

import io.nuvalence.logging.filter.LoggingContextFilter;
import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.service.utils.ServiceTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

/**
 * Provides interceptors with common needed settings for ApiClients.
 */
@Component
@RequiredArgsConstructor
public class ApiClientCommonInterceptorProvider {

    private final ServiceTokenProvider serviceTokenProvider;

    /**
     * Returns a request interceptor that adds the service token and correlation id to the request.
     *
     *
     * @param parentInterceptor the ApiClient parent interceptor
     *
     * @return The configured interceptor
     */
    public Consumer<HttpRequest.Builder> getRequestInterceptor(
            Consumer<HttpRequest.Builder> parentInterceptor) {
        return builder -> {
            if (parentInterceptor != null) {
                parentInterceptor.accept(builder);
            }

            builder.header(
                    HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken());
            builder.header(
                    LoggingContextFilter.HEADER_CORRELATION_ID,
                    CorrelationIdContext.getCorrelationId());
        };
    }
}
