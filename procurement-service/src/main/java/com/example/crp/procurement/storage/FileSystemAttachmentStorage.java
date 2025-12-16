package com.example.crp.procurement.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

public class FileSystemAttachmentStorage implements AttachmentStorage {

    @Override
    public StoredObject put(String keyHint, String fileName, String contentType, byte[] bytes) {
        try {
            Path dir = Path.of("procurement-attachments");
            Files.createDirectories(dir);
            String safeName = (keyHint == null || keyHint.isBlank()) ? fileName : (keyHint + "-" + fileName);
            Path path = dir.resolve(safeName);
            Files.write(path, bytes);
            return new StoredObject("FILESYSTEM", path.toAbsolutePath().toString(), fileName, contentType, bytes.length, sha256(bytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to write attachment to filesystem", e);
        }
    }

    @Override
    public byte[] get(StoredObjectRef ref) {
        try {
            Path path = Path.of(ref.location());
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Attachment not found: " + ref.location());
            }
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read attachment from filesystem", e);
        }
    }

    @Override
    public Optional<java.net.URI> presignGet(StoredObjectRef ref) {
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

