package io.nuvalence.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.platform.configuration.Configuration;
import io.nuvalence.workmanager.client.ApiClient;
import io.nuvalence.workmanager.client.ApiException;
import io.nuvalence.workmanager.client.generated.controllers.AdminApi;
import io.nuvalence.workmanager.client.generated.models.FormConfigurationCreateModel;
import io.nuvalence.workmanager.client.generated.models.FormConfigurationUpdateModel;
import io.nuvalence.workmanager.client.generated.models.ParentSchemas;
import io.nuvalence.workmanager.client.generated.models.SchemaCreateModel;
import io.nuvalence.workmanager.client.generated.models.SchemaModel;
import io.nuvalence.workmanager.client.generated.models.SchemaUpdateModel;
import io.nuvalence.workmanager.client.generated.models.TransactionDefinitionCreateModel;
import io.nuvalence.workmanager.client.generated.models.TransactionDefinitionUpdateModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * Client for interacting with the Work Manager API.
 */
public class WorkManagerClient {
    private static final List<String> roles = List.of("wm:transaction-config-admin");
    private static final String ACCEPTANCE_TEST_USER = "acceptance-test-user";
    private static final String TRANSACTION_DEFINITION_KEY = "TestTransactionDefinition";
    private static final String TRANSACTION_DEFINITION_FORM_KEY = "TestFormConfiguration";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminApi client;
    private final TokenGenerator tokenGenerator;

    /**
     * Creates a new workmanager client.
     *
     * @throws IOException if an error occurs while retrieving the private key
     */
    public WorkManagerClient() throws IOException {
        tokenGenerator = new TokenGenerator();
        tokenGenerator.generateToken(ACCEPTANCE_TEST_USER, roles);

        var apiClient = new ApiClient();
        apiClient.updateBaseUri(Configuration.baseUri + "/api/v1");
        apiClient.setRequestInterceptor(this::applyAuthorization);
        client = new AdminApi(apiClient);
    }

    /**
     * Creates several schemas for the test.
     *
     * @throws ApiException if an error occurs while interacting with the API
     */
    public void createTestSchemas() throws ApiException {
        createTestSchema("default/default-test-schema.json");
        createTestSchema("default/nestedschemas/neighbor.json");
        createTestSchema("default/nestedschemas/grandchild.json");
        createTestSchema("default/nestedschemas/parent.json");
        createTestSchema("default/nestedschemas/grandparent.json");
    }

    /**
     * Creates a schema for the test.
     *
     * @param filePath the path to the schema file
     * @throws ApiException if an error occurs while interacting with the API
     */
    public void createTestSchema(String filePath) throws ApiException {
        try {
            SchemaModel schema = getObject(filePath, SchemaModel.class);

            SchemaCreateModel schemaCreateModel = new SchemaCreateModel();
            schemaCreateModel.setName(schema.getName());
            schemaCreateModel.setKey(schema.getKey());
            schemaCreateModel.setAttributes(schema.getAttributes());

            try {
                client.createSchema(schemaCreateModel);
            } catch (ApiException e) {
                if (e.getMessage().contains("call failed with: 409")) {
                    SchemaUpdateModel schemaUpdateModel = new SchemaUpdateModel();
                    schemaUpdateModel.setName(schema.getName());
                    schemaUpdateModel.setAttributes(schema.getAttributes());
                    client.updateSchema(schema.getKey(), schemaUpdateModel);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new ApiException("Failed to send schema creation request: " + e.getMessage());
        }
    }

    /**
     * Call getSchemaParents endpoint and verify that the result is as expected.
     *
     * @throws ApiException if an error occurs while interacting with the API
     */
    public void getSchemasParents() throws ApiException {
        ParentSchemas neighborParents = client.getSchemaParents("Neighbor");
        checkParents("Neighbor", neighborParents, List.of());

        ParentSchemas grandChildParents = client.getSchemaParents("GrandChild");
        checkParents("GrandChild", grandChildParents, List.of("Parent", "GrandParent"));

        ParentSchemas parentParents = client.getSchemaParents("Parent");
        checkParents("Parent", parentParents, List.of("GrandParent"));

        ParentSchemas grandParentParents = client.getSchemaParents("GrandParent");
        checkParents("GrandParent", grandParentParents, List.of());
    }

    private void checkParents(
            String key, ParentSchemas neighborParents, List<String> expectedParents) {
        List<String> actualParents = neighborParents.getParentSchemas();
        if (actualParents.size() != expectedParents.size()
                || !actualParents.containsAll(expectedParents)) {
            if (!actualParents.equals(expectedParents)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Expected parents %s for schema %s, but got %s",
                                expectedParents, key, actualParents));
            }
        }
    }

    /**
     * Creates a transaction definition for the test.
     *
     * @throws ApiException if an error occurs while interacting with the API
     */
    public void createTestTransactionDefinition() throws ApiException {
        try {
            TransactionDefinitionCreateModel transactionDefinition =
                    getObject(
                            "default/default-test-transaction-definition.json",
                            TransactionDefinitionCreateModel.class);
            transactionDefinition.setKey(TRANSACTION_DEFINITION_KEY);

            try {
                client.postTransactionDefinition(transactionDefinition);
            } catch (ApiException e) {
                if (e.getMessage().contains("call failed with: 409")) {
                    TransactionDefinitionUpdateModel transactionDefinitionUpdateModel =
                            getObject(
                                    "default/default-test-transaction-definition.json",
                                    TransactionDefinitionUpdateModel.class);

                    client.putTransactionDefinition(
                            TRANSACTION_DEFINITION_KEY, transactionDefinitionUpdateModel);

                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            throw new ApiException(
                    "Failed to send transaction definition creation request: " + e.getMessage());
        }
    }

    /**
     * Creates a form configuration for the test.
     *
     * @throws ApiException if an error occurs while interacting with the API
     */
    public void createTestTransactionDefinitionForm() throws ApiException {
        try {
            FormConfigurationCreateModel formConfigurationCreateModel =
                    getObject(
                            "default/default-test-transaction-definition-form.json",
                            FormConfigurationCreateModel.class);
            formConfigurationCreateModel.setKey(TRANSACTION_DEFINITION_FORM_KEY);

            try {
                client.postFormConfiguration(
                        TRANSACTION_DEFINITION_KEY, formConfigurationCreateModel);
            } catch (ApiException e) {
                if (e.getMessage().contains("call failed with: 409")) {
                    FormConfigurationUpdateModel formConfigurationUpdateRequest =
                            getObject(
                                    "default/default-test-transaction-definition-form.json",
                                    FormConfigurationUpdateModel.class);

                    client.putFormConfiguration(
                            TRANSACTION_DEFINITION_KEY,
                            TRANSACTION_DEFINITION_FORM_KEY,
                            formConfigurationUpdateRequest);
                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            throw new ApiException(
                    "Failed to send form configuration creation request: " + e.getMessage());
        }
    }

    private void applyAuthorization(HttpRequest.Builder request) {
        if (tokenGenerator.getToken() != null) {
            request.header("Authorization", "Bearer " + tokenGenerator.getToken());
        }
    }

    private <T> T getObject(String clazzPath, Class<T> valueType) {
        try (InputStream res = this.getClass().getResourceAsStream(clazzPath)) {
            return objectMapper.readValue(res, valueType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
