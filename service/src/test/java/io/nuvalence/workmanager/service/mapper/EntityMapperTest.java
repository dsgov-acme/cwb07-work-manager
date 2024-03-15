package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import io.nuvalence.workmanager.service.utils.testutils.DataUtils;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EntityMapperTest {

    @Mock SchemaService schemaService;

    @Mock private Appender<ILoggingEvent> mockAppender;

    @InjectMocks private final EntityMapper entityMapper = new EntityMapperImpl();
    private Schema schema;
    private Schema officeInfoSchema;

    private Map<String, Object> transactionData;
    private final JsonFileLoader jsonLoader = new JsonFileLoader();

    @BeforeEach
    void setup() throws IOException {
        schema = DataUtils.createSchemaWithoutSavingToDb();
        transactionData = jsonLoader.loadConfigMap("/basicTransactionData.json");

        DynaProperty city = new DynaProperty("city", String.class);
        DynaProperty address = new DynaProperty("address", String.class);
        officeInfoSchema =
                Schema.builder().id(UUID.randomUUID()).properties(List.of(city, address)).build();
        Logger logger = (Logger) LoggerFactory.getLogger(EntityMapper.class);
        logger.addAppender(mockAppender);
    }

    @Test
    void testApplyMappedPropertiesToEntity() throws MissingSchemaException {
        when(schemaService.getSchemaByKey("OfficeInfo"))
                .thenReturn(Optional.ofNullable(officeInfoSchema));
        DynamicEntity result = new DynamicEntity(schema);

        entityMapper.applyMappedPropertiesToEntity(result, transactionData);

        assertEquals("myFirstName", result.get("firstName"));
        assertEquals("myLastName", result.get("lastName"));
        assertEquals("email@something.com", result.get("email"));
        assertEquals("myAddress", result.get("address"));
        assertEquals("myEmploymentStatus", result.get("employmentStatus"));
        assertEquals("myCompany", result.get("company"));
        assertEquals(true, result.get("isMailingAddressNeeded"));
        assertEquals(30, result.get("age"));

        LocalDate localDateResult = (LocalDate) result.get("dateOfBirth");
        assertEquals(1993, localDateResult.getYear());
        assertEquals(12, localDateResult.getMonthValue());
        assertEquals(21, localDateResult.getDayOfMonth());

        Document resultDocument = (Document) result.get("document");
        assertEquals(
                UUID.fromString("f84b20e8-7a64-431f-ad94-440ca0c4b7c1"),
                resultDocument.getDocumentId());
    }

    @Test
    void testApplyMappedPropertiesToEntityKeyNotFound() throws MissingSchemaException {
        DynamicEntity result = new DynamicEntity(schema);
        transactionData.clear();
        transactionData.put("keyNotFound", "value");

        entityMapper.applyMappedPropertiesToEntity(result, transactionData);
        verify(mockAppender)
                .doAppend(
                        ArgumentMatchers.argThat(
                                argument ->
                                        argument.getLevel().equals(Level.WARN)
                                                && argument.getFormattedMessage()
                                                        .contains(
                                                                "Unable to apply property to"
                                                                        + " entity: keyNotFound")));
    }

    @Test
    void testApplyMappedPropertiesToEntityConversionError() throws MissingSchemaException {
        DynaProperty invalidTypeProperty = new DynaProperty("city", Schema.class);
        Schema schema1 =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(List.of(invalidTypeProperty))
                        .build();
        DynamicEntity result = new DynamicEntity(schema1);
        transactionData.clear();
        transactionData.put("city", "value");

        entityMapper.applyMappedPropertiesToEntity(result, transactionData);
        verify(mockAppender)
                .doAppend(
                        ArgumentMatchers.argThat(
                                argument ->
                                        argument.getLevel().equals(Level.WARN)
                                                && argument.getFormattedMessage()
                                                        .contains(
                                                                "Unable to apply property to"
                                                                        + " entity: city")));
    }

    @Test
    void testApplyMappedPropertiesToEntityInvalidKeyTypeCompositeObject()
            throws MissingSchemaException {
        DynaProperty invalidTypeProperty = new DynaProperty("city", DynamicEntity.class);
        Schema schema1 =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(List.of(invalidTypeProperty))
                        .build();
        DynamicEntity result = new DynamicEntity(schema1);
        transactionData.clear();
        transactionData.put("city", "value");

        entityMapper.applyMappedPropertiesToEntity(result, transactionData);
        verify(mockAppender)
                .doAppend(
                        ArgumentMatchers.argThat(
                                argument ->
                                        argument.getLevel().equals(Level.WARN)
                                                && argument.getFormattedMessage()
                                                        .contains(
                                                                "Unable to apply property to"
                                                                        + " entity: city")));
    }

    @Test
    void testApplyMappedPropertiesToEntityInvalidKeyTypeList() throws MissingSchemaException {
        DynaProperty invalidTypeProperty = new DynaProperty("city", List.class);
        Schema schema1 =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(List.of(invalidTypeProperty))
                        .build();
        DynamicEntity result = new DynamicEntity(schema1);
        transactionData.clear();
        transactionData.put("city", 1);

        entityMapper.applyMappedPropertiesToEntity(result, transactionData);
        verify(mockAppender)
                .doAppend(
                        ArgumentMatchers.argThat(
                                argument ->
                                        argument.getLevel().equals(Level.WARN)
                                                && argument.getFormattedMessage()
                                                        .contains(
                                                                "Unable to apply property to"
                                                                        + " entity: city")));
    }

    @Test
    void testFlattenDynaDataMap_withDocumentList() {
        DynaProperty documentList =
                new DynaProperty("multipleDocuments", List.class, Document.class);
        Schema schemaWithDocumentList =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(Collections.singletonList(documentList))
                        .build();

        Document documentOne = Document.builder().documentId(UUID.randomUUID()).build();
        Document documentTwo = Document.builder().documentId(UUID.randomUUID()).build();
        Document documentThree = Document.builder().documentId(UUID.randomUUID()).build();
        List<Document> documentListValue = List.of(documentOne, documentTwo, documentThree);

        DynamicEntity entity = new DynamicEntity(schemaWithDocumentList);
        entity.set("multipleDocuments", documentListValue);

        Map<String, String> result = entityMapper.flattenDynaDataMap(entity);

        assertEquals(3, result.size());
        assertEquals(documentOne.getDocumentId().toString(), result.get("multipleDocuments[0]"));
        assertEquals(documentTwo.getDocumentId().toString(), result.get("multipleDocuments[1]"));
        assertEquals(documentThree.getDocumentId().toString(), result.get("multipleDocuments[2]"));
    }

    @Test
    void testApplyMappedPropertiesToEntity_withPartialFailure() throws MissingSchemaException {
        DynamicEntity dynamicEntity = new DynamicEntity(schema);

        Map<String, Object> transactionData =
                Map.of(
                        "firstName", "myFirstName",
                        "invalidKey", "invalidKeyValue");

        entityMapper.applyMappedPropertiesToEntity(dynamicEntity, transactionData);

        assertEquals("myFirstName", dynamicEntity.get("firstName"));
    }

    @Test
    void testConvertAttributesToGenericMap_withPartialFailure() throws MissingSchemaException {
        Schema schema = mock(Schema.class);
        when(schema.getDynaProperties())
                .thenReturn(
                        new DynaProperty[] {
                            new DynaProperty("firstName", String.class),
                            new DynaProperty("invalidKey", String.class)
                        });

        DynamicEntity dynamicEntity = mock(DynamicEntity.class);
        when(dynamicEntity.getSchema()).thenReturn(schema);
        when(dynamicEntity.get("firstName")).thenReturn("myFirstName");
        when(dynamicEntity.get("invalidKey")).thenThrow(new IllegalArgumentException());

        Map<String, Object> result = entityMapper.convertAttributesToGenericMap(dynamicEntity);

        assertEquals("myFirstName", result.get("firstName"));
        verify(mockAppender)
                .doAppend(
                        ArgumentMatchers.argThat(
                                argument ->
                                        argument.getLevel().equals(Level.WARN)
                                                && argument.getFormattedMessage()
                                                        .contains(
                                                                "Unable to convert property to"
                                                                        + " generic map")));
    }
}
