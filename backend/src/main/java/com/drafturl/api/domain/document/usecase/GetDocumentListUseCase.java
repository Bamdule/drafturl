package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentListResponse;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 내 문서 목록 조회 유스케이스.
 */
@Component
public class GetDocumentListUseCase {

    private final DocumentRepository documentRepository;
    private final String frontendUrl;

    public GetDocumentListUseCase(DocumentRepository documentRepository,
                                   @Value("${app.frontend-url}") String frontendUrl) {
        this.documentRepository = documentRepository;
        this.frontendUrl = frontendUrl;
    }

    public DocumentListResponse execute(UUID userId, int page, int size) {
        Page<Document> documentPage = documentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, DocumentStatus.ACTIVE, PageRequest.of(page, Math.min(size, 50)));

        List<DocumentResponse> documents = documentPage.getContent().stream()
                .map(doc -> DocumentResponse.from(doc, frontendUrl))
                .toList();

        return new DocumentListResponse(documents, new DocumentListResponse.PaginationInfo(
                documentPage.getNumber(), documentPage.getSize(),
                documentPage.getTotalElements(), documentPage.getTotalPages()));
    }
}
