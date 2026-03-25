package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentViewResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentExpiredException;
import com.drafturl.api.domain.document.exception.DocumentGoneException;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 공개 문서 서빙 유스케이스.
 * 상태/만료 확인 후 R2에서 콘텐츠를 로드하여 반환한다.
 */
@Component
public class ViewDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;

    public ViewDocumentUseCase(DocumentRepository documentRepository, FileStorage fileStorage) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
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

        byte[] contentBytes = fileStorage.download(document.getR2Key());
        String content = new String(contentBytes, StandardCharsets.UTF_8);

        return DocumentViewResponse.from(document, content);
    }
}
