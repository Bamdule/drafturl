package com.drafturl.api.domain.document.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank(message = "문서 내용을 입력해주세요")
        @ContentSize
        String content,

        @NotBlank(message = "문서 타입을 지정해주세요")
        String type,

        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        String title
) {
}
