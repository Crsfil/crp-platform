package com.example.crp.inventory.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class OutboxRetentionJob {
    private static final String STATUS_SENT = "SENT";

    private final OutboxEventRepository repository;
    private final int retentionDays;

    public OutboxRetentionJob(OutboxEventRepository repository,
                              @Value("${outbox.retention.days:30}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${outbox.retention.poll-ms:3600000}")
    @Transactional
    public void cleanup() {
        if (retentionDays <= 0) {
            return;
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        repository.deleteByStatusAndCreatedAtBefore(STATUS_SENT, cutoff);
    }
}
