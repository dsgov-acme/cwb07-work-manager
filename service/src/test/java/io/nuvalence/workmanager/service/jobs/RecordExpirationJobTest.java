package io.nuvalence.workmanager.service.jobs;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.service.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordExpirationJobTest {
    private RecordExpirationJob recordExpirationService;

    @Mock private RecordService recordService;

    @BeforeEach
    void setUp() {
        recordExpirationService = new RecordExpirationJob(recordService);
    }

    @Test
    void expireRecordsTest() {
        int expectedExpiredRecords = 5;
        when(recordService.expireRecords()).thenReturn(expectedExpiredRecords);

        recordExpirationService.expireRecords();

        verify(recordService, times(1)).expireRecords();
    }
}
