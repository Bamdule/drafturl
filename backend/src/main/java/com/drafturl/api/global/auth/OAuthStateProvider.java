package com.drafturl.api.global.auth;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * HMAC-SHA256 서명 기반 OAuth2 state 파라미터를 생성/검증한다.
 * 서버 측 저장 없이 서명 유효성과 timestamp 만료를 확인하여 CSRF를 방어한다.
 *
 * <p>state 형식: {@code {timestamp}.{nonce}.{signature}}
 * <ul>
 *   <li>timestamp: epoch seconds (발급 시각)</li>
 *   <li>nonce: 16바이트 랜덤 hex 문자열 (재사용 방지)</li>
 *   <li>signature: HMAC-SHA256(timestamp + "." + nonce, secret)의 Base64URL 인코딩</li>
 * </ul>
 */
@Component
public class OAuthStateProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final int NONCE_BYTES = 16;

    private final SecretKeySpec signingKey;
    private final SecureRandom secureRandom;

    public OAuthStateProvider(JwtProperties properties) {
        this.signingKey = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * HMAC 서명된 OAuth2 state 값을 생성한다.
     *
     * @return "{timestamp}.{nonce}.{signature}" 형식의 state 문자열
     */
    public String generateState() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = generateNonce();
        String payload = timestamp + "." + nonce;
        String signature = sign(payload);
        return payload + "." + signature;
    }

    /**
     * state 값의 HMAC 서명 유효성과 timestamp 만료를 검증한다.
     *
     * @param state 검증할 state 문자열
     * @throws BusinessException 서명이 유효하지 않거나 만료된 경우
     */
    public void validateState(String state) {
        if (state == null || state.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_STATE",
                    "OAuth2 state 값이 유효하지 않습니다");
        }

        String[] parts = state.split("\\.", 3);
        if (parts.length != 3) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_STATE",
                    "OAuth2 state 형식이 올바르지 않습니다");
        }

        String timestamp = parts[0];
        String nonce = parts[1];
        String signature = parts[2];

        // 1. 서명 검증
        String payload = timestamp + "." + nonce;
        String expectedSignature = sign(payload);
        if (!constantTimeEquals(expectedSignature, signature)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_STATE",
                    "OAuth2 state 서명이 유효하지 않습니다");
        }

        // 2. timestamp 만료 검증
        try {
            long issuedAt = Long.parseLong(timestamp);
            Instant issuedInstant = Instant.ofEpochSecond(issuedAt);
            if (Instant.now().isAfter(issuedInstant.plus(STATE_TTL))) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "OAUTH_STATE_EXPIRED",
                        "OAuth2 state가 만료되었습니다 (5분 초과)");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_STATE",
                    "OAuth2 state timestamp가 유효하지 않습니다");
        }
    }

    private String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 생성에 실패했습니다", e);
        }
    }

    /**
     * Timing attack 방지를 위한 상수 시간 비교.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
