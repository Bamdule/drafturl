package com.drafturl.api.domain.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.]+$", message = "태그 이름에 허용되지 않은 문자가 포함되어 있습니다")
        String name
) {
}
