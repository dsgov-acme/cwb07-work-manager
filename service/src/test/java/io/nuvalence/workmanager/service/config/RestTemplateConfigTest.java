package io.nuvalence.workmanager.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.service.utils.ServiceTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.web.client.RestTemplate;

class RestTemplateConfigTest {

    byte[] mockBody = new byte[] {};
    @Mock private ServiceTokenProvider serviceTokenProvider;
    @Mock private HttpRequest mockRequest;
    @Mock private ClientHttpRequestExecution mockExecution;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHttpClient_interceptorConfig() throws Exception {

        String mockToken = "mockToken";
        String mockCorrelationId = "mockCorrelationId";

        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        when(serviceTokenProvider.getServiceToken()).thenReturn(mockToken);
        CorrelationIdContext.setCorrelationId(mockCorrelationId);

        // test
        RestTemplate restTemplate = new RestTemplateConfig().httpClient(serviceTokenProvider);
        var interceptors = restTemplate.getInterceptors();

        interceptors.get(0).intercept(mockRequest, mockBody, mockExecution);

        // verify
        assertEquals("Bearer " + mockToken, mockRequest.getHeaders().getFirst("Authorization"));
        assertEquals(mockCorrelationId, mockRequest.getHeaders().getFirst("X-Correlation-Id"));
    }
}
