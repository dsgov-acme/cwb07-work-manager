package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.generated.models.EntityModel;
import io.nuvalence.workmanager.service.service.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DynamicEntityMapperTest {

    @Mock private SchemaService schemaService;

    private EntityMapper mapper;
    private DynamicEntity entity;
    private EntityModel model;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws JsonProcessingException {
        final Schema addressSchema =
                Schema.builder()
                        .name("Address")
                        .property("line1", String.class)
                        .property("line2", String.class)
                        .property("city", String.class)
                        .property("state", String.class)
                        .property("postalCode", String.class)
                        .build();
        final Schema emailAddressSchema =
                Schema.builder()
                        .name("EmailAddress")
                        .property("type", String.class)
                        .property("email", String.class)
                        .build();
        final Schema contactSchema =
                Schema.builder()
                        .name("Contact")
                        .property("name", String.class)
                        .property("address", addressSchema)
                        .property("emails", List.class, emailAddressSchema)
                        .build();
        entity = new DynamicEntity(contactSchema);
        entity.set("name", "Thomas A. Anderson");
        final DynamicEntity address = new DynamicEntity(addressSchema);
        address.set("line1", "123 Street St");
        address.set("city", "New York");
        address.set("state", "NY");
        address.set("postalCode", "11111");
        entity.set("address", address);
        final DynamicEntity emailAddress = new DynamicEntity(emailAddressSchema);
        emailAddress.set("type", "work");
        emailAddress.set("email", "tanderson@nuvalence.io");
        entity.add("emails", emailAddress);

        model =
                new EntityModel()
                        .schema("Contact")
                        .data(
                                Map.of(
                                        "name", "Thomas A. Anderson",
                                        "address",
                                                Map.of(
                                                        "line1", "123 Street St",
                                                        "city", "New York",
                                                        "state", "NY",
                                                        "postalCode", "11111"),
                                        "emails",
                                                List.of(
                                                        Map.of(
                                                                "type", "work",
                                                                "email",
                                                                        "tanderson@nuvalence.io"))));

        objectMapper = new ObjectMapper();

        mapper = Mappers.getMapper(EntityMapper.class);
        mapper.setSchemaService(schemaService);
        mapper.setObjectMapper(objectMapper);

        Mockito.lenient()
                .when(schemaService.getSchemaByKey("Address"))
                .thenReturn(Optional.of(addressSchema));
        Mockito.lenient()
                .when(schemaService.getSchemaByKey("EmailAddress"))
                .thenReturn(Optional.of(emailAddressSchema));
        Mockito.lenient()
                .when(schemaService.getSchemaByKey("Contact"))
                .thenReturn(Optional.of(contactSchema));
    }

    @Test
    void entityToEntityModel() {
        assertEquals(model, mapper.entityToEntityModel(entity));
    }

    @Test
    void entityModelToEntity() throws MissingSchemaException {
        assertEquals(entity, mapper.entityModelToEntity(model));
    }

    @Test
    void testGetInstance() {

        ApplicationContext applicationContext = mock(ApplicationContext.class);

        // Set the test ApplicationContext in the class under test
        mapper.setApplicationContext(applicationContext);
        when(applicationContext.getBean(EntityMapper.class)).thenReturn(mapper);
        // Call the getInstance() method
        EntityMapper instance = EntityMapper.getInstance();

        // Assert that the returned instance is not null
        assertNotNull(instance);
    }

    @Test
    void entityModelToEntityThrowsMissingSchemaExceptionWhenSchemaDoesntExist() {
        // Arrange
        final EntityModel entityModel =
                new EntityModel().schema("Missing").data(Map.of("foo", "bar"));
        Mockito.when(schemaService.getSchemaByKey("Missing")).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(MissingSchemaException.class, () -> mapper.entityModelToEntity(entityModel));
    }

    @Test
    void testConstructorWithMessage() {
        // Create an instance of the exception
        String errorMessage = "Something went wrong";
        BusinessLogicException exception = new BusinessLogicException(errorMessage);

        // Verify the exception message
        String actualMessage = exception.getMessage();
        assertEquals(errorMessage, actualMessage);
    }
}
