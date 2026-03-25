package com.drafturl.api.domain.document.controller.response;

import java.time.LocalDateTime;

public record DocumentDeleteResponse(
        String id,
        String slug,
        LocalDateTime deletedAt
) {
}
