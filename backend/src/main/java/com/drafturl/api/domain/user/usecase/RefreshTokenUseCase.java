package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.user.controller.response.AuthResponse;
import com.drafturl.api.domain.user.entity.RefreshToken;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.service.AuthTransactionService;
import com.drafturl.api.global.auth.JwtProvider;
import com.drafturl.api.global.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 토큰 갱신 유스케이스.
 * Refresh Token Rotation 적용: 기존 토큰 revoke → 새 토큰 발급.
 */
@Component
public class RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCase.class);

    private final AuthTransactionService txService;
    private final JwtProvider jwtProvider;

    public RefreshTokenUseCase(AuthTransactionService txService, JwtProvider jwtProvider) {
        this.txService = txService;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthResponse execute(String refreshTokenValue) {
        RefreshToken refreshToken = txService.findRefreshToken(refreshTokenValue);

        if (refreshToken.isRevoked()) {
            log.warn("Refresh Token reuse detected for userId={}", refreshToken.getUserId());
            txService.revokeAllTokens(refreshToken.getUserId());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_REUSE_DETECTED",
                    "이미 무효화된 Refresh Token이 재사용되었습니다");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                    "Refresh Token이 만료되었습니다");
        }

        refreshToken.revoke();

        User user = txService.findUser(refreshToken.getUserId());

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getPlan());
        String newRefreshTokenValue = jwtProvider.createRefreshToken(user.getId());
        long expiryMillis = jwtProvider.getRefreshTokenExpiry();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiryMillis / 1000);
        txService.saveRefreshToken(user.getId(), newRefreshTokenValue, expiresAt);

        long expiresInSeconds = jwtProvider.getAccessTokenExpiry() / 1000;
        return AuthResponse.of(accessToken, newRefreshTokenValue, expiresInSeconds, user);
    }
}
