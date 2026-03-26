package com.drafturl.api.infra.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

/**
 * Cloudflare R2용 S3Client 빈 설정.
 * R2는 S3 호환 API를 제공하므로 AWS SDK S3Client를 그대로 사용한다.
 * 로컬(MinIO) 환경에서는 버킷이 없으면 자동 생성한다.
 */
@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {

    private static final Logger log = LoggerFactory.getLogger(R2Config.class);

    @Bean
    public S3Client s3Client(R2Properties properties) {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        ensureBucketExists(client, properties.bucket());
        return client;
    }

    private void ensureBucketExists(S3Client client, String bucket) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Storage bucket '{}' exists", bucket);
        } catch (NoSuchBucketException e) {
            log.error("Storage bucket '{}' not found. Please create it in the Cloudflare R2 dashboard.", bucket);
            throw new StorageException("Bucket '" + bucket + "' does not exist");
        } catch (Exception e) {
            log.warn("Could not verify bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
