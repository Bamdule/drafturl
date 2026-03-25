package com.drafturl.api.global.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 프로퍼티.
 * application.yml의 app.jwt 프리픽스에 바인딩된다.
 *
 * @param secret             HS256 서명 비밀키 (최소 256비트)
 * @param accessTokenExpiry  Access Token 만료 시간 (밀리초)
 * @param refreshTokenExpiry Refresh Token 만료 시간 (밀리초)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiry,
        long refreshTokenExpiry
) {
}
