package com.example.crp.inventory.storage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

public class FileSystemDocumentStorage implements DocumentStorage {

    private final Path baseDir;

    public FileSystemDocumentStorage(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public StoredObject put(String keyHint, String fileName, String contentType, byte[] bytes) {
        String hint = (keyHint == null || keyHint.isBlank()) ? "doc" : keyHint;
        String ts = OffsetDateTime.now().toInstant().toString().replace(":", "").replace(".", "");
        String safeName = (fileName == null || fileName.isBlank()) ? "file.bin" : fileName;
        String key = "inventory/" + hint + "/" + ts + "/" + UUID.randomUUID() + "-" + safeName;
        Path dest = baseDir.resolve(key);
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, bytes);
            return new StoredObject("FILESYSTEM", dest.toString(), safeName, contentType, bytes.length, sha256(bytes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file to filesystem", e);
        }
    }

    @Override
    public byte[] get(StoredObjectRef ref) {
        if (ref == null || ref.location() == null) {
            throw new IllegalArgumentException("Missing storage ref");
        }
        try {
            return Files.readAllBytes(Path.of(ref.location()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored file", e);
        }
    }

    @Override
    public Optional<URI> presignGet(StoredObjectRef ref) {
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

