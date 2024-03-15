package io.nuvalence.workmanager.service.domain.profile;

import java.util.UUID;

/**
 * Represents a link between a user and a profile.
 */
public interface ProfileUserLink {

    UUID getId();

    Profile getProfile();

    UUID getUserId();

    // ProfileAccessLevel getAccessLevel(); // recommended requires significant refactoring, so done
    // when moved to tokens and user-management

    // ProfileType getProfileType(); // recommended (so IndividualUserLink and EmployerUserLink
    // could be persisted in a single table)
}
