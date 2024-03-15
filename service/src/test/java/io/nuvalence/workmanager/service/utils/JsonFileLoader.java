package io.nuvalence.workmanager.service.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * this class allows loading JSON files transformed into Map class to do unit tests.
 */
public class JsonFileLoader {

    /**
     * JSON files Loader and transaformer to Map.
     *
     * @param jsonFilePath Path to the file to be loaded
     * @return JSON file loaded and transformed to Map
     * @throws IOException Error thrown in case the file cannot be loaded
     */
    public Map<String, Object> loadConfigMap(String jsonFilePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(jsonFilePath)) {
            String jsonString = loadConfigString(jsonFilePath);
            return new ObjectMapper().readValue(jsonString, new TypeReference<>() {});
        }
    }

    /**
     * text file Loader.
     *
     * @param filePath Path to the file to be loaded
     * @return File loaded as String
     * @throws IOException Error thrown in case the file cannot be loaded
     */
    public String loadConfigString(String filePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(filePath)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
