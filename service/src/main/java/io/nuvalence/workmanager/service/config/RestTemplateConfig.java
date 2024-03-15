package io.nuvalence.workmanager.service.config;

import io.nuvalence.logging.filter.LoggingContextFilter;
import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.service.utils.ServiceTokenProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Configuration for RestTemplates.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Provides prototype scoped http clients (RestTemplate) with a common needed interceptor for service token and
     * correlation id headers handling.
     *
     * @param serviceTokenProvider service token provider
     *
     * @return configured http client
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public RestTemplate httpClient(ServiceTokenProvider serviceTokenProvider) {

        RestTemplate httpClient = new RestTemplate();
        httpClient.setInterceptors(
                List.of(
                        (request, body, execution) -> {
                            var headers = request.getHeaders();
                            headers.setBearerAuth(serviceTokenProvider.getServiceToken());
                            headers.add(
                                    LoggingContextFilter.HEADER_CORRELATION_ID,
                                    CorrelationIdContext.getCorrelationId());

                            return execution.execute(request, body);
                        }));

        return httpClient;
    }
}
