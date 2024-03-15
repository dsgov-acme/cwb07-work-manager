package io.nuvalence.workmanager.service.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nuvalence.auth.access.cerbos.AccessResourceTranslator;
import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.mapper.RecordMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class RecordAccessResourceTranslator
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
        RecordAccessResourceTranslator.applicationContext = applicationContext;
    }

    @Override
    public Object translate(Object resource) {
        if (resource instanceof Record theRecord) {
            final RecordMapper mapper = applicationContext.getBean(RecordMapper.class);
            return mapper.recordToRecordResponseModel(theRecord);
        }

        return resource;
    }
}
