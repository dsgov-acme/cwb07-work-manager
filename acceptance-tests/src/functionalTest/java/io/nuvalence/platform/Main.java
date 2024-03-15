package io.nuvalence.platform;

import io.nuvalence.platform.configuration.Configuration;
import io.nuvalence.workmanager.client.ApiException;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Main class for running the acceptance test configuration.
 */
public class Main {

    /**
     * Main method for testing.
     *
     * @param args command line arguments
     * @throws IOException if an error occurs while retrieving the private key
     * @throws ApiException if an error occurs while interacting with the API
     * @throws URISyntaxException if an error occurs while parsing the URI for resources
     */
    public static void main(String[] args) throws ApiException, IOException, URISyntaxException {
        Configuration.init(true);

        WorkManagerClient workManagerClient = new WorkManagerClient();
        workManagerClient.createTestSchemas();
        workManagerClient.createTestTransactionDefinition();
        workManagerClient.createTestTransactionDefinitionForm();
        workManagerClient.createTestTransactionDefinitionForm();
        workManagerClient.getSchemasParents();

        CamundaClient camundaClient = new CamundaClient();
        camundaClient.createWorkflow();
    }
}
