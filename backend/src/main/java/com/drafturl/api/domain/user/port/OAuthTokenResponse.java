package com.drafturl.api.domain.user.port;

/**
 * OAuth2 Provider로부터 받은 토큰 응답.
 *
 * @param accessToken OAuth2 Access Token
 * @param tokenType   토큰 타입 (일반적으로 "Bearer")
 * @param scope       부여된 스코프
 */
public record OAuthTokenResponse(
        String accessToken,
        String tokenType,
        String scope
) {
}
