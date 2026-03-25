package com.drafturl.api.domain.document.controller.response;

import com.drafturl.api.domain.document.entity.Document;

import java.time.LocalDateTime;

public record DocumentEditResponse(
        String id,
        String slug,
        String url,
        String title,
        String docType,
        String content,
        long contentSize,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentEditResponse from(Document document, String content, String frontendUrl) {
        return new DocumentEditResponse(
                document.getId(),
                document.getSlug(),
                frontendUrl + "/" + document.getSlug(),
                document.getTitle(),
                document.getDocType().name().toLowerCase(),
                content,
                document.getContentSize(),
                document.getStatus().name().toLowerCase(),
                document.getExpiresAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
