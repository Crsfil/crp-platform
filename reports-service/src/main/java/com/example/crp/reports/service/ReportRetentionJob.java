package com.example.crp.reports.service;

import com.example.crp.reports.repo.ReportJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ReportRetentionJob {
    private static final List<String> TERMINAL_STATUSES = List.of("DONE", "FAILED", "CANCELED");

    private final ReportJobRepository repository;
    private final int retentionDays;

    public ReportRetentionJob(ReportJobRepository repository,
                              @Value("${reports.retention.days:60}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${reports.retention.poll-ms:3600000}")
    @Transactional
    public void cleanup() {
        if (retentionDays <= 0) {
            return;
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        repository.deleteByStatusInAndCreatedAtBefore(TERMINAL_STATUSES, cutoff);
    }
}
