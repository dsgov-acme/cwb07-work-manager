package io.nuvalence.workmanager.service.controllers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.workmanager.service.utils.UserUtility;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EnumsApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthorizationHandler authorizationHandler;

    @Test
    void getAllEnumerationsTest() throws Exception {

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            mockMvc.perform(get("/api/v1/enumerations"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.document-review-statuses").exists())
                    .andExpect(jsonPath("$.note-types").exists())
                    .andExpect(jsonPath("$.transaction-priorities").exists())
                    .andExpect(jsonPath("$.schema-attribute-types").exists())
                    .andExpect(jsonPath("$.document-rejection-reasons").exists())
                    .andExpect(jsonPath("$.profile-access-levels").exists())
                    .andExpect(jsonPath("$.transaction-record-link-types").exists())
                    .andExpect(jsonPath("$.user-types").exists())
                    .andExpect(jsonPath("$.[*][*].label").exists())
                    .andExpect(jsonPath("$.[*][*].value").exists())
                    .andExpect(jsonPath("$.transaction-priorities[*].rank").exists())
                    .andExpect(jsonPath("$.transaction-priorities.length()").value(4))
                    .andExpect(jsonPath("$.profile-access-levels.length()").value(3))
                    .andExpect(
                            jsonPath(
                                    "$.transaction-priorities[*].rank",
                                    everyItem(instanceOf(Integer.class))));
        }
    }

    @Test
    void getEnumerationByIdTest_ReviewStatus() throws Exception {
        String enumerationId = "document-review-statuses";

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");

            mockMvc.perform(get("/api/v1/enumerations/" + enumerationId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$..label").exists())
                    .andExpect(jsonPath("$..value").exists())
                    .andExpect(
                            jsonPath(
                                    "$[*].value",
                                    containsInAnyOrder("NEW", "ACCEPTED", "REJECTED")));
        }
    }

    @Test
    void getEnumerationByIdTest_ProfileAccessLevelPublicUser() throws Exception {
        String enumerationId = "profile-access-levels";

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            mockMvc.perform(get("/api/v1/enumerations/" + enumerationId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$..label").exists())
                    .andExpect(jsonPath("$..value").exists())
                    .andExpect(
                            jsonPath(
                                    "$[*].value", containsInAnyOrder("ADMIN", "WRITER", "READER")));
        }
    }

    @Test
    void getEnumerationByIdTest_ProfileAccessLevelAgencyUser() throws Exception {
        String enumerationId = "profile-access-levels";

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            mockMvc.perform(get("/api/v1/enumerations/" + enumerationId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$..label").exists())
                    .andExpect(jsonPath("$..value").exists())
                    .andExpect(
                            jsonPath(
                                    "$[*].value",
                                    containsInAnyOrder(
                                            "ADMIN", "WRITER", "READER", "AGENCY_READONLY")));
        }
    }

    @Test
    void getEnumerationByIdTest_TransactionRecordLinkTypes() throws Exception {
        String enumerationId = "transaction-record-link-types";

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");

            mockMvc.perform(get("/api/v1/enumerations/" + enumerationId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$..label").exists())
                    .andExpect(jsonPath("$..value").exists())
                    .andExpect(jsonPath("$[*].value", containsInAnyOrder("CREATED", "UPDATED")));
        }
    }

    @Test
    void getEnumerationByIdTest_Priorities() throws Exception {
        String enumerationId = "transaction-priorities";

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            mockMvc.perform(get("/api/v1/enumerations/" + enumerationId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$..label").exists())
                    .andExpect(jsonPath("$..value").exists())
                    .andExpect(jsonPath("$..rank").exists())
                    .andExpect(jsonPath("$[*].rank", everyItem(instanceOf(Integer.class))));
        }
    }

    @Test
    void getEnumerationByIdNotFoundTest() throws Exception {
        String enumerationId = "non-existent-enum";

        mockMvc.perform(get("/api/v1/enumerations/" + enumerationId))
                .andExpect(status().isNotFound());
    }
}
