package io.nuvalence.workmanager.service.utils.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.service.utils.ServiceTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.function.Consumer;

class ApiClientCommonInterceptorProviderTest {

    @Mock private ServiceTokenProvider serviceTokenProvider;
    @Mock private Consumer<HttpRequest.Builder> parentInterceptor;
    private ApiClientCommonInterceptorProvider interceptorProvider;

    @BeforeEach
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        interceptorProvider = new ApiClientCommonInterceptorProvider(serviceTokenProvider);
    }

    @Test
    void testGetRequestInterceptor() {

        String mockToken = "mockToken";
        String mockCorrelationId = "mockCorrelationId";

        when(serviceTokenProvider.getServiceToken()).thenReturn(mockToken);
        CorrelationIdContext.setCorrelationId(mockCorrelationId);

        // test
        var interceptor = interceptorProvider.getRequestInterceptor(parentInterceptor);
        var testerBuilder = HttpRequest.newBuilder();
        interceptor.accept(testerBuilder);

        var headers = testerBuilder.uri(URI.create("http://localhost")).build().headers();

        // verify
        assertEquals("Bearer mockToken", headers.firstValue(HttpHeaders.AUTHORIZATION).get());
        assertEquals(mockCorrelationId, headers.firstValue("X-Correlation-Id").get());
        verify(parentInterceptor).accept(testerBuilder);
    }
}
