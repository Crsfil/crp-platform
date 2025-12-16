package com.example.crp.reports.config;

import com.example.crp.reports.storage.FileSystemReportStorage;
import com.example.crp.reports.storage.ReportStorage;
import com.example.crp.reports.storage.S3ReportStorage;
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

@Configuration
@EnableConfigurationProperties(ReportsStorageProperties.class)
public class ReportsStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "reports.storage", name = "type", havingValue = "filesystem", matchIfMissing = true)
    public ReportStorage fileSystemReportStorage() {
        return new FileSystemReportStorage();
    }

    @Bean
    @ConditionalOnProperty(prefix = "reports.storage", name = "type", havingValue = "s3")
    public S3Client reportsS3Client(ReportsStorageProperties props) {
        ReportsStorageProperties.S3 s3 = props.getS3();
        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
                .endpointOverride(URI.create(s3.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccess()).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "reports.storage", name = "type", havingValue = "s3")
    public S3Presigner reportsS3Presigner(ReportsStorageProperties props) {
        ReportsStorageProperties.S3 s3 = props.getS3();
        return S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
                .endpointOverride(URI.create(s3.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccess()).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "reports.storage", name = "type", havingValue = "s3")
    public ReportStorage s3ReportStorage(ReportsStorageProperties props, S3Client s3Client, S3Presigner presigner) {
        return new S3ReportStorage(s3Client, presigner, props.getS3().getBucket(), props.getS3().getPresignTtl());
    }
}

