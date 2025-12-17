package com.example.crp.inventory.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);
    java.util.Optional<OutboxEvent> findFirstByStatusOrderByCreatedAtAsc(String status);
    long deleteByStatusAndCreatedAtBefore(String status, java.time.OffsetDateTime cutoff);
}
