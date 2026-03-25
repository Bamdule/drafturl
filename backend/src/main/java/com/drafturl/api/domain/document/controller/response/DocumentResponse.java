package com.drafturl.api.domain.document.controller.response;

import com.drafturl.api.domain.document.entity.Document;

import java.time.LocalDateTime;

public record DocumentResponse(
        String id,
        String slug,
        String url,
        String title,
        String docType,
        long contentSize,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentResponse from(Document document, String frontendUrl) {
        return new DocumentResponse(
                document.getId(),
                document.getSlug(),
                frontendUrl + "/" + document.getSlug(),
                document.getTitle(),
                document.getDocType().name().toLowerCase(),
                document.getContentSize(),
                document.getStatus().name().toLowerCase(),
                document.getExpiresAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
