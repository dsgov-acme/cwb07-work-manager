package io.nuvalence.workmanager.service.usermanagementapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserPageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

class UserManagementClientTest {

    private UserManagementClient userManagementClient;

    @Mock private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userManagementClient = new UserManagementClient(restTemplate);
    }

    @Test
    void getUser_succesfulrequest() {
        UUID userId = UUID.randomUUID();
        User user =
                User.builder()
                        .id(userId)
                        .displayName("Federico Garcia")
                        .email("federico@nobody.com")
                        .build();
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class)))
                .thenReturn(new ResponseEntity<>(user, HttpStatus.OK));

        Optional<User> returnedUser = userManagementClient.getUserOptional(userId);

        // Assert
        assertTrue(returnedUser.isPresent());
        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class));
    }

    @Test
    void getUser_notfound() {
        UUID userId = UUID.randomUUID();
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        Optional<User> returnedUser = userManagementClient.getUserOptional(userId);

        // Assert
        assertTrue(returnedUser.isEmpty());
        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class));
    }

    @Test
    void getUsers_successfulRequest() {
        // Prepare test data
        String name = "John Doe";
        String email = "john.doe@example.com";
        UserPageDTO userPageDTO = new UserPageDTO(); // Assume you have a proper UserPageDTO object

        // Mock the exchange method to return a successful response
        when(restTemplate.exchange(
                        any(URI.class),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(UserPageDTO.class)))
                .thenReturn(new ResponseEntity<>(userPageDTO, HttpStatus.OK));

        List<UserDTO> returnedUserPage = userManagementClient.getUsers(name, email);

        // Assert
        assertTrue(returnedUserPage.isEmpty());
        verify(restTemplate, times(1))
                .exchange(
                        any(URI.class),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(UserPageDTO.class));
    }

    @Test
    void getUsers_notFound() {
        // Prepare test data
        String name = "John Doe";
        String email = "john.doe@example.com";

        // Mock the exchange method to throw a NOT_FOUND exception
        when(restTemplate.exchange(
                        any(URI.class),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(UserPageDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        List<UserDTO> returnedUserPage = userManagementClient.getUsers(name, email);

        // Assert
        assertTrue(returnedUserPage.isEmpty());
        verify(restTemplate, times(1))
                .exchange(
                        any(URI.class),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(UserPageDTO.class));
    }

    @Test
    void getUser_userExists() {
        UUID userId = UUID.randomUUID();
        User user =
                User.builder()
                        .id(userId)
                        .displayName("Federico Garcia")
                        .email("federico@nobody.com")
                        .build();

        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class)))
                .thenReturn(new ResponseEntity<>(user, HttpStatus.OK));

        User result = userManagementClient.getUser(userId);

        assertEquals(user, result);
        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class));
    }

    @Test
    void getUser_userNotFound() {
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(
                NotFoundException.class,
                () -> {
                    userManagementClient.getUser(userId);
                });
    }

    @Test
    void getUser_errorCallingUserService() {
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(User.class)))
                .thenThrow(HttpClientErrorException.class);

        assertThrows(
                UnexpectedException.class,
                () -> {
                    userManagementClient.getUser(userId);
                });
    }
}
