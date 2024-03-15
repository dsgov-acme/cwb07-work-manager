package io.nuvalence.workmanager.service.domain.transaction;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TaskFormMappingTest {

    @Test
    void equalsHashcodeContract() {
        EqualsVerifier.forClass(TaskFormMapping.class).usingGetClass().verify();
    }
}
