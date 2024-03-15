package io.nuvalence.workmanager.service.domain.profile;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

class EmployerTest {
    @Test
    void testEqualsWithSameFields() {
        UUID id = UUID.randomUUID();
        Employer employer1 =
                Employer.builder()
                        .id(id)
                        .fein("123456789")
                        .legalName("Acme Corporation")
                        .otherNames(Collections.singletonList("Acme"))
                        .type("LLC")
                        .industry("Retail")
                        .summaryOfBusiness("Retail Business")
                        .businessPhone("123-456-7890")
                        .build();

        Employer employer2 =
                Employer.builder()
                        .id(id)
                        .fein("123456789")
                        .legalName("Acme Corporation")
                        .otherNames(Collections.singletonList("Acme"))
                        .type("LLC")
                        .industry("Retail")
                        .summaryOfBusiness("Retail Business")
                        .businessPhone("123-456-7890")
                        .build();

        assertEquals(employer1, employer2);
    }

    @Test
    void testNotEqualsWithDifferentFields() {
        Employer employer1 = Employer.builder().id(UUID.randomUUID()).fein("123456789").build();

        Employer employer2 = Employer.builder().id(UUID.randomUUID()).fein("987654321").build();

        assertNotEquals(employer1, employer2);
    }

    @Test
    void testHashCodeConsistency() {
        Employer employer = Employer.builder().id(UUID.randomUUID()).fein("123456789").build();

        int initialHashCode = employer.hashCode();
        int subsequentHashCode = employer.hashCode();

        assertEquals(initialHashCode, subsequentHashCode);
    }
}
