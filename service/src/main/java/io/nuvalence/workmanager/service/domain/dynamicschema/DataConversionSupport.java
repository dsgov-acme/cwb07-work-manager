package io.nuvalence.workmanager.service.domain.dynamicschema;

import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Utility to support conversion of data unmarshalled from JSON to their intended types defined by schema.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class DataConversionSupport {
    private static final Map<Class<?>, Map<Class<?>, Function<?, ?>>> converters;
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern ISO_DATE_TIME_PATTERN =
            Pattern.compile(
                    "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:[+-]\\d{2}:\\d{2}|Z)?$");

    static {
        converters = new HashMap<>();
        register(Double.class, BigDecimal.class, DataConversionSupport::convertDoubleToBigDecimal);
        register(
                Integer.class, BigDecimal.class, DataConversionSupport::convertIntegerToBigDecimal);
        register(String.class, BigDecimal.class, DataConversionSupport::convertStringToBigDecimal);

        register(String.class, Boolean.class, DataConversionSupport::convertStringToBoolean);

        register(String.class, Integer.class, DataConversionSupport::convertStringToInteger);

        register(String.class, LocalDate.class, DataConversionSupport::convertStringToLocalDate);

        register(String.class, LocalTime.class, DataConversionSupport::convertStringToLocalTime);

        register(Map.class, Document.class, DataConversionSupport::convertMapToDocument);
    }

    /**
     * Converts the given value to the requested type, if a converter exists.
     *
     * @param value Value to convert
     * @param type  requested type
     * @param <T>   requested type
     * @return the value converted ot the requested type
     */
    public static <T> T convert(final Object value, final Class<T> type) {
        if (value == null) {
            return null;
        }

        final Class<?> inputType = value.getClass();
        if (type.isAssignableFrom(inputType)) {
            return type.cast(value);
        }

        return findConverter(inputType, type)
                .orElseThrow(
                        () ->
                                new UnsupportedOperationException(
                                        String.format(
                                                "No converter found to convert %s to %s",
                                                value.getClass().getName(), type.getName())))
                .apply(inputType.cast(value));
    }

    private static <T, R> void register(
            final Class<T> from, final Class<R> to, final Function<T, R> converter) {
        converters.computeIfAbsent(from, key -> new HashMap<>()).put(to, converter);
    }

    private static <T, R> Optional<Function<Object, R>> findConverter(
            final Class<T> from, final Class<R> to) {
        for (Map.Entry<Class<?>, Map<Class<?>, Function<?, ?>>> inputType : converters.entrySet()) {
            if (inputType.getKey().isAssignableFrom(from)) {
                final Map<Class<?>, Function<?, ?>> candidates = inputType.getValue();
                for (Map.Entry<Class<?>, Function<?, ?>> outputType : candidates.entrySet()) {
                    if (to.isAssignableFrom(outputType.getKey())) {
                        @SuppressWarnings("unchecked")
                        final Function<Object, R> converter =
                                (Function<Object, R>) outputType.getValue();
                        return Optional.of(converter);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static LocalDate convertStringToLocalDate(final String value) {
        if (value.isEmpty()) {
            return null;
        }

        if (ISO_DATE_PATTERN.matcher(value).matches()) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        }

        if (ISO_DATE_TIME_PATTERN.matcher(value).matches()) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        }

        log.error("Could not convert value \"{}\" to LocalDate value. Returning null.", value);
        return null;
    }

    private static LocalTime convertStringToLocalTime(final String value) {
        if (value.isEmpty()) {
            return null;
        }
        return LocalTime.parse(value);
    }

    private static Integer convertStringToInteger(final String value) {
        if (value.isEmpty()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private static BigDecimal convertStringToBigDecimal(final String value) {
        if (value.isEmpty()) {
            return null;
        }
        return new BigDecimal(value);
    }

    private static BigDecimal convertIntegerToBigDecimal(final Integer value) {
        return new BigDecimal(value);
    }

    private static BigDecimal convertDoubleToBigDecimal(final Double value) {
        return BigDecimal.valueOf(value);
    }

    private static Boolean convertStringToBoolean(final String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    "Could not convert string value to boolean; the length of string is"
                            + " less than 1");
        }

        return switch (value.toLowerCase(Locale.ENGLISH)) {
            case "true", "yes" -> true;
            case "false", "no" -> false;
            default -> {
                log.warn("Could not convert string value \"{}\" to boolean value.", value);
                throw new IllegalArgumentException(
                        String.format("Could not convert string value %s to boolean value", value));
            }
        };
    }

    private static Document convertMapToDocument(Map<String, Object> documentMap) {
        final String documentIdKeyLabel = "documentId";
        final String filenameKeyLabel = "filename";

        if (!documentMap.containsKey(documentIdKeyLabel)) {
            log.error(
                    "could not find documentId. docutmentId: {}",
                    documentMap.get(documentIdKeyLabel));
            return null;
        }

        UUID documentId = UUID.fromString((String) documentMap.get("documentId"));
        String filename = (String) documentMap.get(filenameKeyLabel);

        return Document.builder().documentId(documentId).filename(filename).build();
    }
}
