package io.nuvalence.workmanager.service.domain.schema;

import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

class SchemaRowTest {

    @Test
    void equalsHashCodeContract() {
        SchemaRow schemaRow1 =
                SchemaRow.builder()
                        .id(UUID.fromString("8d1b6c4f-89af-4d34-a983-98c40594ac63"))
                        .key("CommonAddress")
                        .name("Common address")
                        .schemaJson("{}")
                        .build();

        SchemaRow schemaRow2 =
                SchemaRow.builder()
                        .id(UUID.fromString("8d1b6c4f-89af-4d34-a983-98c40594ac64"))
                        .key("DifferentKey")
                        .name("Different name")
                        .schemaJson("{}")
                        .build();

        SchemaRow schemaRow3 =
                SchemaRow.builder()
                        .id(UUID.fromString("8d1b6c4f-89af-4d34-a983-98c40594ac65"))
                        .key("AnotherDifferentKey")
                        .name("Another different name")
                        .schemaJson("{}")
                        .build();

        EqualsVerifier.forClass(SchemaRow.class)
                .withPrefabValues(SchemaRow.class, schemaRow1, schemaRow2)
                .withPrefabValues(
                        Set.class,
                        Collections.singleton(schemaRow1),
                        Collections.singleton(schemaRow3))
                .usingGetClass()
                .verify();
    }
}
