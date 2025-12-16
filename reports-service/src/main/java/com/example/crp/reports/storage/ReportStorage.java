package com.example.crp.reports.storage;

import java.net.URI;
import java.util.Optional;

public interface ReportStorage {

    StoredReport put(String keyHint, String fileName, String contentType, byte[] bytes);

    byte[] get(StoredReportRef ref);

    Optional<URI> presignGet(StoredReportRef ref);

    record StoredReport(String storageType,
                        String location,
                        String fileName,
                        String contentType,
                        long sizeBytes,
                        String sha256Hex) {
        public StoredReportRef ref() {
            return new StoredReportRef(storageType, location);
        }
    }

    record StoredReportRef(String storageType, String location) {}
}

