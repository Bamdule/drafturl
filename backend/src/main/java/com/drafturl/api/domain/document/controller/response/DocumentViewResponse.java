package com.drafturl.api.domain.document.controller.response;

import com.drafturl.api.domain.document.entity.Document;

import java.time.LocalDateTime;

public record DocumentViewResponse(
        String id,
        String title,
        String docType,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentViewResponse from(Document document, String content) {
        return new DocumentViewResponse(
                document.getId(),
                document.getTitle(),
                document.getDocType().name().toLowerCase(),
                content,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
