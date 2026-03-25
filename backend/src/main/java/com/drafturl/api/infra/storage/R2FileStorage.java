package com.drafturl.api.infra.storage;

import com.drafturl.api.domain.document.port.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Cloudflare R2 파일 저장소 어댑터.
 * FileStorage 포트를 구현하며, AWS S3 SDK를 사용하여 R2에 접근한다.
 */
@Component
public class R2FileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(R2FileStorage.class);

    private final S3Client s3Client;
    private final String bucketName;

    public R2FileStorage(S3Client s3Client, R2Properties properties) {
        this.s3Client = s3Client;
        this.bucketName = properties.bucket();
    }

    @Override
    public void upload(String key, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            log.debug("R2 업로드 완료: key={}, size={}, contentType={}", key, content.length, contentType);
        } catch (S3Exception e) {
            log.error("R2 업로드 실패: key={}", key, e);
            throw new StorageException("파일 업로드에 실패했습니다: " + key, e);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("R2 파일 없음: key={}", key);
            throw new StorageException("파일을 찾을 수 없습니다: " + key, e);
        } catch (S3Exception e) {
            log.error("R2 다운로드 실패: key={}", key, e);
            throw new StorageException("파일 다운로드에 실패했습니다: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.debug("R2 삭제 완료: key={}", key);
        } catch (S3Exception e) {
            log.error("R2 삭제 실패: key={}", key, e);
            throw new StorageException("파일 삭제에 실패했습니다: " + key, e);
        }
    }
}
