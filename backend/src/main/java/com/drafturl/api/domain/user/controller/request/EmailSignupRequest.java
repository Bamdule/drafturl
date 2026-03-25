package com.drafturl.api.domain.user.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailSignupRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다")
        String password,

        @NotBlank(message = "이름은 필수입니다")
        String name
) {
}
