package com.example.crp.reports.messaging;

import java.time.OffsetDateTime;

public class Events {
    public record ReportJobDone(Long jobId,
                                String type,
                                String status,
                                String storageType,
                                String storageLocation,
                                String fileName,
                                Long fileSize,
                                String sha256,
                                OffsetDateTime finishedAt,
                                OffsetDateTime snapshotTimestamp,
                                String templateId,
                                String datasetVersion) {}

    public record ReportJobFailed(Long jobId,
                                  String type,
                                  String status,
                                  String errorMessage,
                                  OffsetDateTime finishedAt,
                                  OffsetDateTime snapshotTimestamp,
                                  String templateId,
                                  String datasetVersion) {}
}
