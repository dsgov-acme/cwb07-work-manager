package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowAction;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelResponse;
import io.nuvalence.workmanager.service.generated.models.TransactionModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowActionModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowTaskModel;
import io.nuvalence.workmanager.service.service.TransactionTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
class TransactionMapperTest {

    private Transaction transaction;
    private TransactionModel model;
    private TransactionMapper mapper;
    private DynamicEntity entity;

    @Mock private TransactionTaskService transactionTaskService;

    @BeforeEach
    void setup() {
        final UUID subjectUserId = UUID.randomUUID();
        final Schema addressSchema =
                Schema.builder()
                        .name("Address")
                        .property("line1", String.class)
                        .property("line2", String.class)
                        .property("city", String.class)
                        .property("state", String.class)
                        .property("postalCode", String.class)
                        .build();
        final Schema emailAddressSchema =
                Schema.builder()
                        .name("EmailAddress")
                        .property("type", String.class)
                        .property("email", String.class)
                        .build();
        final Schema contactSchema =
                Schema.builder()
                        .name("Contact")
                        .property("name", String.class)
                        .property("address", addressSchema)
                        .property("emails", List.class, emailAddressSchema)
                        .property("tags", List.class, String.class)
                        .build();
        entity = new DynamicEntity(contactSchema);
        entity.set("name", "Thomas A. Anderson");
        entity.set("tags", List.of("tag1", "tag2"));
        final DynamicEntity address = new DynamicEntity(addressSchema);
        address.set("line1", "123 Street St");
        address.set("city", "New York");
        address.set("state", "NY");
        address.set("postalCode", "11111");
        entity.set("address", address);
        final DynamicEntity emailAddress1 = new DynamicEntity(emailAddressSchema);
        emailAddress1.set("type", "work");
        emailAddress1.set("email", "tanderson@nuvalence.io");
        entity.add("emails", emailAddress1);

        CustomerProvidedDocumentModelResponse customerProvidedDocumentModel =
                new CustomerProvidedDocumentModelResponse();
        customerProvidedDocumentModel.setId(
                UUID.fromString("8f41d20f-a0cc-4b5d-b1d7-9a4aaf7314f9"));
        customerProvidedDocumentModel.dataPath("randomSchema");
        customerProvidedDocumentModel.setReviewStatus(ReviewStatus.NEW.getValue());
        customerProvidedDocumentModel.setActive(true);
        customerProvidedDocumentModel.setTransaction(
                UUID.fromString("c3148d70-9b43-11ec-96c7-0242ac120003"));
        customerProvidedDocumentModel.setRejectionReasons(new ArrayList<>());

        CustomerProvidedDocument customerProvidedDocument =
                CustomerProvidedDocument.builder()
                        .id(UUID.fromString("8f41d20f-a0cc-4b5d-b1d7-9a4aaf7314f9"))
                        .transactionId(UUID.fromString("c3148d70-9b43-11ec-96c7-0242ac120003"))
                        .dataPath("randomSchema")
                        .reviewStatus(ReviewStatus.NEW)
                        .active(true)
                        .build();

        CustomerProvidedDocument inactiveCustomerProvidedDocument =
                CustomerProvidedDocument.builder()
                        .id(UUID.fromString("5fbd01cf-7711-4b69-b07c-5a68b4ae9c48"))
                        .transactionId(UUID.fromString("c3148d70-9b43-11ec-96c7-0242ac120003"))
                        .dataPath("randomSchema")
                        .reviewStatus(ReviewStatus.NEW)
                        .active(false)
                        .build();

        transaction =
                Transaction.builder()
                        .transactionDefinitionKey("test")
                        .transactionDefinitionId(UUID.randomUUID())
                        .processInstanceId("process-id")
                        .createdBy("Dummy user")
                        .assignedTo("Dummy Agent")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .status("new")
                        .priority(TransactionPriority.MEDIUM)
                        .district("DISTRICT1")
                        .data(entity)
                        .customerProvidedDocuments(
                                List.of(customerProvidedDocument, inactiveCustomerProvidedDocument))
                        .subjectProfileId(subjectUserId)
                        .subjectProfileType(ProfileType.INDIVIDUAL)
                        .additionalParties(Collections.emptyList())
                        .build();
        ReflectionTestUtils.setField(
                transaction, "transactionDefinition", new TransactionDefinition());
        transaction.getTransactionDefinition().setName("testingName");

        model =
                new TransactionModel()
                        .transactionDefinitionKey("test")
                        .transactionDefinitionId(transaction.getTransactionDefinitionId())
                        .transactionDefinitionName("testingName")
                        .processInstanceId("process-id")
                        .createdBy("Dummy user")
                        .assignedTo("Dummy Agent")
                        .priority(TransactionPriority.MEDIUM.getValue())
                        .district("DISTRICT1")
                        .createdTimestamp(transaction.getCreatedTimestamp())
                        .lastUpdatedTimestamp(transaction.getLastUpdatedTimestamp())
                        .status("new")
                        .putDataItem("attribute", "value")
                        .isComplete(false)
                        .activeTasks(
                                List.of(
                                        new WorkflowTaskModel()
                                                .key("active-task")
                                                .name("Active Task")
                                                .actions(
                                                        List.of(
                                                                new WorkflowActionModel()
                                                                        .key("Complete")
                                                                        .uiLabel("Complete")
                                                                        .uiClass("PRIMARY")
                                                                        .modalContext(
                                                                                "complete")))))
                        .subjectProfileId(subjectUserId)
                        .subjectProfileType("INDIVIDUAL")
                        .additionalParties(Collections.emptyList())
                        .customerProvidedDocuments(List.of(customerProvidedDocumentModel))
                        .recordLinks(List.of());
        mapper = Mappers.getMapper(TransactionMapper.class);
        final EntityMapper entityMapper = Mappers.getMapper(EntityMapper.class);
        final CustomerProvidedDocumentMapper customerProvidedDocumentMapper =
                Mappers.getMapper(CustomerProvidedDocumentMapper.class);
        mapper.setEntityMapper(entityMapper);
        mapper.setTransactionTaskService(transactionTaskService);
        mapper.setCustomerProvidedDocumentMapper(customerProvidedDocumentMapper);
        model.setData(entityMapper.convertAttributesToGenericMap(entity));
    }

    @Test
    void transactionToTransactionModel() {
        when(transactionTaskService.getActiveTasksForCurrentUser(transaction))
                .thenReturn(
                        Collections.singletonList(
                                WorkflowTask.builder()
                                        .key("active-task")
                                        .name("Active Task")
                                        .action(
                                                WorkflowAction.builder()
                                                        .key("Complete")
                                                        .uiLabel("Complete")
                                                        .uiClass("PRIMARY")
                                                        .modalContext("complete")
                                                        .build())
                                        .build()));
        when(transactionTaskService.hasReachedEndEvent(transaction)).thenReturn(false);

        assertEquals(model, mapper.transactionToTransactionModel(transaction));
    }
}
