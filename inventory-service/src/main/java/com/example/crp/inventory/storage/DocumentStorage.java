package com.example.crp.inventory.storage;

import java.net.URI;
import java.util.Optional;

public interface DocumentStorage {

    StoredObject put(String keyHint, String fileName, String contentType, byte[] bytes);

    byte[] get(StoredObjectRef ref);

    Optional<URI> presignGet(StoredObjectRef ref);

    record StoredObject(String storageType,
                        String location,
                        String fileName,
                        String contentType,
                        long sizeBytes,
                        String sha256Hex) {}

    record StoredObjectRef(String storageType, String location) {}
}

