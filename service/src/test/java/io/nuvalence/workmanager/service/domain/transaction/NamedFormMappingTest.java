package io.nuvalence.workmanager.service.domain.transaction;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class NamedFormMappingTest {

    @Test
    void equalsHashcodeContract() {
        EqualsVerifier.forClass(NamedFormMapping.class).usingGetClass().verify();
    }
}
