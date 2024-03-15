package io.nuvalence.workmanager.service.domain.dynamicschema;

import io.nuvalence.auth.access.AccessResource;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Dynamically configured schema.
 */
@AccessResource(value = "dynamic_schema", translator = SchemaTranslator.class)
@ToString
public final class Schema implements DynaClass {

    @Getter private final UUID id;
    @Getter private final String key;
    @Getter private final String description;

    @Delegate private final DynaClass dynaClass;

    @Getter private final Map<String, String> relatedSchemas;

    @Getter private final Map<String, List<AttributeConfiguration>> attributeConfigurations;

    @Getter private final String createdBy;
    @Getter private final String lastUpdatedBy;
    @Getter private final OffsetDateTime createdTimestamp;
    @Getter private final OffsetDateTime lastUpdatedTimestamp;

    /**
     * Constructs new Schema.
     *
     * @param id                      ID of the schema
     * @param key                     schema idefinition key to fetch
     * @param name                    Name of ths schema
     * @param description             Description of the schema
     * @param properties              List of property schemas (type and contents)
     * @param relatedSchemas          Map of attributes to their named dynamic schemas (for attributes of type Entity)
     * @param attributeConfigurations Additional configuration data for asynchronous document processing
     * @param createdBy               User who created the schema
     * @param lastUpdatedBy           User who last updated the schema
     * @param createdTimestamp        Timestamp when the schema was created
     * @param lastUpdatedTimestamp    Timestamp when the schema was last updated
     */
    @SuppressWarnings("java:S107")
    @Builder
    public Schema(
            final UUID id,
            final String key,
            final String name,
            final String description,
            final List<DynaProperty> properties,
            final Map<String, String> relatedSchemas,
            final Map<String, List<AttributeConfiguration>> attributeConfigurations,
            final String createdBy,
            final String lastUpdatedBy,
            final OffsetDateTime createdTimestamp,
            final OffsetDateTime lastUpdatedTimestamp) {
        this.id = id;
        this.key = key;
        this.description = description;
        this.attributeConfigurations = attributeConfigurations;
        this.dynaClass = new SchemaDynaClass(name, null, properties.toArray(DynaProperty[]::new));
        this.relatedSchemas = relatedSchemas;
        this.createdBy = createdBy;
        this.lastUpdatedBy = lastUpdatedBy;
        this.createdTimestamp = createdTimestamp;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    /**
     * Returns true if object under test is equal to this Schema.
     *
     * @param o object to test for equality
     * @return true if object under test is equal to this Schema, false otherwise
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Schema)) {
            return false;
        }

        final Schema other = (Schema) o;

        if (!Objects.equals(this.id, other.id)) {
            return false;
        }

        if (!Objects.equals(this.key, other.key)) {
            return false;
        }

        if (!Objects.equals(this.description, other.description)) {
            return false;
        }

        if (!Objects.equals(this.createdBy, other.createdBy)) {
            return false;
        }

        if (!Objects.equals(this.lastUpdatedBy, other.lastUpdatedBy)) {
            return false;
        }

        if (!Objects.equals(this.createdTimestamp, other.createdTimestamp)) {
            return false;
        }

        if (!Objects.equals(this.lastUpdatedTimestamp, other.lastUpdatedTimestamp)) {
            return false;
        }

        final DynaProperty[] thisDynaProperties =
                (this.dynaClass == null) ? new DynaProperty[0] : this.dynaClass.getDynaProperties();
        final DynaProperty[] otherDynaProperties =
                (other.dynaClass == null)
                        ? new DynaProperty[0]
                        : other.dynaClass.getDynaProperties();
        if (!Arrays.equals(thisDynaProperties, otherDynaProperties)) {
            return false;
        }

        if (!Objects.equals(this.relatedSchemas, other.relatedSchemas)) {
            return false;
        }

        return Objects.equals(this.attributeConfigurations, other.attributeConfigurations);
    }

    /**
     * Returns a hash code as int that complies with the contract between hashCode() and equals().
     *
     * @return hash code as int
     */
    public int hashCode() {
        final int prime = 59;
        int result = 1;
        result = prime * result + Objects.hashCode(this.id);
        result = prime * result + Objects.hashCode(this.key);
        result = prime * result + Objects.hashCode(this.description);
        result = prime * result + Objects.hashCode(this.createdBy);
        result = prime * result + Objects.hashCode(this.lastUpdatedBy);
        result = prime * result + Objects.hashCode(this.createdTimestamp);
        result = prime * result + Objects.hashCode(this.lastUpdatedTimestamp);
        final DynaClass localDynaClass = this.dynaClass;
        result =
                result * prime
                        + (localDynaClass == null
                                ? 43
                                : Arrays.hashCode(localDynaClass.getDynaProperties()));
        final Object localRelatedSchemas = this.relatedSchemas;
        result =
                result * prime
                        + (localRelatedSchemas == null ? 43 : localRelatedSchemas.hashCode());
        final Object localAttributeConfigurations = this.attributeConfigurations;
        result =
                result * prime
                        + (localAttributeConfigurations == null
                                ? 43
                                : localAttributeConfigurations.hashCode());
        return result;
    }

    /**
     * Fluent builder for Schema instances.
     */
    public static final class SchemaBuilder {
        private List<DynaProperty> properties = new ArrayList<>();
        private Map<String, String> relatedSchemas = new HashMap<>();

        private Map<String, List<AttributeConfiguration>> attributeConfigurations = new HashMap<>();

        /**
         * Adds a single property to the collection of properties in this schema with a given name and type.
         *
         * @param name property name
         * @param type property type
         * @return reference to this builder
         */
        public SchemaBuilder property(final String name, final Class<?> type) {
            properties.add(new DynaProperty(name, type));
            return this;
        }

        /**
         * Adds a single property to the collection of properties in this schema with a given name and type.
         *
         * @param name property name
         * @param type Schema defining property type
         * @return reference to this builder
         */
        public SchemaBuilder property(final String name, final Schema type) {
            relatedSchemas.put(name, type.getName());
            properties.add(new DynaProperty(name, DynamicEntity.class));
            return this;
        }

        /**
         * Adds a single property to the collection of properties in this schema with a given name and generic type.
         *
         * @param name        property name
         * @param type        property type (generic)
         * @param contentType property content type
         * @return reference to this builder
         */
        public SchemaBuilder property(
                final String name, final Class<?> type, final Class<?> contentType) {
            properties.add(new DynaProperty(name, type, contentType));
            return this;
        }

        /**
         * Adds a single property to the collection of properties in this schema with a given name and generic type.
         *
         * @param name        property name
         * @param type        property type (generic)
         * @param contentType Schema that defines the property content type
         * @return reference to this builder
         */
        public SchemaBuilder property(
                final String name, final Class<?> type, final Schema contentType) {
            relatedSchemas.put(name, contentType.getName());
            properties.add(new DynaProperty(name, type, DynamicEntity.class));
            return this;
        }

        public SchemaBuilder computedProperty(
                final String name, final Class<?> type, final String expression) {
            properties.add(new ComputedDynaProperty(name, type, expression));
            return this;
        }

        /**
         * Adds a single attributeConfiguration to the schema.
         *
         * @param name       property name that attributeConfiguration applies to
         * @param attributeConfiguration additional attribute configuration for document processing
         * @return reference to this builder
         */
        public SchemaBuilder attributeConfiguration(
                final String name, final AttributeConfiguration attributeConfiguration) {
            final List<AttributeConfiguration> attributeConfigurationList =
                    attributeConfigurations.computeIfAbsent(
                            name, attributeKey -> new LinkedList<>());
            attributeConfigurationList.add(attributeConfiguration);
            return this;
        }
    }

    /**
     * Gets attribute configurations given the attribute and desired class to cast to.
     *
     * @param attribute attribute name to get attribute list by.
     * @param type implementation of AttributeConfiguration to cast results to.
     * @param <T> a class that extends AttributeConfiguration
     * @return list of attribute configurations
     */
    public final <T extends AttributeConfiguration> List<T> getAttributeConfigurations(
            final String attribute, final Class<T> type) {
        return attributeConfigurations.getOrDefault(attribute, Collections.emptyList()).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
