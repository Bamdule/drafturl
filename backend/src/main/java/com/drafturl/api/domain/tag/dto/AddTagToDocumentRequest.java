package com.drafturl.api.domain.tag.dto;

import jakarta.validation.constraints.NotNull;

public record AddTagToDocumentRequest(
        @NotNull Long tagId
) {
}
