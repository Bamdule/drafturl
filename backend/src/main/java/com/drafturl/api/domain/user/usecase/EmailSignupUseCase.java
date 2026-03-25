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
 * 이메일 회원가입 유스케이스.
 * 이메일 중복 체크 → 비밀번호 해싱 → 사용자 생성 → JWT 발급.
 */
@Component
public class EmailSignupUseCase {

    private final UserRepository userRepository;
    private final AuthTransactionService txService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public EmailSignupUseCase(UserRepository userRepository,
                               AuthTransactionService txService,
                               JwtProvider jwtProvider,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.txService = txService;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse execute(String email, String password, String name) {
        // 이메일 중복 체크
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "이미 사용 중인 이메일입니다");
        }

        // 비밀번호 해싱 + 사용자 생성
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(email, name, encodedPassword, "email", email, true);
        user = userRepository.save(user);

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
