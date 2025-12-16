package com.example.crp.reports.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

public class FileSystemReportStorage implements ReportStorage {

    @Override
    public StoredReport put(String keyHint, String fileName, String contentType, byte[] bytes) {
        try {
            Path dir = Path.of("reports");
            Files.createDirectories(dir);
            String safeName = (keyHint == null || keyHint.isBlank()) ? fileName : (keyHint + "-" + fileName);
            Path path = dir.resolve(safeName);
            Files.write(path, bytes);
            String sha = sha256(bytes);
            return new StoredReport("FILESYSTEM", path.toAbsolutePath().toString(), fileName, contentType, bytes.length, sha);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write report to filesystem", e);
        }
    }

    @Override
    public byte[] get(StoredReportRef ref) {
        try {
            Path path = Path.of(ref.location());
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Report file not found: " + ref.location());
            }
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read report from filesystem", e);
        }
    }

    @Override
    public Optional<java.net.URI> presignGet(StoredReportRef ref) {
        return Optional.empty();
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

