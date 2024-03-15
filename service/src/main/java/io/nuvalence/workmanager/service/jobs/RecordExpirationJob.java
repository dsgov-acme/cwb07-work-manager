package io.nuvalence.workmanager.service.jobs;

import io.nuvalence.workmanager.service.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service that expires records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordExpirationJob {

    private final RecordService recordService;

    @Scheduled(cron = "0 0 0 * * *", zone = "America/New_York")
    @SchedulerLock(name = "RecordExpirationJob", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void expireRecords() {
        int recordsExpired = recordService.expireRecords();
        log.info("Expired {} records", recordsExpired);
    }
}
