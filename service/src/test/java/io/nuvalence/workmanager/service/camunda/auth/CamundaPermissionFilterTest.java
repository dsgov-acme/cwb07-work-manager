package io.nuvalence.workmanager.service.camunda.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.access.AuthorizationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class CamundaPermissionFilterTest {
    @Mock private AuthorizationHandler authorizationHandler;

    private CamundaPermissionFilter permissionFilter;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        permissionFilter =
                new CamundaPermissionFilter(authorizationHandler, "namespace", "/camunda/**");
    }

    @Test
    void testDoFilterInternal_WithValidRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/namespace/camunda/processes");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        permissionFilter.doFilterInternal(request, response, filterChain);

        // Assert that the filter chain was called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithUnauthorizedRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/namespace/camunda/processes");

        // Configure the authorization handler to return false (not allowed)
        when(authorizationHandler.isAllowed("edit", "workflow")).thenReturn(false);

        // Simulate an authenticated user
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        permissionFilter.doFilterInternal(request, response, filterChain);

        // Assert that the authentication was cleared
        verify(securityContext, times(1)).setAuthentication(null);
    }
}
