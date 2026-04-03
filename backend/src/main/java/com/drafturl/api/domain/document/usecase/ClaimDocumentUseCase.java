package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게스트 문서 이관 유스케이스.
 * 비로그인으로 생성된 문서(userId=null)를 로그인 사용자에게 이관한다.
 */
@Component
public class ClaimDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentTransactionService txService;

    public ClaimDocumentUseCase(DocumentRepository documentRepository,
                                DocumentTransactionService txService) {
        this.documentRepository = documentRepository;
        this.txService = txService;
    }

    public void execute(String slug, UUID userId) {
        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new DocumentNotFoundException(slug));

        // ACTIVE 상태가 아니면 404 (삭제/만료된 문서는 존재하지 않는 것으로 처리)
        if (document.getStatus() != DocumentStatus.ACTIVE) {
            throw new DocumentNotFoundException(slug);
        }

        // 이미 소유자가 있으면 409 CONFLICT
        if (document.getUserId() != null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "DOCUMENT_ALREADY_OWNED",
                    "이미 소유자가 있는 문서입니다");
        }

        // 만료된 문서면 410 GONE
        if (document.getExpiresAt() != null && document.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    HttpStatus.GONE, "DOCUMENT_EXPIRED",
                    "만료된 문서는 이관할 수 없습니다");
        }

        txService.claimDocument(document.getId(), userId, document.getContentSize());
    }
}
