package io.nuvalence.platform.configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for the functional tests.
 */
@Slf4j
public class Configuration {
    public static String baseUri;
    public static String gcpProjectId;
    public static String issuer;
    public static String secretVersion;
    public static String tokenPrivateKeySecret;

    /**
     * Initializes the configuration.
     *
     * @param isLocal true if running locally, false otherwise
     * @throws IllegalStateException if any required environment variables are missing
     */
    public static void init(boolean isLocal) {
        if (isLocal) {
            baseUri = "http://api.dsgov.test/wm";
            gcpProjectId = "dsgov-dev";
            issuer = "dsgov";
            secretVersion = "latest";
            tokenPrivateKeySecret = "dsgov-self-signed-token-private-key";
        } else {
            baseUri = System.getenv("SERVICE_URI");
            gcpProjectId = System.getenv("GCP_PROJECT_ID");
            issuer = System.getenv("TOKEN_ISSUER");
            secretVersion = System.getenv("TOKEN_PRIVATE_KEY_VERSION");
            tokenPrivateKeySecret = System.getenv("TOKEN_PRIVATE_KEY_SECRET");
        }
        if (gcpProjectId == null
                || issuer == null
                || secretVersion == null
                || tokenPrivateKeySecret == null) {
            throw new IllegalStateException("Missing required environment variables");
        }
    }
}
