package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.List;

class DynamicEntityTest {

    @Mock private ApplicationContext applicationContext;
    private Schema contactSchema;
    private Schema addressSchema;
    private Schema emailAddressSchema;
    private DynamicEntity contact;

    private EntityMapper mapper;

    @BeforeEach
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        mapper = Mappers.getMapper(EntityMapper.class);
        mapper.setApplicationContext(applicationContext);
        mapper.setObjectMapper(objectMapper);
        addressSchema =
                Schema.builder()
                        .name("Address")
                        .property("line1", String.class)
                        .property("line2", String.class)
                        .property("city", String.class)
                        .property("state", String.class)
                        .property("postalCode", String.class)
                        .build();
        emailAddressSchema =
                Schema.builder()
                        .name("EmailAddress")
                        .property("type", String.class)
                        .property("email", String.class)
                        .build();
        contactSchema =
                Schema.builder()
                        .name("Contact")
                        .property("firstName", String.class)
                        .property("middleName", String.class)
                        .property("lastName", String.class)
                        .computedProperty(
                                "name",
                                String.class,
                                "#concat(\" \", firstName, middleName, lastName)")
                        .property("address", addressSchema)
                        .property("emails", List.class, emailAddressSchema)
                        .build();
        contact = new DynamicEntity(contactSchema);
        contact.set("firstName", "Thomas");
        contact.set("lastName", "Anderson");
        final DynamicEntity address = new DynamicEntity(addressSchema);
        address.set("line1", "123 Street St");
        address.set("city", "New York");
        address.set("state", "NY");
        address.set("postalCode", "11111");
        contact.set("address", address);
        final DynamicEntity emailAddress1 = new DynamicEntity(emailAddressSchema);
        emailAddress1.set("type", "work");
        emailAddress1.set("email", "tanderson@nuvalence.io");
        contact.add("emails", emailAddress1);
    }

    @Test
    void canRetrievePropertyValuesByElPath() {
        assertEquals("Thomas Anderson", contact.getProperty("name", String.class));
        assertEquals("New York", contact.getProperty("address.city", String.class));
        assertEquals(
                "tanderson@nuvalence.io", contact.getProperty("emails[0].email", String.class));
    }

    @Test
    void throwsIllegalArgumentExceptionWhenAddIsCalledOnNonList() {
        DynamicEntity address = contact.getProperty("address", DynamicEntity.class);
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    contact.add("address", address);
                },
                "IllegalArgumentException was expected");
    }

    @Test
    void throwsIllegalArgumentExceptionExceptionWhenPropertyPathIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    contact.getProperty("faxNumber", String.class);
                },
                "IllegalArgumentException was expected");
    }

    @Test
    void equalsHashcodeContract() {
        final BasicDynaClass redDynaClass =
                new BasicDynaClass(
                        "redschema",
                        BasicDynaBean.class,
                        List.of(
                                        new DynaProperty("foo", String.class),
                                        new DynaProperty("bar", List.class, String.class),
                                        new DynaProperty("baz", Integer.class))
                                .toArray(new DynaProperty[0]));
        final BasicDynaClass blueDynaClass =
                new BasicDynaClass(
                        "blueschema",
                        BasicDynaBean.class,
                        List.of(
                                        new DynaProperty("foo", String.class),
                                        new DynaProperty("baz", String.class))
                                .toArray(new DynaProperty[0]));
        final BasicDynaBean redDynaBean = new BasicDynaBean(redDynaClass);
        redDynaBean.set("foo", "foo");
        redDynaBean.set("bar", List.of("bar"));
        redDynaBean.set("baz", 42);
        final BasicDynaBean blueDynaBean = new BasicDynaBean(redDynaClass);
        blueDynaBean.set("foo", "foo");
        blueDynaBean.set("bar", List.of("baz"));

        EqualsVerifier.forClass(DynamicEntity.class)
                .withPrefabValues(DynaClass.class, redDynaClass, blueDynaClass)
                .withPrefabValues(DynaBean.class, redDynaBean, blueDynaBean)
                .withNonnullFields("attributes")
                .suppress(Warning.ALL_FIELDS_SHOULD_BE_USED)
                .verify();
    }

    @Test
    void attemptToSetComputedPropertyThrowsUnsupportedException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    contact.set("name", "Neo");
                },
                "UnsupportedOperationException was expected");
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    contact.set("name", 0, "Neo");
                },
                "UnsupportedOperationException was expected");
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    contact.set("name", "key", "Neo");
                },
                "UnsupportedOperationException was expected");
    }

    @Test
    void attemptToAccessComputedPropertyAsListOrMapThrowsUnsupportedException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    contact.get("name", 0);
                },
                "UnsupportedOperationException was expected");
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    contact.get("name", "key");
                },
                "UnsupportedOperationException was expected");
    }
}
