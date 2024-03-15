package io.nuvalence.workmanager.service.domain.dynamicschema;

import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

/**
 * Represents a single instance of a data object with a dynamically configured schema.
 */
@ToString
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public final class DynamicEntity implements DynaBean {
    @Getter
    @Convert(converter = SchemaReferenceAttributeConverter.class)
    @Column(name = "dynamic_schema_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Schema schema;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> data;

    @Delegate @Transient @EqualsAndHashCode.Exclude private DynaBean attributes;

    /**
     * Constructs new Entity with a given schema.
     *
     * @param schema Schema that defines this Entity's structure
     *
     * @throws UnexpectedException if unable to instantiate new schema
     */
    public DynamicEntity(final Schema schema) {
        this.schema = schema;
        try {
            this.attributes = schema.newInstance();
            for (DynaProperty property : schema.getDynaProperties()) {
                if (List.class.isAssignableFrom(property.getType())) {
                    attributes.set(property.getName(), new ArrayList<>());
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            throw new UnexpectedException("Unable to instantiate new " + schema.getName(), e);
        }
    }

    /**
     * Gets property value at a given Expression Language (EL) path and casts it to a provided type.
     *
     * @param path expression Language (EL) path of property to retrieve
     * @param type Class definition of expected property type
     * @param <T>  Expected property type
     * @return Property value as type &lt;T&gt;
     *
     * @throws IllegalArgumentException if the field is not a list
     */
    public <T> T getProperty(final String path, final Class<T> type) {
        try {
            return type.cast(PropertyUtils.getProperty(attributes, path));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Unable to access path: " + path + " as type " + type.getSimpleName(), e);
        }
    }

    /**
     * Adds an element to a list attribute.
     *
     * @param name  name of the list attribute
     * @param value value to add
     *
     * @throws IllegalArgumentException if the field is not a list
     */
    public void add(final String name, final Object value) {
        if (!isList(name)) {
            throw new IllegalArgumentException(String.format("Field [%s] is not a list.", name));
        }
        @SuppressWarnings("unchecked")
        final List<Object> list = (List<Object>) get(name);
        list.add(value);
    }

    private boolean isList(final String name) {
        return List.class.isAssignableFrom(schema.getDynaProperty(name).getType());
    }

    /**
     * JPA post load conversion of internal data.
     *
     * @throws UnexpectedException if unable to instantiate new schema
     */
    public void postLoad() {
        final EntityMapper mapper = EntityMapper.getInstance();
        try {
            this.attributes = schema.newInstance();
            mapper.applyMappedPropertiesToEntity(this, data);
        } catch (IllegalAccessException | InstantiationException | MissingSchemaException e) {
            throw new UnexpectedException("Unable to instantiate new " + schema.getName(), e);
        }
    }

    /**
     * JPA pre persist/update conversion of internal data.
     */
    public void preSave() {
        final EntityMapper mapper = EntityMapper.getInstance();
        data = mapper.convertAttributesToGenericMap(this);
    }
}
