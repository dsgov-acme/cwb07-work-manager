package io.nuvalence.workmanager.service.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nuvalence.auth.access.cerbos.AccessResourceTranslator;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.mapper.TransactionMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Transaction Translator Class.
 */
@Component
public class TransactionAccessResourceTranslator
        implements AccessResourceTranslator, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    @SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification =
                    "This is an established pattern for exposing spring state to static contexts."
                        + " The applicationContext is a singleton, so if this write were to occur"
                        + " multiple times, it would be idempotent.")
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        TransactionAccessResourceTranslator.applicationContext = applicationContext;
    }

    @Override
    public Object translate(Object resource) {
        if (resource instanceof Transaction transaction) {
            final TransactionMapper mapper = applicationContext.getBean(TransactionMapper.class);
            return mapper.transactionToTransactionModel(transaction);
        }

        return resource;
    }
}
