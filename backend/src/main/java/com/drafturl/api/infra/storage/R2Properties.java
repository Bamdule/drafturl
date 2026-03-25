package com.drafturl.api.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudflare R2 (S3 호환) 스토리지 접속 설정.
 * application.yml의 app.storage 프리픽스에 바인딩된다.
 */
@ConfigurationProperties(prefix = "app.storage")
public record R2Properties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region
) {
}
