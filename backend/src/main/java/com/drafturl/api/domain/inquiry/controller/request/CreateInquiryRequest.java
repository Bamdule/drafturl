package com.drafturl.api.domain.inquiry.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequest(
        @NotBlank(message = "문의 유형을 선택해주세요")
        String type,

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        String name,

        @NotBlank(message = "제목을 입력해주세요")
        String subject,

        @NotBlank(message = "내용을 입력해주세요")
        @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
        String message,

        String documentId
) {}
