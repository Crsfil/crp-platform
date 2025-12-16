package com.example.crp.inventory.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String aggregateType, Long aggregateId, String topic, String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setTopic(topic);
        event.setEventType(eventType);
        event.setStatus("PENDING");
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize outbox payload", e);
        }
        event.setLastError(null);
        event.setLastErrorAt(null);
        repository.save(event);
    }

    @Transactional
    public void markFailed(OutboxEvent event, Exception ex) {
        event.setStatus("PENDING");
        event.incrementRetry();
        event.setLastError(ex.getMessage());
        event.setLastErrorAt(OffsetDateTime.now());
        repository.save(event);
    }
}

