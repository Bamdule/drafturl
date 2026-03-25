package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.user.controller.response.AuthResponse;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.port.OAuth2Provider;
import com.drafturl.api.domain.user.port.OAuthTokenResponse;
import com.drafturl.api.domain.user.port.OAuthUserInfo;
import com.drafturl.api.domain.user.service.AuthTransactionService;
import com.drafturl.api.global.auth.JwtProvider;
import com.drafturl.api.global.auth.OAuthStateProvider;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth2 로그인 유스케이스.
 * 외부 API 호출(트랜잭션 밖) → 사용자 조회/생성(트랜잭션) → JWT 발급.
 */
@Component
public class OAuthLoginUseCase {

    private final Map<String, OAuth2Provider> providerMap;
    private final AuthTransactionService txService;
    private final JwtProvider jwtProvider;
    private final OAuthStateProvider oAuthStateProvider;

    public OAuthLoginUseCase(List<OAuth2Provider> providers,
                              AuthTransactionService txService,
                              JwtProvider jwtProvider,
                              OAuthStateProvider oAuthStateProvider) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(OAuth2Provider::getProviderName, Function.identity()));
        this.txService = txService;
        this.jwtProvider = jwtProvider;
        this.oAuthStateProvider = oAuthStateProvider;
    }

    public AuthResponse execute(String provider, String code, String redirectUri, String state) {
        // HMAC 서명 검증 + timestamp 만료 확인
        oAuthStateProvider.validateState(state);

        OAuth2Provider oAuth2Provider = providerMap.get(provider);
        if (oAuth2Provider == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETERS",
                    "지원하지 않는 OAuth2 Provider입니다: " + provider);
        }

        // 1. 외부 API 호출 (트랜잭션 밖)
        OAuthTokenResponse tokenResponse = oAuth2Provider.exchangeCode(code, redirectUri);
        OAuthUserInfo userInfo = oAuth2Provider.getUserInfo(tokenResponse.accessToken());

        // 2. DB 조작 (트랜잭션 안)
        User user = txService.findOrCreateUser(userInfo, provider);

        // 3. JWT 생성 + RefreshToken 저장
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
