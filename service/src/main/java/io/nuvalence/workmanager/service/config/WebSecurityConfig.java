package io.nuvalence.workmanager.service.config;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.access.cerbos.CerbosAuthorizationHandler;
import io.nuvalence.auth.token.SelfSignedTokenAuthenticationProvider;
import io.nuvalence.auth.token.TokenFilter;
import io.nuvalence.auth.token.firebase.FirebaseAuthenticationProvider;
import io.nuvalence.auth.util.RsaKeyUtility;
import io.nuvalence.auth.util.TrailingSlashRedirectingFilter;
import io.nuvalence.logging.filter.LoggingContextFilter;
import io.nuvalence.workmanager.service.camunda.auth.CamundaPermissionFilter;
import io.nuvalence.workmanager.service.utils.JacocoIgnoreInGeneratedReport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.List;
import javax.annotation.PostConstruct;

/**
 * Configures TokenFilter.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!test")
@JacocoIgnoreInGeneratedReport(
        reason =
                "Initialization has side effects making unit tests difficult. Tested in acceptance"
                        + " tests.")
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class WebSecurityConfig {
    @Value("${spring.cloud.gcp.project-id}")
    private String gcpProjectId;

    @Value("${management.endpoints.web.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${management.endpoints.web.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${management.endpoints.web.cors.allowed-headers}")
    private List<String> allowedHeaders;

    @Value("${management.endpoints.web.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${auth.token-filter.self-signed.issuer}")
    private String selfSignIssuer;

    @Value("${auth.token-filter.self-signed.public-key}")
    private String selfSignPublicKey;

    @Value("${cerbos.uri}")
    private String cerbosUri;

    private static final String NAMESPACE = "wm";

    private AuthorizationHandler authorizationHandler;

    @PostConstruct
    public void init() throws CerbosClientBuilder.InvalidClientConfigurationException {
        final CerbosBlockingClient cerbosClient =
                new CerbosClientBuilder(cerbosUri).withPlaintext().buildBlockingClient();

        this.authorizationHandler = new CerbosAuthorizationHandler(cerbosClient);
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    /**
     * Allows unauthenticated access to API docs.
     *
     * @param http Spring HttpSecurity configuration.
     * @return Configured SecurityFilterChain
     * @throws Exception If any erroes occur during configuration
     */
    @Bean
    @Order(0)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(CsrfConfigurer::disable)
                .requestCache(requestCache -> requestCache.requestCache(new NullRequestCache()))
                .securityContext(SecurityContextConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                mvc.pattern("/"),
                                                mvc.pattern("/swagger-ui.html"),
                                                mvc.pattern("/swagger-ui/**"),
                                mvc.pattern("/v3/api-docs/**"),
                                mvc.pattern("/actuator/health")).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new TrailingSlashRedirectingFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new LoggingContextFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(
                        new TokenFilter(
                                new FirebaseAuthenticationProvider(gcpProjectId, NAMESPACE),
                                new SelfSignedTokenAuthenticationProvider(
                                        selfSignIssuer,
                                        RsaKeyUtility.getPublicKeyFromString(selfSignPublicKey),
                                        NAMESPACE
                                )
                        ),
                        LoggingContextFilter.class
                )
                .addFilterAfter(
                        new CamundaPermissionFilter(authorizationHandler, NAMESPACE, "/engine-rest/**"),
                        TokenFilter.class
                )
                .build();
    }


    /**
     * Provides configurer that sets up CORS.
     *
     * @return a cors configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // Make the below setting as * to allow connection from any hos
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        corsConfiguration.setAllowedMethods(allowedMethods);
        corsConfiguration.setAllowCredentials(allowCredentials);
        corsConfiguration.setAllowedHeaders(allowedHeaders);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    /**
     * Provides configurer that sets up CORS.
     *
     * @return a configured configurer
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new CorsWebMvcConfigurer();
    }

    @JacocoIgnoreInGeneratedReport(reason = "Simple config class.")
    private class CorsWebMvcConfigurer implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins(allowedOrigins.toArray(String[]::new))
                    .allowedHeaders(allowedHeaders.toArray(String[]::new))
                    .allowedMethods(allowedMethods.toArray(String[]::new))
                    .allowCredentials(allowCredentials);
        }
    }
}
