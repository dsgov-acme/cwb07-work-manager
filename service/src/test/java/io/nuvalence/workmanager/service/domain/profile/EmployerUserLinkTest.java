package io.nuvalence.workmanager.service.domain.profile;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class EmployerUserLinkTest {

    @Test
    void testEqualsWithSameFields() {
        UUID id = UUID.randomUUID();
        Employer employer = new Employer();
        UUID userId = UUID.randomUUID();
        ProfileAccessLevel accessLevel = ProfileAccessLevel.ADMIN;

        EmployerUserLink link1 =
                EmployerUserLink.builder()
                        .id(id)
                        .profile(employer)
                        .userId(userId)
                        .profileAccessLevel(accessLevel)
                        .createdBy("User1")
                        .lastUpdatedBy("User1")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .build();

        EmployerUserLink link2 =
                EmployerUserLink.builder()
                        .id(id)
                        .profile(employer)
                        .userId(userId)
                        .profileAccessLevel(accessLevel)
                        .createdBy("User1")
                        .lastUpdatedBy("User1")
                        .createdTimestamp(link1.getCreatedTimestamp())
                        .lastUpdatedTimestamp(link1.getLastUpdatedTimestamp())
                        .build();

        assertEquals(link1, link2);
    }

    @Test
    void testNotEqualsWithDifferentFields() {
        Employer employer = new Employer(); // assuming a default employer is available
        ProfileAccessLevel accessLevel1 = ProfileAccessLevel.ADMIN;
        ProfileAccessLevel accessLevel2 = ProfileAccessLevel.WRITER;

        EmployerUserLink link1 =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(employer)
                        .userId(UUID.randomUUID())
                        .profileAccessLevel(accessLevel1)
                        .build();

        EmployerUserLink link2 =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(employer)
                        .userId(UUID.randomUUID())
                        .profileAccessLevel(accessLevel2)
                        .build();

        assertNotEquals(link1, link2);
    }

    @Test
    void testHashCodeConsistency() {
        EmployerUserLink link =
                EmployerUserLink.builder()
                        .id(UUID.randomUUID())
                        .profile(new Employer()) // assuming a default employer is available
                        .userId(UUID.randomUUID())
                        .profileAccessLevel(ProfileAccessLevel.ADMIN) // adjust as needed
                        .build();

        int initialHashCode = link.hashCode();
        int subsequentHashCode = link.hashCode();

        assertEquals(initialHashCode, subsequentHashCode);
    }
}
