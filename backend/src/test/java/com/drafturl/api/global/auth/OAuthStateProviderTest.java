package com.drafturl.api.global.auth;

import com.drafturl.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthStateProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";

    private OAuthStateProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, 3600000L, 604800000L);
        provider = new OAuthStateProvider(properties);
    }

    @Nested
    @DisplayName("generateState")
    class GenerateState {

        @Test
        @DisplayName("timestamp.nonce.signature 형식의 state를 생성한다")
        void generatesValidFormat() {
            String state = provider.generateState();

            String[] parts = state.split("\\.");
            assertThat(parts).hasSize(3);

            // timestamp는 현재 시각 근처의 epoch seconds
            long timestamp = Long.parseLong(parts[0]);
            long now = Instant.now().getEpochSecond();
            assertThat(timestamp).isBetween(now - 5, now + 5);

            // nonce는 32자 hex 문자열 (16바이트)
            assertThat(parts[1]).hasSize(32).matches("[0-9a-f]+");

            // signature는 비어있지 않음
            assertThat(parts[2]).isNotBlank();
        }

        @Test
        @DisplayName("매번 다른 state를 생성한다")
        void generatesDifferentValues() {
            String state1 = provider.generateState();
            String state2 = provider.generateState();

            assertThat(state1).isNotEqualTo(state2);
        }
    }

    @Nested
    @DisplayName("validateState")
    class ValidateState {

        @Test
        @DisplayName("유효한 state는 예외 없이 통과한다")
        void validStatePassesValidation() {
            String state = provider.generateState();

            provider.validateState(state); // 예외가 발생하지 않으면 성공
        }

        @Test
        @DisplayName("null state는 INVALID_OAUTH_STATE 예외를 던진다")
        void nullStateThrowsException() {
            assertThatThrownBy(() -> provider.validateState(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_OAUTH_STATE");
        }

        @Test
        @DisplayName("빈 문자열 state는 INVALID_OAUTH_STATE 예외를 던진다")
        void blankStateThrowsException() {
            assertThatThrownBy(() -> provider.validateState(""))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_OAUTH_STATE");
        }

        @Test
        @DisplayName("형식이 잘못된 state는 INVALID_OAUTH_STATE 예외를 던진다")
        void malformedStateThrowsException() {
            assertThatThrownBy(() -> provider.validateState("invalid-state"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_OAUTH_STATE");
        }

        @Test
        @DisplayName("서명이 변조된 state는 INVALID_OAUTH_STATE 예외를 던진다")
        void tamperedSignatureThrowsException() {
            String state = provider.generateState();
            // 서명 부분을 변조
            String tampered = state.substring(0, state.lastIndexOf('.')) + ".TAMPERED_SIGNATURE";

            assertThatThrownBy(() -> provider.validateState(tampered))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_OAUTH_STATE");
        }

        @Test
        @DisplayName("타임스탬프가 변조된 state는 INVALID_OAUTH_STATE 예외를 던진다")
        void tamperedTimestampThrowsException() {
            String state = provider.generateState();
            String[] parts = state.split("\\.", 3);
            // 타임스탬프를 변경하면 서명이 불일치
            String tampered = "9999999999" + "." + parts[1] + "." + parts[2];

            assertThatThrownBy(() -> provider.validateState(tampered))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_OAUTH_STATE");
        }

        @Test
        @DisplayName("만료된 state는 OAUTH_STATE_EXPIRED 예외를 던진다")
        void expiredStateThrowsException() {
            // 6분 전 타임스탬프로 state를 수동 생성
            long expiredTimestamp = Instant.now().getEpochSecond() - 360;
            // OAuthStateProvider 내부의 sign 메서드에 직접 접근할 수 없으므로,
            // 다른 secret의 provider로 만료 테스트를 하는 대신
            // reflection 또는 별도 helper를 사용해야 하지만,
            // 여기서는 만료된 state가 서명 검증을 통과할 수 있도록 별도 provider를 사용한다
            OAuthStateProvider expiredProvider = new ExpiredStateTestHelper(SECRET);
            String expiredState = expiredProvider.generateState();

            assertThatThrownBy(() -> provider.validateState(expiredState))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "OAUTH_STATE_EXPIRED");
        }

        @Test
        @DisplayName("다른 secret으로 생성한 state는 INVALID_OAUTH_STATE 예외를 던진다")
        void differentSecretThrowsException() {
            JwtProperties otherProperties = new JwtProperties(
                    "other-secret-key-must-be-at-least-256-bits-long-for-hs256-algo", 3600000L, 604800000L);
            OAuthStateProvider otherProvider = new OAuthStateProvider(otherProperties);
            String otherState = otherProvider.generateState();

            assertThatThrownBy(() -> provider.validateState(otherState))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_OAUTH_STATE");
        }
    }

    /**
     * 만료 테스트를 위해 6분 전 타임스탬프로 state를 생성하는 헬퍼.
     * 같은 secret을 사용하므로 서명은 유효하지만 타임스탬프가 만료된다.
     */
    private static class ExpiredStateTestHelper extends OAuthStateProvider {

        ExpiredStateTestHelper(String secret) {
            super(new JwtProperties(secret, 3600000L, 604800000L));
        }

        @Override
        public String generateState() {
            // 부모의 generateState()를 호출한 뒤 타임스탬프만 6분 전으로 교체
            // 하지만 서명을 다시 해야 하므로 reflection이 필요하다.
            // 대안: 직접 HMAC을 계산한다
            long expiredTimestamp = Instant.now().getEpochSecond() - 360;
            String nonce = "abcdef0123456789abcdef0123456789";
            String payload = expiredTimestamp + "." + nonce;

            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(
                        "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "HmacSHA256");
                mac.init(key);
                byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String signature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
                return payload + "." + signature;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
