package com.example.crp.billing.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository repository,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.billing.poll-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (events.isEmpty()) {
            return;
        }
        for (OutboxEvent event : events) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
                String key = event.getAggregateId() != null ? event.getAggregateId().toString() : event.getEventType();
                kafkaTemplate.send(event.getTopic(), key, payload);
                event.setStatus("SENT");
            } catch (Exception ex) {
                log.warn("Outbox publish failed for event {}", event.getId(), ex);
                event.incrementRetry();
                event.setLastError(ex.getMessage());
                event.setLastErrorAt(java.time.OffsetDateTime.now());
                event.setStatus("PENDING");
            }
        }
        repository.saveAll(events);
    }
}
