package io.nuvalence.workmanager.service.domain.profile;

import java.util.UUID;

/**
 * Represents a profile.
 */
public interface Profile {

    UUID getId();

    // ProfileType getProfileType(); // this method signature is a recommended approach instead of
    // the following default method implementation. But this is being moved to tokens and
    // user-management
    /**
     * Returns the type of profile.
     *
     * @return the profile type
     */
    default ProfileType getProfileType() {
        if (this instanceof Individual) {
            return ProfileType.INDIVIDUAL;
        } else if (this instanceof Employer) {
            return ProfileType.EMPLOYER;
        } else {
            throw new IllegalStateException("Unknown profile type");
        }
    }
}
