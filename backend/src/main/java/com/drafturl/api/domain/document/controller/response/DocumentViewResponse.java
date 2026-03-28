package com.drafturl.api.domain.document.controller.response;

import com.drafturl.api.domain.document.entity.Document;

import java.time.LocalDateTime;

public record DocumentViewResponse(
        String id,
        String title,
        String docType,
        String contentUrl,
        boolean isPasswordProtected,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentViewResponse from(Document document, String cdnBaseUrl) {
        String ext = document.getDocType().name().toLowerCase().equals("html") ? "html" : "md";
        String contentUrl = cdnBaseUrl + "/documents/" + document.getSlug() + "/content." + ext;
        return new DocumentViewResponse(
                document.getId(),
                document.getTitle(),
                document.getDocType().name().toLowerCase(),
                contentUrl,
                document.isPasswordProtected(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public static DocumentViewResponse protectedMetadata(Document document) {
        return new DocumentViewResponse(
                document.getId(),
                document.getTitle(),
                document.getDocType().name().toLowerCase(),
                null,
                true,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
