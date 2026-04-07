package com.drafturl.api.domain.document.controller.response;

import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.tag.dto.TagDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record DocumentResponse(
        String id,
        String slug,
        String url,
        String title,
        String docType,
        long contentSize,
        String status,
        boolean isPasswordProtected,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TagDto> tags,
        String preview
) {

    public static DocumentResponse from(Document document, String frontendUrl) {
        return from(document, frontendUrl, Collections.emptyList());
    }

    public static DocumentResponse from(Document document, String frontendUrl, List<TagDto> tags) {
        return new DocumentResponse(
                document.getId(),
                document.getSlug(),
                frontendUrl + "/" + document.getSlug(),
                document.getTitle(),
                document.getDocType().name().toLowerCase(),
                document.getContentSize(),
                document.getStatus().name().toLowerCase(),
                document.isPasswordProtected(),
                document.getExpiresAt(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                tags,
                document.getPreview()
        );
    }
}
