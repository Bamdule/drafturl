package com.drafturl.api.global.auth;

import com.drafturl.api.domain.user.PlanType;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private static final long ACCESS_EXPIRY = 3600000L;
    private static final long REFRESH_EXPIRY = 604800000L;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
        jwtProvider = new JwtProvider(properties);
    }

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("유효한 Access Token을 생성한다")
        void createsValidToken() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createAccessToken(userId, "test@email.com", PlanType.FREE);

            Claims claims = jwtProvider.validateAndGetClaims(token);
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("Claims에 userId, email, plan이 포함된다")
        void containsExpectedClaims() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createAccessToken(userId, "test@email.com", PlanType.STARTER);

            Claims claims = jwtProvider.validateAndGetClaims(token);
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo("test@email.com");
            assertThat(claims.get("plan", String.class)).isEqualTo("STARTER");
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("유효한 Refresh Token을 생성한다")
        void createsValidToken() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createRefreshToken(userId);

            Claims claims = jwtProvider.validateAndGetClaims(token);
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("Claims에 userId만 포함된다")
        void containsOnlyUserId() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createRefreshToken(userId);

            Claims claims = jwtProvider.validateAndGetClaims(token);
            assertThat(claims.get("email")).isNull();
            assertThat(claims.get("plan")).isNull();
        }
    }

    @Nested
    @DisplayName("validateAndGetClaims")
    class ValidateAndGetClaims {

        @Test
        @DisplayName("유효한 토큰의 Claims를 반환한다")
        void returnsClaimsForValidToken() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createAccessToken(userId, "test@email.com", PlanType.FREE);

            Claims claims = jwtProvider.validateAndGetClaims(token);

            assertThat(claims).isNotNull();
        }

        @Test
        @DisplayName("만료된 토큰은 null을 반환한다")
        void returnsNullForExpiredToken() {
            JwtProperties shortExpiry = new JwtProperties(SECRET, 1L, 1L);
            JwtProvider shortProvider = new JwtProvider(shortExpiry);

            UUID userId = UUID.randomUUID();
            String token = shortProvider.createAccessToken(userId, "test@email.com", PlanType.FREE);

            // 1ms 만료이므로 즉시 만료됨
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            Claims claims = jwtProvider.validateAndGetClaims(token);
            assertThat(claims).isNull();
        }

        @Test
        @DisplayName("잘못된 서명의 토큰은 null을 반환한다")
        void returnsNullForInvalidSignature() {
            JwtProperties otherProperties = new JwtProperties(
                    "other-secret-key-must-be-at-least-256-bits-long-for-hs256-algo!", ACCESS_EXPIRY, REFRESH_EXPIRY);
            JwtProvider otherProvider = new JwtProvider(otherProperties);

            String token = otherProvider.createAccessToken(UUID.randomUUID(), "test@email.com", PlanType.FREE);

            Claims claims = jwtProvider.validateAndGetClaims(token);
            assertThat(claims).isNull();
        }

        @Test
        @DisplayName("형식이 잘못된 토큰은 null을 반환한다")
        void returnsNullForMalformedToken() {
            Claims claims = jwtProvider.validateAndGetClaims("not-a-jwt-token");
            assertThat(claims).isNull();
        }
    }

    @Nested
    @DisplayName("extractUserPrincipal")
    class ExtractUserPrincipal {

        @Test
        @DisplayName("Claims에서 UserPrincipal을 올바르게 추출한다")
        void extractsCorrectly() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createAccessToken(userId, "test@email.com", PlanType.PRO);
            Claims claims = jwtProvider.validateAndGetClaims(token);

            UserPrincipal principal = jwtProvider.extractUserPrincipal(claims);

            assertThat(principal.userId()).isEqualTo(userId);
            assertThat(principal.email()).isEqualTo("test@email.com");
            assertThat(principal.plan()).isEqualTo(PlanType.PRO);
        }

        @Test
        @DisplayName("plan이 없으면 FREE를 기본값으로 사용한다")
        void defaultsToFreeWhenPlanMissing() {
            UUID userId = UUID.randomUUID();
            String token = jwtProvider.createRefreshToken(userId);
            Claims claims = jwtProvider.validateAndGetClaims(token);

            UserPrincipal principal = jwtProvider.extractUserPrincipal(claims);

            assertThat(principal.plan()).isEqualTo(PlanType.FREE);
        }
    }

    @Nested
    @DisplayName("expiry getters")
    class ExpiryGetters {

        @Test
        @DisplayName("Access Token 만료 시간을 반환한다")
        void accessTokenExpiry() {
            assertThat(jwtProvider.getAccessTokenExpiry()).isEqualTo(ACCESS_EXPIRY);
        }

        @Test
        @DisplayName("Refresh Token 만료 시간을 반환한다")
        void refreshTokenExpiry() {
            assertThat(jwtProvider.getRefreshTokenExpiry()).isEqualTo(REFRESH_EXPIRY);
        }
    }
}
