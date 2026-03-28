package com.drafturl.api.domain.document.controller.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequest(
        @NotBlank(message = "비밀번호를 입력해주세요")
        String password
) {
}
