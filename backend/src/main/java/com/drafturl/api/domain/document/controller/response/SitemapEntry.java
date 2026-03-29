package com.drafturl.api.domain.document.controller.response;

import java.time.LocalDateTime;

public record SitemapEntry(
        String slug,
        LocalDateTime updatedAt
) {
}
