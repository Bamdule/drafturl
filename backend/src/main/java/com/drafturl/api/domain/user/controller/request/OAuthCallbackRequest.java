package com.drafturl.api.domain.user.controller.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthCallbackRequest(
        @NotBlank(message = "OAuth2 인가 코드는 필수입니다")
        String code,

        @NotBlank(message = "리다이렉트 URI는 필수입니다")
        String redirectUri,

        @NotBlank(message = "OAuth2 state 값은 필수입니다")
        String state
) {
}
