package com.drafturl.api.domain.document.controller.request;

import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @ContentSize
        String content,

        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        String title,

        @Size(max = 100, message = "비밀번호는 100자를 초과할 수 없습니다")
        String password
) {
}
