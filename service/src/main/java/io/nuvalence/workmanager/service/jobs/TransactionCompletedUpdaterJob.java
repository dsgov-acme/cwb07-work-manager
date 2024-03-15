package io.nuvalence.workmanager.service.jobs;

import io.nuvalence.workmanager.service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Job to update transactions that are completed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Exclude this service from the 'test' profile
public class TransactionCompletedUpdaterJob {

    private final TransactionService transactionService;

    @PostConstruct
    public void init() {
        run(); // run on startup
    }

    @Scheduled(cron = "0 0 12 * * ?") // 12:00 PM every day
    @SchedulerLock(name = "TransactionCompletedUpdaterJob", lockAtLeastFor = "PT10S")
    public void run() {
        int updatedRows = transactionService.updateCompletedTransactions();
        log.info("Marked {} transactions as completed", updatedRows);
    }
}
