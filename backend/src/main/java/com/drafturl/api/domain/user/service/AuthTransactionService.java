package com.drafturl.api.domain.user.service;

import com.drafturl.api.domain.user.entity.RefreshToken;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.port.OAuthUserInfo;
import com.drafturl.api.domain.user.repository.RefreshTokenRepository;
import com.drafturl.api.domain.user.repository.UserRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuthService의 DB 조작을 트랜잭션 경계로 감싸는 서비스.
 * 외부 OAuth API 호출이 트랜잭션 안에서 실행되지 않도록 분리한다.
 */
@Service
public class AuthTransactionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTransactionService(UserRepository userRepository,
                                   RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * OAuth 사용자 정보로 기존 사용자를 조회하거나 새로 생성한다.
     */
    @Transactional
    public User findOrCreateUser(OAuthUserInfo userInfo, String provider) {
        return userRepository.findByProviderAndProviderId(provider, userInfo.providerId())
                .map(existingUser -> {
                    existingUser.updateProfile(userInfo.name(), userInfo.avatarUrl());
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 같은 이메일이 다른 provider로 이미 등록되어 있는지 확인
                    userRepository.findByEmail(userInfo.email()).ifPresent(existing -> {
                        String existingProvider = existing.getProvider();
                        throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                                String.format("이미 %s 계정으로 가입된 이메일입니다. %s로 로그인해주세요.",
                                        existingProvider, existingProvider));
                    });
                    return userRepository.save(
                            new User(userInfo.email(), userInfo.name(), userInfo.avatarUrl(), provider, userInfo.providerId())
                    );
                });
    }

    /**
     * RefreshToken을 저장한다.
     */
    @Transactional
    public void saveRefreshToken(UUID userId, String tokenValue, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken(userId, tokenValue, expiresAt);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * RefreshToken을 조회한다.
     */
    @Transactional(readOnly = true)
    public RefreshToken findRefreshToken(String tokenValue) {
        return refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                        "유효하지 않은 Refresh Token입니다"));
    }

    /**
     * 사용자를 ID로 조회한다.
     */
    @Transactional(readOnly = true)
    public User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                        "사용자를 찾을 수 없습니다"));
    }

    /**
     * 사용자의 모든 RefreshToken을 무효화한다.
     */
    @Transactional
    public void revokeAllTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
