package com.example.crp.reports.messaging;

import com.example.crp.reports.domain.ReportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReportsEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;

    public ReportsEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 @Value("${reports.events.enabled:true}") boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    public void publishDone(ReportJob job) {
        if (!enabled) return;
        Events.ReportJobDone event = new Events.ReportJobDone(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getStorageType(),
                job.getStorageLocation(),
                job.getFileName(),
                job.getFileSize(),
                job.getSha256(),
                job.getFinishedAt(),
                job.getSnapshotTimestamp(),
                job.getTemplateId(),
                job.getDatasetVersion()
        );
        send("reports.job.done", job.getId(), event);
    }

    public void publishFailed(ReportJob job) {
        if (!enabled) return;
        Events.ReportJobFailed event = new Events.ReportJobFailed(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getErrorMessage(),
                job.getFinishedAt(),
                job.getSnapshotTimestamp(),
                job.getTemplateId(),
                job.getDatasetVersion()
        );
        send("reports.job.failed", job.getId(), event);
    }

    private void send(String topic, Long jobId, Object payload) {
        try {
            String key = jobId == null ? "job" : jobId.toString();
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception ex) {
            log.warn("Failed to publish report event to {}", topic, ex);
        }
    }
}
