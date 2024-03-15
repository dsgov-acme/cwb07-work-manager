package io.nuvalence.workmanager.service.utils.auth;

import io.nuvalence.auth.token.UserToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility for grabbing info about the current user.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CurrentUserUtility {

    /**
     * Fetches the current UserToken if one exists.
     *
     * @return UserToken if present.
     */
    public static Optional<UserToken> getCurrentUser() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UserToken userToken) {
            return Optional.of(userToken);
        }

        return Optional.empty();
    }
}
