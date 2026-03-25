package com.drafturl.api.domain.user.port;

/**
 * OAuth2 Provider로부터 조회한 사용자 정보.
 *
 * @param providerId Provider 측 사용자 고유 ID
 * @param email      사용자 이메일
 * @param name       사용자 이름 (표시명)
 * @param avatarUrl  프로필 이미지 URL (nullable)
 */
public record OAuthUserInfo(
        String providerId,
        String email,
        String name,
        String avatarUrl
) {
}
