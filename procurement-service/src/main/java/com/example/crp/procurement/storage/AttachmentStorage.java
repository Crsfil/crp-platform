package com.example.crp.procurement.storage;

import java.net.URI;
import java.util.Optional;

public interface AttachmentStorage {

    StoredObject put(String keyHint, String fileName, String contentType, byte[] bytes);

    byte[] get(StoredObjectRef ref);

    Optional<URI> presignGet(StoredObjectRef ref);

    record StoredObject(String storageType,
                        String location,
                        String fileName,
                        String contentType,
                        long sizeBytes,
                        String sha256Hex) {
        public StoredObjectRef ref() {
            return new StoredObjectRef(storageType, location);
        }
    }

    record StoredObjectRef(String storageType, String location) {}
}

