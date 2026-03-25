package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.user.controller.response.AuthResponse;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.repository.UserRepository;
import com.drafturl.api.domain.user.service.AuthTransactionService;
import com.drafturl.api.global.auth.JwtProvider;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이메일 로그인 유스케이스.
 * 이메일 존재 확인 + 비밀번호 검증 → JWT 발급.
 */
@Component
public class EmailLoginUseCase {

    private final UserRepository userRepository;
    private final AuthTransactionService txService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public EmailLoginUseCase(UserRepository userRepository,
                              AuthTransactionService txService,
                              JwtProvider jwtProvider,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.txService = txService;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse execute(String email, String password) {
        // 이메일로 사용자 조회 (provider="email"인 사용자만)
        User user = userRepository.findByProviderAndProviderId("email", email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // JWT 발급
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getPlan());
        String refreshTokenValue = jwtProvider.createRefreshToken(user.getId());
        long refreshTokenExpiryMillis = jwtProvider.getRefreshTokenExpiry();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiryMillis / 1000);
        txService.saveRefreshToken(user.getId(), refreshTokenValue, expiresAt);
        long expiresInSeconds = jwtProvider.getAccessTokenExpiry() / 1000;
        return AuthResponse.of(accessToken, refreshTokenValue, expiresInSeconds, user);
    }
}
