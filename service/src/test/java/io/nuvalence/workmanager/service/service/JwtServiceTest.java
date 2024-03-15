package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;

import java.util.Map;

class JwtServiceTest {

    @Test
    void getPayloadFromJwtWithBearerReturnsStringIfValid() {
        String decodedToken =
                "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true,\"email\":"
                        + "\"email@email.com\",\"iat\":1653398505,\"exp\":1653402105}";
        String token =
                "Bearer"
                        + " eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."
                        + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIi"
                        + "wiYWRtaW4iOnRydWUsImVtYWlsIjoiZW1haWxAZW1haWwuY2"
                        + "9tIiwiaWF0IjoxNjUzMzk4NTA1LCJleHAiOjE2NT"
                        + "M0MDIxMDV9.47CG8O05lgSOBQN5PVfdeYNSTY_Q-A3n4VwzajXhhas";
        Map<String, Object> jwtMap = JsonParserFactory.getJsonParser().parseMap(decodedToken);
        JwtService jwtService = new JwtService();
        Map<String, Object> payloadMap = jwtService.getObjectMapFromHeader(token);
        assertEquals(payloadMap, jwtMap);
    }

    @Test
    void getPayloadFromJwtReturnsStringIfValid() {
        String decodedToken =
                "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true,\"email\":"
                        + "\"email@email.com\",\"iat\":1653398505,\"exp\":1653402105}";
        String token =
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."
                        + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIi"
                        + "wiYWRtaW4iOnRydWUsImVtYWlsIjoiZW1haWxA"
                        + "ZW1haWwuY29tIiwiaWF0IjoxNjUzMzk4NTA1LCJleHA"
                        + "iOjE2NTM0MDIxMDV9.47CG8O05lgSOBQN5PVfdeYNSTY_Q-A3n4VwzajXhhas";
        Map<String, Object> jwtMap = JsonParserFactory.getJsonParser().parseMap(decodedToken);
        JwtService jwtService = new JwtService();
        Map<String, Object> payloadMap = jwtService.getObjectMapFromHeader(token);
        assertEquals(payloadMap, jwtMap);
    }

    @Test
    void getPayloadFromJwtThrowsIfNotValid() {
        String invalidToken = "test1234";
        JwtService jwtService = new JwtService();
        Exception exception =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            Map<String, Object> payloadMap =
                                    jwtService.getObjectMapFromHeader(invalidToken);
                        });

        String expectedMessage = "Invalid token";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void getPayloadFromJwtThrowsIfTokenIsNull() {
        JwtService jwtService = new JwtService();

        Exception exception =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            Map<String, Object> payloadMap =
                                    jwtService.getObjectMapFromHeader(null);
                        });

        String expectedMessage = "Token cannot be null";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
