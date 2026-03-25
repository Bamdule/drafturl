package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.controller.response.DocumentEditResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.global.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 편집용 문서 상세 조회 유스케이스.
 * 소유권 확인 후 R2에서 콘텐츠를 로드한다.
 */
@Component
public class GetDocumentForEditUseCase {

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;
    private final String frontendUrl;

    public GetDocumentForEditUseCase(DocumentRepository documentRepository,
                                      FileStorage fileStorage,
                                      @Value("${app.frontend-url}") String frontendUrl) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.frontendUrl = frontendUrl;
    }

    public DocumentEditResponse execute(String slug, UUID userId) {
        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new DocumentNotFoundException(slug));
        verifyOwnership(document, userId);

        byte[] contentBytes = fileStorage.download(document.getR2Key());
        String content = new String(contentBytes, StandardCharsets.UTF_8);

        return DocumentEditResponse.from(document, content, frontendUrl);
    }

    private void verifyOwnership(Document document, UUID userId) {
        if (document.getUserId() == null || !document.getUserId().equals(userId)) {
            throw new ForbiddenException("문서 소유자가 아닌 사용자의 접근입니다");
        }
    }
}
