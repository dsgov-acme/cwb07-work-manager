package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowAction;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.models.RelatedPartyModel;
import io.nuvalence.workmanager.service.generated.models.TransactionModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowActionModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowTaskModel;
import io.nuvalence.workmanager.service.service.TransactionTaskService;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Maps transactions between the following 2 forms.
 *
 * <ul>
 *     <li>API Model ({@link io.nuvalence.workmanager.service.generated.models.TransactionModel})</li>
 *     <li>Logic/Persistence Model ({@link io.nuvalence.workmanager.service.domain.transaction.Transaction})</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public abstract class TransactionMapper {
    @Autowired @Setter protected EntityMapper entityMapper;
    @Autowired @Setter protected TransactionTaskService transactionTaskService;
    @Autowired @Setter protected CustomerProvidedDocumentMapper customerProvidedDocumentMapper;
    @Autowired @Setter protected RelatedPartyMapper relatedPartyMapper;

    @Mapping(
            target = "data",
            expression = "java(entityMapper.convertAttributesToGenericMap(transaction.getData()))")
    @Mapping(target = "activeTasks", expression = "java(populateActiveTasks(transaction))")
    @Mapping(
            target = "customerProvidedDocuments",
            expression =
                    "java(customerProvidedDocumentMapper.mapAndFilterCustomerProvidedDocuments("
                            + "transaction.getCustomerProvidedDocuments()))")
    @Mapping(
            target = "transactionDefinitionName",
            expression = "java(transaction.getTransactionDefinition().getName())")
    @Mapping(target = "priority", expression = "java(mapTransactionPriority(transaction))")
    @Mapping(
            target = "isComplete",
            expression = "java(transactionTaskService.hasReachedEndEvent(transaction))")
    @Mapping(target = "subjectProfileId", expression = "java(transaction.getSubjectProfileId())")
    @Mapping(
            target = "subjectProfileType",
            expression = "java(transaction.getSubjectProfileType().getValue())")
    @Mapping(
            target = "additionalParties",
            expression = "java(mapAdditionalParties(transaction.getAdditionalParties()))")
    public abstract TransactionModel transactionToTransactionModel(Transaction transaction);

    String mapTransactionPriority(Transaction transaction) {
        if (Objects.isNull(transaction.getPriority())) {
            return null;
        }
        return transaction.getPriority().getValue();
    }

    List<WorkflowTaskModel> populateActiveTasks(Transaction transaction) {
        return transactionTaskService.getActiveTasksForCurrentUser(transaction).stream()
                .map(this::mapWorkflowTaskToWorkflowTaskModel)
                .toList();
    }

    public abstract WorkflowTaskModel mapWorkflowTaskToWorkflowTaskModel(WorkflowTask workflowTask);

    public abstract WorkflowActionModel mapWorkflowActionToWorkflowActionModel(
            WorkflowAction workflowAction);

    List<RelatedPartyModel> mapAdditionalParties(List<RelatedParty> relatedPartyList) {
        List<RelatedPartyModel> result = new ArrayList<>();
        relatedPartyList.forEach(party -> result.add(relatedPartyMapper.entityToModel(party)));

        return result;
    }
}
