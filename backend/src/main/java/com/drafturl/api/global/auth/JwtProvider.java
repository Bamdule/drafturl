package com.drafturl.api.global.auth;

import com.drafturl.api.domain.user.PlanType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 생성, 검증, Claims 추출을 담당한다.
 * HS256 알고리즘과 jjwt 라이브러리를 사용한다.
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private final SecretKey signingKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtProvider(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = properties.accessTokenExpiry();
        this.refreshTokenExpiry = properties.refreshTokenExpiry();
    }

    /**
     * Access Token을 생성한다.
     *
     * @param userId 사용자 ID (sub 클레임)
     * @param email  사용자 이메일
     * @param plan   사용자 요금제
     * @return 서명된 JWT 문자열
     */
    public String createAccessToken(UUID userId, String email, PlanType plan) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiry);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("plan", plan.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Refresh Token을 생성한다.
     * Refresh Token은 sub(userId)만 포함한다.
     *
     * @param userId 사용자 ID
     * @return 서명된 JWT 문자열
     */
    public String createRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiry);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * JWT를 검증하고 Claims를 반환한다.
     * 서명 검증과 만료 확인을 수행한다.
     *
     * @param token JWT 문자열
     * @return 유효한 경우 Claims, 유효하지 않으면 null
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT Claims에서 UserPrincipal을 추출한다.
     *
     * @param claims JWT Claims
     * @return UserPrincipal 객체
     */
    public UserPrincipal extractUserPrincipal(Claims claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String planStr = claims.get("plan", String.class);
        PlanType plan = planStr != null ? PlanType.valueOf(planStr) : PlanType.FREE;

        return new UserPrincipal(userId, email, plan);
    }

    /**
     * Access Token 만료 시간(밀리초)을 반환한다.
     */
    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    /**
     * Refresh Token 만료 시간(밀리초)을 반환한다.
     */
    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }
}
