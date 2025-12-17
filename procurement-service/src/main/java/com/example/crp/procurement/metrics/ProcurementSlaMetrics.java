package com.example.crp.procurement.metrics;

import com.example.crp.procurement.repo.ProcurementServiceOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ProcurementSlaMetrics {

    private static final List<String> ACTIVE_EXCLUDED = List.of("COMPLETED", "CANCELED");
    private static final List<String> SERVICE_TYPES = List.of("SERVICE_STORAGE", "SERVICE_VALUATION", "SERVICE_REPAIR", "SERVICE_AUCTION");

    private final ProcurementServiceOrderRepository repository;
    private final Map<String, AtomicInteger> overdueByType = new ConcurrentHashMap<>();

    public ProcurementSlaMetrics(ProcurementServiceOrderRepository repository, MeterRegistry registry) {
        this.repository = repository;
        for (String type : SERVICE_TYPES) {
            AtomicInteger gauge = new AtomicInteger(0);
            overdueByType.put(type, gauge);
            registry.gauge("procurement.service_order.overdue", java.util.List.of(
                    io.micrometer.core.instrument.Tag.of("service_type", type)
            ), gauge);
        }
    }

    @Scheduled(fixedDelayString = "${procurement.metrics.sla.poll-ms:60000}")
    public void refresh() {
        OffsetDateTime now = OffsetDateTime.now();
        for (String type : SERVICE_TYPES) {
            long count = repository.countByServiceTypeAndStatusNotInAndSlaUntilBefore(type, ACTIVE_EXCLUDED, now);
            AtomicInteger gauge = overdueByType.get(type);
            if (gauge != null) {
                gauge.set((int) count);
            }
        }
    }
}
