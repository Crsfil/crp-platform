package com.example.crp.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "inventory.documents.storage")
public class InventoryDocumentsStorageProperties {

    /**
     * filesystem | s3
     */
    private String type = "filesystem";

    private final Filesystem filesystem = new Filesystem();
    private final S3 s3 = new S3();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Filesystem getFilesystem() { return filesystem; }
    public S3 getS3() { return s3; }

    public static class Filesystem {
        /**
         * Base directory for stored documents when using filesystem storage.
         */
        private String baseDir;

        public String getBaseDir() { return baseDir; }
        public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
    }

    public static class S3 {
        private String endpoint;
        private String region = "us-east-1";
        private String bucket = "crp-inventory";
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccess = true;
        private Duration presignTtl = Duration.ofMinutes(15);

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public boolean isPathStyleAccess() { return pathStyleAccess; }
        public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
        public Duration getPresignTtl() { return presignTtl; }
        public void setPresignTtl(Duration presignTtl) { this.presignTtl = presignTtl; }
    }
}

