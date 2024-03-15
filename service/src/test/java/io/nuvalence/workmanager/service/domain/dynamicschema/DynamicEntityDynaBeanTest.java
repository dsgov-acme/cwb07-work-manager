package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DynamicEntityDynaBeanTest {
    @Test
    void computedAttributeCanAccessAttributesOfDynaClass() {
        var dynaClass =
                Schema.builder()
                        .property("name", String.class)
                        .computedProperty("computed", String.class, "name")
                        .build();
        var dynaBean = new DynamicEntityDynaBean(dynaClass);
        dynaBean.set("name", "test");

        assertEquals("test", dynaBean.get("computed"));
    }

    @Test
    void computedAttributeCanAccessChildAttributesInDynaClass() {
        var childClass = Schema.builder().property("name", String.class).build();
        var parentClass =
                Schema.builder()
                        .property("child", childClass)
                        .computedProperty("computed", String.class, "child.name")
                        .build();
        var entity = new DynamicEntity(parentClass);
        entity.set("child", new DynamicEntity(childClass));
        ((DynamicEntity) entity.get("child")).set("name", "test");

        assertEquals("test", entity.get("computed"));
    }

    @Test
    void computedAttributeCannotAccessArbitraryStaticMethods() {
        var dynaClass =
                Schema.builder()
                        .computedProperty("computed", String.class, "T(java.lang.Math).sqrt(15)")
                        .build();
        var dynaBean = new DynamicEntityDynaBean(dynaClass);

        assertThrows(
                IllegalStateException.class,
                () -> {
                    dynaBean.get("computed");
                });
    }
}
