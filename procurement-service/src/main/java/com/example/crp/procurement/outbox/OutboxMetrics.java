package com.example.crp.procurement.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxMetrics {
    private static final String STATUS_PENDING = "PENDING";

    private final OutboxEventRepository repository;
    private final AtomicLong pendingCount = new AtomicLong(0);
    private final AtomicLong oldestAgeSeconds = new AtomicLong(0);

    public OutboxMetrics(OutboxEventRepository repository, MeterRegistry registry) {
        this.repository = repository;
        Tags tags = Tags.of("service", "procurement-service");
        registry.gauge("outbox.pending", tags, pendingCount);
        registry.gauge("outbox.oldest.age.seconds", tags, oldestAgeSeconds);
    }

    @Scheduled(fixedDelayString = "${outbox.metrics.poll-ms:60000}")
    public void refresh() {
        pendingCount.set(repository.countByStatus(STATUS_PENDING));
        var oldest = repository.findFirstByStatusOrderByCreatedAtAsc(STATUS_PENDING).orElse(null);
        if (oldest == null) {
            oldestAgeSeconds.set(0);
            return;
        }
        long age = Duration.between(oldest.getCreatedAt(), OffsetDateTime.now()).getSeconds();
        oldestAgeSeconds.set(Math.max(age, 0));
    }
}
