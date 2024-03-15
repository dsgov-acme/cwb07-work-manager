package io.nuvalence.workmanager.service.domain.dynamicschema.attributes;

import io.nuvalence.workmanager.service.domain.ApplicationEnum;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Review status for a particular customer provided document.
 */
public enum SupportedTypes implements ApplicationEnum {
    STRING(String.class, "String"),
    LIST(List.class, "List"),
    DYNAMIC_ENTITY(DynamicEntity.class, "Dynamic Entity"),
    BOOLEAN(Boolean.class, "Boolean"),
    INTEGER(Integer.class, "Integer"),
    DECIMAL_NUMBER(BigDecimal.class, "Decimal Number"),
    DATE(LocalDate.class, "Date"),
    TIME(LocalTime.class, "Time"),
    DOCUMENT(Document.class, "Document");

    private final Class<?> typeClass;
    private final String simpleName;
    private final String label;

    SupportedTypes(Class<?> typeClass, String label) {
        this.typeClass = typeClass;
        this.simpleName = typeClass.getSimpleName();
        this.label = label;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getValue() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    /**
     * Utility method to get the SupportedType enum based on the class name.
     *
     * @param simpleName the class name.
     *
     * @return the SupportedType enum.
     */
    public static SupportedTypes getBySimpleName(String simpleName) {
        for (SupportedTypes supportedType : SupportedTypes.values()) {
            if (supportedType.getSimpleName().equals(simpleName)) {
                return supportedType;
            }
        }
        return null; // or throw an exception if not found
    }
}
