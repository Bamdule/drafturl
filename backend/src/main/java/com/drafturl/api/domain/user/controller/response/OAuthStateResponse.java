package com.drafturl.api.domain.user.controller.response;

/**
 * OAuth2 state 생성 응답.
 *
 * @param state HMAC 서명된 OAuth2 state 값
 */
public record OAuthStateResponse(String state) {
}
