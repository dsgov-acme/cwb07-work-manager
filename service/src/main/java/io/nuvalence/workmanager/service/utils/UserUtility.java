package io.nuvalence.workmanager.service.utils;

import io.nuvalence.auth.token.UserToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility for fetching information about the current authenticated user.
 */
public class UserUtility {

    /**
     * Private constructor to prevent instantiation.
     */
    private UserUtility() {}

    /**
     * Returns the current user's UserToken if available.
     *
     * @return Optional wrapping UserToken
     */
    public static Optional<UserToken> getCurrentUserToken() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(UserToken.class::isInstance)
                .map(UserToken.class::cast);
    }

    /**
     * Returns the current user's application user ID if available.
     *
     * @return Optional wrapping application user ID (String)
     */
    public static Optional<String> getCurrentApplicationUserId() {
        return getCurrentUserToken().map(token -> token.getPrincipal().toString());
    }

    public static String getAuthenticatedUserType() {
        Optional<UserToken> currentUserToken = getCurrentUserToken();
        return currentUserToken.map(UserToken::getUserType).orElse(null);
    }
}
