package io.nuvalence.workmanager.service.usermanagementapi;

import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.usermanagementapi.models.UserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client to interface with User Management API.
 */
public interface UserManagementService {

    /**
     * Get user optional.
     *
     * @param userId id of the user.
     * @return User.
     * @throws ApiException for possible errors reaching user management service.
     */
    Optional<User> getUserOptional(UUID userId);

    /**
     * Get user.
     *
     * @param userId id of the user.
     * @return User.
     */
    User getUser(UUID userId);

    List<UserDTO> getUsers(String name, String email);
}
