package io.nuvalence.platform;

import io.nuvalence.platform.configuration.Configuration;
import io.nuvalence.workmanager.client.ApiException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client for interacting with the Camunda API.
 */
public class CamundaClient {

    private static final String ACCEPTANCE_TEST_USER = "acceptance-test-user";

    private static final List<String> roles = List.of("wm:transaction-config-admin");

    private static final int HTTP_OK = 200;
    private final Consumer<HttpPost> authInterceptor;
    private final TokenGenerator tokenGenerator;

    CamundaClient() throws IOException {
        tokenGenerator = new TokenGenerator();
        tokenGenerator.generateToken(ACCEPTANCE_TEST_USER, roles);

        authInterceptor =
                (HttpPost httpPost) -> {
                    httpPost.setHeader("Authorization", "Bearer " + tokenGenerator.getToken());
                };
    }

    /**
     * Creates a workflow.
     *
     * @throws ApiException if an error occurs while interacting with the API
     * @throws URISyntaxException if an error occurs while parsing the URI for resources
     */
    public void createWorkflow() throws ApiException, URISyntaxException {
        String fileName = "io/nuvalence/platform/default/default-test-workflow.bpmn";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(fileName);
        File file = new File(resource.toURI());

        createDeployment(true, true, "TestWorkflow", OffsetDateTime.now(), file);
    }

    /**
     * Creates a deployment.
     * @param deployChangedOnly If engine should perform duplicate checking on a per-resource basis.
     * @param enableDuplicateFiltering If process engine should perform duplicate checking for the deployment.
     * @param deploymentName The name for the deployment to be created.
     * @param deploymentActivationTime Sets the date on which this deployment will be activated.
     * @param data The binary data to create the deployment resource.
     * @throws ApiException if an error occurs while interacting with the API
     */
    private void createDeployment(
            Boolean deployChangedOnly,
            Boolean enableDuplicateFiltering,
            String deploymentName,
            OffsetDateTime deploymentActivationTime,
            File data)
            throws ApiException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.addTextBody("deployment-source", "configuration-deployer");
            entityBuilder.addTextBody("deploy-changed-only", deployChangedOnly.toString());
            entityBuilder.addTextBody(
                    "enable-duplicate-filtering", enableDuplicateFiltering.toString());
            entityBuilder.addTextBody("deployment-name", deploymentName);
            entityBuilder.addTextBody(
                    "deployment-activation-time", deploymentActivationTime.toString());
            entityBuilder.addPart("data", new FileBody(data, ContentType.DEFAULT_BINARY));

            HttpEntity entity = entityBuilder.build();
            HttpPost httpPost =
                    new HttpPost(
                            URI.create(Configuration.baseUri + "/engine-rest/deployment/create"));
            httpPost.setEntity(entity);

            if (authInterceptor != null) {
                authInterceptor.accept(httpPost);
            }

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HTTP_OK) {
                    throw new ApiException("HTTP request failed with status code: " + statusCode);
                }
            } catch (IOException e) {
                throw new ApiException("Failed to send the HTTP request: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new ApiException("Failed to create or close the HTTP client: " + e.getMessage());
        }
    }
}
