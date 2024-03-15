package io.nuvalence.workmanager.service.usermanagementapi;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserPageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;

/**
 * Client to interface with User Management API.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserManagementClient implements UserManagementService {

    @Value("${userManagement.baseUrl}")
    private String baseUrl;

    private final RestTemplate httpClient;

    @Override
    public Optional<User> getUserOptional(UUID userId) {
        final String url = String.format("%s/api/v1/users/%s", baseUrl, userId.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> payload = new HttpEntity<>(headers);
        ResponseEntity<User> response = null;
        try {
            response = httpClient.exchange(url, HttpMethod.GET, payload, User.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to get user from user management service", e);
            if (NOT_FOUND == e.getStatusCode()) {
                return Optional.empty();
            }
            throw e;
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ApiException("Failed to upload roles: " + response.getStatusCode());
        }
        return Optional.ofNullable(response.getBody());
    }

    @Override
    public List<UserDTO> getUsers(String name, String email) {
        List<UserDTO> allUsers = new ArrayList<>();

        String queryParameters =
                Stream.of(
                                Optional.ofNullable(name)
                                        .map(
                                                n ->
                                                        "name="
                                                                + URLEncoder.encode(
                                                                        n, StandardCharsets.UTF_8))
                                        .orElse(""),
                                Optional.ofNullable(email)
                                        .map(
                                                e ->
                                                        "email="
                                                                + URLEncoder.encode(
                                                                        e, StandardCharsets.UTF_8))
                                        .orElse(""))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("&"));

        if (queryParameters.isEmpty()) {
            throw new IllegalArgumentException("Both name and email cannot be empty");
        }

        String nextPageUrl = baseUrl + "/api/v1/users/?" + queryParameters;

        while (nextPageUrl != null && !nextPageUrl.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> payload = new HttpEntity<>(headers);

            ResponseEntity<UserPageDTO> response;
            try {
                URI uri = new URI(nextPageUrl);
                response = httpClient.exchange(uri, HttpMethod.GET, payload, UserPageDTO.class);
            } catch (HttpClientErrorException e) {
                log.error("Failed to get user from user management service", e);
                if (NOT_FOUND.equals(e.getStatusCode())) {
                    break;
                }
                throw e;
            } catch (URISyntaxException e) {
                throw new ApiException("Failed to get users: " + e.getMessage());
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ApiException("Failed to get users: " + response.getStatusCode());
            }

            UserPageDTO userPage = response.getBody();
            if (userPage != null && userPage.getUsers() != null) {
                allUsers.addAll(userPage.getUsers());
                nextPageUrl = userPage.getPagingMetadata().getNextPage();
            } else {
                nextPageUrl = "";
            }
        }

        return allUsers;
    }

    @Override
    public User getUser(UUID userId) {
        try {
            Optional<User> optionalUser = getUserOptional(userId);
            if (optionalUser.isEmpty()) {
                throw new NotFoundException("User not found");
            }
            return optionalUser.get();
        } catch (HttpClientErrorException e) {
            log.error("An error reaching user management occurred: ", e);
            throw new UnexpectedException("Could not verify user existence");
        }
    }
}
