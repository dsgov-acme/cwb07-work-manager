package io.nuvalence.platform;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import io.nuvalence.auth.token.SelfSignedTokenGenerator;
import io.nuvalence.auth.util.RsaKeyUtility;
import io.nuvalence.platform.configuration.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Generates a self-signed JWT token for use in functional tests.
 */
@Slf4j
public class TokenGenerator {
    private static final Duration tokenDuration = Duration.ofMinutes(5);
    private SelfSignedTokenGenerator generator;
    private String token;

    /**
     * Creates a new token generator.
     *
     * @throws IOException if an error occurs while retrieving the private key
     */
    public TokenGenerator() throws IOException {
        init();
    }

    /**
     * Initializes the token generator.
     *
     * @throws IOException if an error occurs while retrieving the private key
     */
    private void init() throws IOException {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName secretVersionName =
                    SecretVersionName.of(
                            Configuration.gcpProjectId,
                            Configuration.tokenPrivateKeySecret,
                            Configuration.secretVersion);
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            String key =
                    new String(
                            response.getPayload().getData().toByteArray(), StandardCharsets.UTF_8);
            log.info("key: {}", key);
            generator =
                    new SelfSignedTokenGenerator(
                            Configuration.issuer,
                            tokenDuration,
                            RsaKeyUtility.getPrivateKeyFromString(key));
        }
    }

    /**
     * Generates a token for the given subject.
     *
     * @param subject the subject to generate a token for
     * @param roles roles
     */
    public void generateToken(final String subject, final List<String> roles) {
        token = generator.generateToken(subject, roles);
    }

    /**
     * Gets the generated token.
     *
     * @return the generated token
     */
    public String getToken() {
        return token;
    }
}
