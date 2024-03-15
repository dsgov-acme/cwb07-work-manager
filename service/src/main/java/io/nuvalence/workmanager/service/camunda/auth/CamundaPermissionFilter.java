package io.nuvalence.workmanager.service.camunda.auth;

import io.nuvalence.auth.access.AuthorizationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom that filter chain that checks if the provided user has permission to edit Camunda workflows.
 */
@RequiredArgsConstructor
@Slf4j
public class CamundaPermissionFilter extends OncePerRequestFilter {
    private final AuthorizationHandler authorizationHandler;
    private final String namespace;
    private final String camundaPathPattern;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {

        if (pathMatcher.match("/" + namespace + camundaPathPattern, request.getRequestURI())
                && (SecurityContextHolder.getContext().getAuthentication() != null
                        && !authorizationHandler.isAllowed("edit", "workflow"))) {
            log.warn("Unauthorized request to camunda REST API");
            SecurityContextHolder.getContext().setAuthentication(null);
        }
        filterChain.doFilter(request, response);
    }
}
