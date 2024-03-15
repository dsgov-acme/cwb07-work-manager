package io.nuvalence.workmanager.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.dynamicschema.ComputedDynaProperty;
import io.nuvalence.workmanager.service.domain.dynamicschema.DataConversionSupport;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.generated.models.EntityModel;
import io.nuvalence.workmanager.service.service.SchemaService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaProperty;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps dynamic entities between 3 forms.
 *
 * <ul>
 *     <li>API Model ({@link io.nuvalence.workmanager.service.generated.models.EntityModel})</li>
 *     <li>Logic Object ({@link DynamicEntity})</li>
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        uses = {ObjectMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@Slf4j
@Component
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public abstract class EntityMapper implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private final Set<String> attributesToMesh = Set.of("documentList"); // O(1) contains call
    @Autowired @Setter private ObjectMapper objectMapper;
    @Autowired @Setter private SchemaService schemaService;

    /**
     * Constructs a new instance of an EntityMapper.
     */
    protected EntityMapper() {
        objectMapper =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public static EntityMapper getInstance() {
        return applicationContext.getBean(EntityMapper.class);
    }

    /**
     * Maps {@link DynamicEntity} to
     * {@link io.nuvalence.workmanager.service.generated.models.EntityModel}.
     *
     * @param entity Logic model for entity
     * @return API model for entity
     */
    public EntityModel entityToEntityModel(final DynamicEntity entity) {
        return new EntityModel()
                .schema(entity.getSchema().getName())
                .data(convertAttributesToGenericMap(entity));
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.generated.models.EntityModel} to
     * {@link DynamicEntity}.
     *
     * @param model API model for entity
     * @return Logic model for entity
     * @throws MissingSchemaException If the model references a schema that does not exist in the system.
     */
    public DynamicEntity entityModelToEntity(final EntityModel model)
            throws MissingSchemaException {
        final Schema schema =
                schemaService
                        .getSchemaByKey(model.getSchema())
                        .orElseThrow(() -> new MissingSchemaException(model.getSchema()));
        final DynamicEntity entity = new DynamicEntity(schema);
        applyMappedPropertiesToEntity(entity, model.getData());

        return entity;
    }

    /**
     * Produces a generic map, suitable for JSON serialization.
     *
     * @param entity Entity to convert ot a map
     * @return A generic map (string keys, any values)
     */
    public Map<String, Object> convertAttributesToGenericMap(final DynamicEntity entity) {
        final Map<String, Object> attributes = new HashMap<>();
        for (DynaProperty dynaProperty : entity.getSchema().getDynaProperties()) {
            try {
                final Object value = entity.get(dynaProperty.getName());
                if (value != null) {
                    attributes.put(
                            dynaProperty.getName(), convertDynaPropertyValueToGenericObject(value));
                }
            } catch (Exception e) {
                log.warn(
                        "Unable to convert property to generic map: {}", dynaProperty.getName(), e);
            }
        }

        return attributes;
    }

    /**
     * Applies data to an entity.
     *
     * @param entity Entity to update
     * @param data Generic map of data to apply to entity.
     * @throws MissingSchemaException If the model references a schema that does not exist in the system.
     */
    public void applyMappedPropertiesToEntity(
            final DynamicEntity entity, final Map<String, Object> data)
            throws MissingSchemaException {
        final Schema schema = entity.getSchema();

        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                try {
                    applyProperty(entity, schema, entry);
                } catch (Exception e) {
                    log.warn("Unable to apply property to entity: {}", entry.getKey(), e);
                }
            }
        }
    }

    private void applyProperty(DynamicEntity entity, Schema schema, Map.Entry<String, Object> entry)
            throws MissingSchemaException {
        String key = entry.getKey();
        Object value = entry.getValue();
        DynaProperty dynaProperty = getDynaProperty(schema, key);

        if (dynaProperty instanceof ComputedDynaProperty) {
            return;
        }

        Class<?> type = dynaProperty.getType();

        if (DynamicEntity.class.isAssignableFrom(type)) {
            applyDynamicEntityProperty(entity, schema, key, value);
        } else if (List.class.isAssignableFrom(type)) {
            applyListProperty(entity, schema, key, value);
        } else {
            applySingleValueProperty(entity, schema, key, value, type);
        }
    }

    private DynaProperty getDynaProperty(Schema schema, String key) {
        DynaProperty dynaProperty = schema.getDynaProperty(key);
        if (dynaProperty == null) {
            throw new BusinessLogicException(String.format("Key not found in schema: '%s'.", key));
        }
        return dynaProperty;
    }

    private void applyDynamicEntityProperty(
            DynamicEntity entity, Schema schema, String key, Object value)
            throws MissingSchemaException {
        if (value != null && !(Map.class.isAssignableFrom(value.getClass()))) {
            throw new BusinessLogicException(
                    String.format(
                            "Invalid type for key: '%s'. It should be a composite object.", key));
        }

        Object dynamicEntity = convertSingleValueToEntity(schema, DynamicEntity.class, key, value);
        applyMappedPropertiesToEntity((DynamicEntity) dynamicEntity, (Map<String, Object>) value);
        entity.set(key, dynamicEntity);
    }

    private void applyListProperty(DynamicEntity entity, Schema schema, String key, Object value)
            throws MissingSchemaException {
        if (!List.class.isAssignableFrom(value.getClass())) {
            throw new BusinessLogicException(
                    String.format("Invalid type for key: '%s'. It should be a List.", key));
        }

        List<?> list;
        if (attributesToMesh.contains(key)) {
            list =
                    entityListMapMesh(
                            (List<Object>) entity.get(key), schema, key, (List<Object>) value);
        } else {
            list = convertListValueToEntity(schema, key, (List<Object>) value);
        }

        entity.set(key, list);
    }

    private void applySingleValueProperty(
            DynamicEntity entity, Schema schema, String key, Object value, Class<?> type)
            throws MissingSchemaException {
        if (value != null && !type.isAssignableFrom(value.getClass())) {
            try {
                value = DataConversionSupport.convert(value, type);
                if (value == null) {
                    handleConversionError(value, type, key);
                }
            } catch (Exception e) {
                handleConversionError(value, type, key);
            }
        }
        entity.set(key, convertSingleValueToEntity(schema, type, key, value));
    }

    private void handleConversionError(Object element, Class<?> dynaPropertyType, String key) {
        log.warn("Could not convert {} to {}", element, dynaPropertyType.getSimpleName());
        throw new BusinessLogicException(
                String.format(
                        "Invalid type for key: '%s'. It should be: '%s'.",
                        key, dynaPropertyType.getSimpleName()));
    }

    /**
     * Given a schema, converts a generic map to an DynamicEntity.
     *
     * @param schema Schema of the target entity
     * @param map map containing attribute data
     * @return converted DynamicEntity
     * @throws MissingSchemaException If Schema references by name any additional missing schemas
     * @throws BusinessLogicException If data is not convertible to specified schema
     */
    public DynamicEntity convertGenericMapToEntity(
            final Schema schema, final Map<String, Object> map)
            throws MissingSchemaException, BusinessLogicException {
        try {
            final DynamicEntity entity = new DynamicEntity(schema);
            applyMappedPropertiesToEntity(entity, map);

            return entity;
        } catch (UnsupportedOperationException e) {
            throw new BusinessLogicException("Unable to map data");
        }
    }

    private Object convertDynaPropertyValueToGenericObject(final Object object) {
        if (object instanceof DynamicEntity entity) {
            return convertAttributesToGenericMap(entity);
        } else if (List.class.isAssignableFrom(object.getClass())) {
            return ((List<?>) object)
                    .stream().map(this::convertDynaPropertyValueToGenericObject).toList();
        }

        return object;
    }

    private Object convertSingleValueToEntity(
            final Schema schema, final Class<?> type, final String key, final Object value)
            throws MissingSchemaException {
        if (DynamicEntity.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            final EntityModel subModel =
                    new EntityModel()
                            .schema(schema.getRelatedSchemas().get(key))
                            .data((Map<String, Object>) value);
            return entityModelToEntity(subModel);
        }

        return DataConversionSupport.convert(value, type);
    }

    private List<?> convertListValueToEntity(
            final Schema schema, final String key, final List<Object> list)
            throws MissingSchemaException {

        final List<Object> result = new LinkedList<>();
        for (Object value : list) {
            result.add(
                    convertSingleValueToEntity(
                            schema, schema.getDynaProperty(key).getContentType(), key, value));
        }

        return result;
    }

    private List<?> entityListMapMesh(
            final List<Object> persistedList,
            final Schema schema,
            final String attributeName,
            final List<Object> inputList)
            throws MissingSchemaException {

        // Document specific logic
        if (schema.getDynaProperty(attributeName).getContentType().equals(Document.class)
                && (!inputList.isEmpty())) {
            final List<Object> result = new LinkedList<>();

            // generate a map of persisted documents with an id key to compare ids, access old
            // value.
            Map<UUID, Document> documentMap =
                    persistedList.stream()
                            .filter(Document.class::isInstance)
                            .map(Document.class::cast)
                            .collect(
                                    Collectors.toMap(Document::getDocumentId, Function.identity()));

            // loop input objects, this will ignore removed documents and remove them.
            for (Object rawDoc : inputList) {
                Document doc =
                        (Document)
                                DataConversionSupport.convert(
                                        rawDoc,
                                        schema.getDynaProperty(attributeName).getContentType());
                // run mesh on already existing documents
                if (documentMap.containsKey(doc.getDocumentId())) {
                    if (isAuthorizedForDocuments()) {
                        result.add(
                                convertSingleValueToEntity(
                                        schema,
                                        schema.getDynaProperty(attributeName).getContentType(),
                                        attributeName,
                                        rawDoc));
                    } else {
                        result.add(documentMap.get(doc.getDocumentId()));
                    }
                } else {
                    result.add(
                            convertSingleValueToEntity(
                                    schema,
                                    schema.getDynaProperty(attributeName).getContentType(),
                                    attributeName,
                                    rawDoc));
                }
            }
            return result;
        }
        return convertListValueToEntity(schema, attributeName, inputList);
    }

    // This currently works for every user
    private boolean isAuthorizedForDocuments() {
        // TODO: Add control checks once custom tenant claims are passed in. (requires ADFS / okta
        // claims.)
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER"); // replace this
        return user.getAuthorities().contains(authority);
    }

    @Override
    @SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification =
                    "This is an established pattern for exposing spring state to static contexts."
                        + " The applicationContext is a singleton, so if this write were to occur"
                        + " multiple times, it would be idempotent.")
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        EntityMapper.applicationContext = applicationContext;
    }

    /**
     * This method converts a dynamicEntity data into its flattened String map equivalent.
     *
     * @param entity DynamicEntity data to be flattened
     * @return flattened String map
     */
    public Map<String, String> flattenDynaDataMap(DynamicEntity entity) {
        Map<String, String> flattenedMap = new HashMap<>();
        flattenMap("", entity, flattenedMap);
        return flattenedMap;
    }

    private void flattenMap(String prefix, DynamicEntity entity, Map<String, String> flattenedMap) {
        for (DynaProperty dynaProperty : entity.getSchema().getDynaProperties()) {
            String key = dynaProperty.getName();
            Object value = entity.get(key);

            if (value == null) {
                continue;
            }

            if (value instanceof DynamicEntity dynamicEntity) {
                handleDynamicEntity(prefix, key, dynamicEntity, flattenedMap);
            } else if (value instanceof List) {
                handleList(prefix, key, (List<Object>) value, flattenedMap);
            } else {
                handleSimpleValue(prefix, key, value, flattenedMap);
            }
        }
    }

    private void handleDynamicEntity(
            String prefix, String key, DynamicEntity value, Map<String, String> flattenedMap) {
        String nestedPrefix = prefix.isEmpty() ? key : prefix + "." + key;
        flattenMap(nestedPrefix, value, flattenedMap);
    }

    private void handleList(
            String prefix, String key, List<Object> list, Map<String, String> flattenedMap) {
        for (int i = 0; i < list.size(); i++) {
            Object listValue = list.get(i);
            String nestedPrefix =
                    prefix.isEmpty() ? key + "[" + i + "]" : prefix + "." + key + "[" + i + "]";

            if (listValue instanceof DynamicEntity dynamicEntity) {
                flattenMap(nestedPrefix, dynamicEntity, flattenedMap);
            } else if (listValue instanceof Document document) {
                flattenedMap.put(nestedPrefix, document.getDocumentId().toString());
            } else {
                flattenedMap.put(nestedPrefix, String.valueOf(listValue));
            }
        }
    }

    private void handleSimpleValue(
            String prefix, String key, Object value, Map<String, String> flattenedMap) {
        String stringValue =
                value instanceof Document document
                        ? document.getDocumentId().toString()
                        : String.valueOf(value);

        if (!stringValue.isBlank()) {
            String mapKey = prefix.isEmpty() ? key : prefix + "." + key;
            flattenedMap.put(mapKey, stringValue);
        }
    }
}
