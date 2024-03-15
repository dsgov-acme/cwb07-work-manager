package io.nuvalence.workmanager.service.camunda.delegates;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventRequestObjectDto;
import io.nuvalence.workmanager.service.models.auditevents.TransactionSubmittedAuditEventDto;
import io.nuvalence.workmanager.service.service.AuditEventService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.camunda.CamundaPropertiesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Delegate to report audit events regarding transactions being submitted.
 */
@RequiredArgsConstructor
@Component("simpleTransactionAuditEventDelegate")
@Profile("!test")
@Slf4j
public class SimpleTransactionAuditEventDelegate implements JavaDelegate {

    private final AuditEventService transactionAuditEventService;

    private final TransactionService transactionService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        try {
            UUID transactionId = (UUID) execution.getVariable("transactionId");

            final String originatorId = SecurityContextUtility.getAuthenticatedUserId();
            final TransactionSubmittedAuditEventDto transactionSubmittedAuditEventDto =
                    new TransactionSubmittedAuditEventDto(originatorId);

            Optional<Transaction> optionalTransaction =
                    transactionService.getTransactionById(transactionId);
            if (optionalTransaction.isEmpty()) {
                log.warn(
                        "Transaction {} not found, could not post transaction submitted audit"
                                + " event.",
                        transactionId);
                return;
            }

            Optional<String> activityEventType =
                    CamundaPropertiesUtils.getExtensionProperty("activity_event_type", execution);
            Optional<String> optionalTemplateString =
                    CamundaPropertiesUtils.getExtensionProperty("summary", execution);
            if (optionalTemplateString.isEmpty() || activityEventType.isEmpty()) {
                log.warn(
                        "Camunda properties are missing, could not post transaction submitted audit"
                                + " event for {}.",
                        transactionId);
                return;
            }
            String templateString = optionalTemplateString.get();

            Handlebars handlebars = new Handlebars();
            Template template = handlebars.compileInline(templateString);

            Transaction transaction = optionalTransaction.get();

            Map<String, Object> contextData = new HashMap<>();
            contextData.put("transactionId", transactionId);
            contextData.put("externalId", transaction.getExternalId());
            contextData.put("status", transaction.getStatus());

            Context context = Context.newContext(contextData);

            String summary = template.apply(context);

            AuditActivityType auditActivityType =
                    AuditActivityType.fromValue(activityEventType.get());

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(summary)
                            .businessObjectId(transactionId)
                            .businessObjectType(AuditEventBusinessObject.TRANSACTION)
                            .data(transactionSubmittedAuditEventDto.toJson(), auditActivityType)
                            .build();

            transactionAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            log.error("An error occurred when submitting a transaction.", e);
        }
    }
}
