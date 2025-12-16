package com.example.crp.inventory.config;

import com.example.crp.inventory.storage.DocumentStorage;
import com.example.crp.inventory.storage.FileSystemDocumentStorage;
import com.example.crp.inventory.storage.S3DocumentStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(InventoryDocumentsStorageProperties.class)
public class InventoryDocumentsStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "inventory.documents.storage", name = "type", havingValue = "filesystem", matchIfMissing = true)
    public DocumentStorage fileSystemDocumentStorage(InventoryDocumentsStorageProperties props) {
        String baseDir = props.getFilesystem().getBaseDir();
        Path dir = (baseDir == null || baseDir.isBlank())
                ? Path.of(System.getProperty("java.io.tmpdir"), "crp-inventory-documents")
                : Path.of(baseDir);
        return new FileSystemDocumentStorage(dir);
    }

    @Bean
    @ConditionalOnProperty(prefix = "inventory.documents.storage", name = "type", havingValue = "s3")
    public S3Client inventoryS3Client(InventoryDocumentsStorageProperties props) {
        InventoryDocumentsStorageProperties.S3 s3 = props.getS3();
        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
                .endpointOverride(URI.create(s3.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccess()).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "inventory.documents.storage", name = "type", havingValue = "s3")
    public S3Presigner inventoryS3Presigner(InventoryDocumentsStorageProperties props) {
        InventoryDocumentsStorageProperties.S3 s3 = props.getS3();
        return S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
                .endpointOverride(URI.create(s3.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccess()).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "inventory.documents.storage", name = "type", havingValue = "s3")
    public DocumentStorage s3DocumentStorage(InventoryDocumentsStorageProperties props, S3Client s3Client, S3Presigner presigner) {
        return new S3DocumentStorage(s3Client, presigner, props.getS3().getBucket(), props.getS3().getPresignTtl());
    }
}

