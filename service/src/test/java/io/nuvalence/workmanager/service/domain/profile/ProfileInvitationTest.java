package io.nuvalence.workmanager.service.domain.profile;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class ProfileInvitationTest {

    @Test
    void testEqualsWithSameFields() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        OffsetDateTime createdTimestamp = OffsetDateTime.now();

        ProfileInvitation invitation1 =
                ProfileInvitation.builder()
                        .id(id)
                        .profileId(profileId)
                        .type(ProfileType.EMPLOYER)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .expires(createdTimestamp.plusDays(7))
                        .claimed(false)
                        .email("test@example.com")
                        .createdTimestamp(createdTimestamp)
                        .build();

        ProfileInvitation invitation2 =
                ProfileInvitation.builder()
                        .id(id)
                        .profileId(profileId)
                        .type(ProfileType.EMPLOYER)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .expires(createdTimestamp.plusDays(7))
                        .claimed(false)
                        .email("test@example.com")
                        .createdTimestamp(createdTimestamp)
                        .build();

        assertEquals(invitation1, invitation2);
    }

    @Test
    void testNotEqualsWithDifferentFields() {
        ProfileInvitation invitation1 =
                ProfileInvitation.builder()
                        .id(UUID.randomUUID())
                        .profileId(UUID.randomUUID())
                        .type(ProfileType.EMPLOYER)
                        .build();

        ProfileInvitation invitation2 =
                ProfileInvitation.builder()
                        .id(UUID.randomUUID())
                        .profileId(UUID.randomUUID())
                        .type(ProfileType.EMPLOYER)
                        .build();

        assertNotEquals(invitation1, invitation2);
    }

    @Test
    void testHashCodeConsistency() {
        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .id(UUID.randomUUID())
                        .profileId(UUID.randomUUID())
                        .type(ProfileType.EMPLOYER)
                        .build();

        int initialHashCode = invitation.hashCode();
        int subsequentHashCode = invitation.hashCode();

        assertEquals(initialHashCode, subsequentHashCode);
    }
}
