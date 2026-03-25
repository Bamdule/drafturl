package com.drafturl.api.domain.user.port;

/**
 * OAuth2 Provider 인터페이스.
 * Google, GitHub 등 외부 OAuth2 제공자와의 통신을 추상화한다.
 * 도메인 계층에서 정의하고, infra 계층에서 구현한다 (Port-Adapter 패턴).
 */
public interface OAuth2Provider {

    /**
     * Provider 이름을 반환한다.
     *
     * @return "google" 또는 "github"
     */
    String getProviderName();

    /**
     * Authorization Code를 Access Token으로 교환한다.
     *
     * @param code        OAuth2 인가 코드
     * @param redirectUri 콜백 Redirect URI
     * @return 토큰 응답
     */
    OAuthTokenResponse exchangeCode(String code, String redirectUri);

    /**
     * Access Token으로 사용자 정보를 조회한다.
     *
     * @param accessToken OAuth2 Access Token
     * @return 사용자 정보
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
