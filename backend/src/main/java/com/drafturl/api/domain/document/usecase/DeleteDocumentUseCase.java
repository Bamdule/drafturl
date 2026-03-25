package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.controller.response.DocumentDeleteResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.global.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 문서 삭제 유스케이스 (soft delete).
 * DB soft delete 먼저 → R2 삭제 나중에. R2 실패 시 스케줄러가 정리.
 */
@Component
public class DeleteDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteDocumentUseCase.class);

    private final DocumentRepository documentRepository;
    private final DocumentTransactionService txService;
    private final FileStorage fileStorage;

    public DeleteDocumentUseCase(DocumentRepository documentRepository,
                                  DocumentTransactionService txService,
                                  FileStorage fileStorage) {
        this.documentRepository = documentRepository;
        this.txService = txService;
        this.fileStorage = fileStorage;
    }

    public DocumentDeleteResponse execute(String slug, UUID userId) {
        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new DocumentNotFoundException(slug));
        verifyOwnership(document, userId);

        Document deleted = txService.softDeleteDocument(document.getId(), userId, document.getContentSize());

        try {
            fileStorage.delete(document.getR2Key());
        } catch (Exception e) {
            log.warn("R2 파일 삭제 실패 (스케줄러가 정리 예정): slug={}", slug, e);
        }

        return new DocumentDeleteResponse(deleted.getId(), deleted.getSlug(), deleted.getUpdatedAt());
    }

    private void verifyOwnership(Document document, UUID userId) {
        if (document.getUserId() == null || !document.getUserId().equals(userId)) {
            throw new ForbiddenException("문서 소유자가 아닌 사용자의 접근입니다");
        }
    }
}
