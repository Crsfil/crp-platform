package com.example.crp.inventory.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_error_at")
    private OffsetDateTime lastErrorAt;

    public UUID getId() { return id; }

    public String getAggregateType() { return aggregateType; }

    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public Long getAggregateId() { return aggregateId; }

    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }

    public String getEventType() { return eventType; }

    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTopic() { return topic; }

    public void setTopic(String topic) { this.topic = topic; }

    public String getPayload() { return payload; }

    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public Integer getRetryCount() { return retryCount; }

    public void incrementRetry() { this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1; }

    public String getLastError() { return lastError; }

    public void setLastError(String lastError) { this.lastError = lastError; }

    public OffsetDateTime getLastErrorAt() { return lastErrorAt; }

    public void setLastErrorAt(OffsetDateTime lastErrorAt) { this.lastErrorAt = lastErrorAt; }
}

