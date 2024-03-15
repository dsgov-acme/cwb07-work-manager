package io.nuvalence.workmanager.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.generated.models.RecordResponseModel;
import io.nuvalence.workmanager.service.mapper.RecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class RecordAccessResourceTranslatorTest {

    private RecordAccessResourceTranslator translator;

    @Mock private RecordMapper mapper;

    @Mock private ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        translator = new RecordAccessResourceTranslator();
        translator.setApplicationContext(applicationContext);
    }

    @Test
    void testTranslate_ShouldReturnTranslatedObject() {
        Record record = new Record();
        RecordResponseModel expectedModel = new RecordResponseModel();
        when(applicationContext.getBean(RecordMapper.class)).thenReturn(mapper);
        when(mapper.recordToRecordResponseModel(record)).thenReturn(expectedModel);

        Object result = translator.translate(record);

        assertEquals(expectedModel, result);
        verify(applicationContext, times(1)).getBean(RecordMapper.class);
        verify(mapper, times(1)).recordToRecordResponseModel(record);
    }

    @Test
    void testTranslate_ShouldReturnOriginalObject() {
        Object resource = new Object();

        Object result = translator.translate(resource);

        assertEquals(resource, result);
        verify(applicationContext, never()).getBean((String) any());
        verify(mapper, never()).recordToRecordResponseModel(any());
    }
}
