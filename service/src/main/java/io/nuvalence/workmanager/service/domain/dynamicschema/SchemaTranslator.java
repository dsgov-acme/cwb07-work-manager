package io.nuvalence.workmanager.service.domain.dynamicschema;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nuvalence.auth.access.cerbos.AccessResourceTranslator;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Schema Translator Class.
 */
@Component
public class SchemaTranslator implements AccessResourceTranslator, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    @SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification =
                    "This is an established pattern for exposing spring state to static contexts."
                        + " The applicationContext is a singleton, so if this write were to occur"
                        + " multiple times, it would be idempotent.")
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        SchemaTranslator.applicationContext = applicationContext;
    }

    @Override
    public Object translate(Object resource) {
        if (resource instanceof Schema schema) {
            final DynamicSchemaMapper mapper =
                    applicationContext.getBean(DynamicSchemaMapper.class);
            return mapper.schemaToSchemaModel(schema);
        }

        return resource;
    }
}
