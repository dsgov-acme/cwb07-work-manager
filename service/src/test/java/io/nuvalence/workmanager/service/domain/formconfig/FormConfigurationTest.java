package io.nuvalence.workmanager.service.domain.formconfig;

import io.nuvalence.workmanager.service.domain.record.RecordDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class FormConfigurationTest {
    @Test
    void equalsHashcodeContract() {

        TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test1")
                        .key("test1")
                        .description("test1")
                        .build();
        TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test2")
                        .key("test2")
                        .description("test2")
                        .build();
        RecordDefinition recordDefinition1 =
                RecordDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test1")
                        .key("test1")
                        .description("test1")
                        .build();
        RecordDefinition recordDefinition2 =
                RecordDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test2")
                        .key("test2")
                        .description("test2")
                        .build();

        EqualsVerifier.forClass(FormConfiguration.class)
                .withPrefabValues(
                        TransactionDefinition.class, transactionDefinition1, transactionDefinition2)
                .withPrefabValues(RecordDefinition.class, recordDefinition1, recordDefinition2)
                .usingGetClass()
                .verify();
    }
}
