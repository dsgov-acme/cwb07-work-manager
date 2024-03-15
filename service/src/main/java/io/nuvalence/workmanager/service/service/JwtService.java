package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Service layer to manage JSON Web Tokens .
 */
@Component
public class JwtService {

    /**
     * Returns mapped object from passed JWT object.
     *
     * @param token Token to map object from HTTP request
     * @return Mapped JWT object
     *
     * @throws UnexpectedException If there is an error parsing the JWT.
     */
    public Map<String, Object> getObjectMapFromHeader(String token) {
        if (token == null) {
            Thread.currentThread().interrupt();
            throw new UnexpectedException(
                    "Unable to parse JWT Object from header; Token cannot be null.");
        }
        try {
            if (token.contains("Bearer ")) {
                token = token.replace("Bearer ", "");
            }

            String payload = this.getPayloadFromJwt(token);
            return JsonParserFactory.getJsonParser().parseMap(payload);
        } catch (ArrayIndexOutOfBoundsException e) {
            Thread.currentThread().interrupt();
            throw new UnexpectedException(
                    "Unable to parse JWT Object from header; Invalid token", e);
        }
    }

    private String getPayloadFromJwt(String token) {
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
    }
}
