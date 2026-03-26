package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentViewResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentExpiredException;
import com.drafturl.api.domain.document.exception.DocumentGoneException;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 공개 문서 서빙 유스케이스.
 * 상태/만료 확인 후 CDN URL을 생성하여 반환한다.
 */
@Component
public class ViewDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final String cdnBaseUrl;

    public ViewDocumentUseCase(DocumentRepository documentRepository,
                               @Value("${app.cdn.base-url}") String cdnBaseUrl) {
        this.documentRepository = documentRepository;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public DocumentViewResponse execute(String slug) {
        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new DocumentNotFoundException(slug));

        if (document.getStatus() != DocumentStatus.ACTIVE) {
            switch (document.getStatus()) {
                case EXPIRED -> throw new DocumentExpiredException(slug);
                case DELETED -> throw new DocumentGoneException(slug);
                default -> throw new DocumentNotFoundException(slug);
            }
        }

        if (document.getExpiresAt() != null && document.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new DocumentExpiredException(slug);
        }

        return DocumentViewResponse.from(document, cdnBaseUrl);
    }
}
