package io.nuvalence.workmanager.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.models.TransactionModel;
import io.nuvalence.workmanager.service.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

class TransactionAccessResourceTranslatorTest {
    private TransactionAccessResourceTranslator translator;

    @Mock private TransactionMapper mapper;

    @Mock private ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        translator = new TransactionAccessResourceTranslator();
        translator.setApplicationContext(applicationContext);
    }

    @Test
    void testTranslate_withTransaction_shouldReturnTranslatedObject() {
        // Arrange
        Transaction transaction = new Transaction();
        TransactionModel expectedModel = new TransactionModel();
        when(applicationContext.getBean(TransactionMapper.class)).thenReturn(mapper);
        when(mapper.transactionToTransactionModel(transaction)).thenReturn(expectedModel);

        // Act
        Object result = translator.translate(transaction);

        // Assert
        assertEquals(expectedModel, result);
        verify(applicationContext, times(1)).getBean(TransactionMapper.class);
        verify(mapper, times(1)).transactionToTransactionModel(transaction);
    }

    @Test
    void testTranslate_withNonTransaction_shouldReturnOriginalObject() {
        // Arrange
        Object resource = new Object();

        // Act
        Object result = translator.translate(resource);

        // Assert
        assertEquals(resource, result);
        verify(applicationContext, never()).getBean((String) any());
        verify(mapper, never()).transactionToTransactionModel(any());
    }
}
