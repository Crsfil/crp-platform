package com.example.crp.procurement.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

public class S3AttachmentStorage implements AttachmentStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration presignTtl;

    public S3AttachmentStorage(S3Client s3, S3Presigner presigner, String bucket, Duration presignTtl) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.presignTtl = presignTtl == null ? Duration.ofMinutes(15) : presignTtl;
    }

    @Override
    public StoredObject put(String keyHint, String fileName, String contentType, byte[] bytes) {
        String key = buildKey(keyHint, fileName);
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3.putObject(req, RequestBody.fromBytes(bytes));
        return new StoredObject("S3", bucket + "/" + key, fileName, contentType, bytes.length, sha256(bytes));
    }

    @Override
    public byte[] get(StoredObjectRef ref) {
        BucketKey bk = parse(ref.location());
        GetObjectRequest req = GetObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build();
        return s3.getObjectAsBytes(req).asByteArray();
    }

    @Override
    public Optional<URI> presignGet(StoredObjectRef ref) {
        BucketKey bk = parse(ref.location());
        GetObjectRequest get = GetObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(presignTtl)
                .getObjectRequest(get)
                .build();
        return Optional.of(URI.create(presigner.presignGetObject(presignReq).url().toString()));
    }

    private static String buildKey(String keyHint, String fileName) {
        String hint = (keyHint == null || keyHint.isBlank()) ? "attachment" : keyHint;
        String ts = OffsetDateTime.now().toInstant().toString().replace(":", "").replace(".", "");
        return "procurement/" + hint + "/" + ts + "/" + fileName;
    }

    private static BucketKey parse(String location) {
        int idx = location.indexOf('/');
        if (idx <= 0 || idx == location.length() - 1) {
            throw new IllegalArgumentException("Invalid S3 location: " + location);
        }
        return new BucketKey(location.substring(0, idx), location.substring(idx + 1));
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record BucketKey(String bucket, String key) {}
}

