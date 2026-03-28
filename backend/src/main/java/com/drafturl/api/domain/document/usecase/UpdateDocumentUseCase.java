package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.controller.request.UpdateDocumentRequest;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.ContentTooLargeException;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.port.ContentSanitizer;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.global.exception.BusinessException;
import com.drafturl.api.global.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 문서 수정 유스케이스.
 * 소유권 확인 → 새니타이징 → R2 덮어쓰기 → DB UPDATE.
 */
@Component
public class UpdateDocumentUseCase {

    private static final long MAX_CONTENT_SIZE = 5L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final ContentSanitizer contentSanitizer;
    private final FileStorage fileStorage;
    private final DocumentTransactionService txService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;

    public UpdateDocumentUseCase(DocumentRepository documentRepository,
                                  ContentSanitizer contentSanitizer,
                                  FileStorage fileStorage,
                                  DocumentTransactionService txService,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.frontend-url}") String frontendUrl) {
        this.documentRepository = documentRepository;
        this.contentSanitizer = contentSanitizer;
        this.fileStorage = fileStorage;
        this.txService = txService;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
    }

    public DocumentResponse execute(String slug, UpdateDocumentRequest request, UUID userId) {
        boolean hasContent = request.content() != null && !request.content().isEmpty();
        boolean hasTitle = request.title() != null;
        boolean hasPassword = request.password() != null;
        if (!hasContent && !hasTitle && !hasPassword) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EMPTY_UPDATE",
                    "수정할 내용이 없습니다. content 또는 title 중 하나는 포함해야 합니다");
        }

        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new DocumentNotFoundException(slug));
        verifyOwnership(document, userId);

        long oldContentSize = document.getContentSize();
        long newContentSize = oldContentSize;
        String newTitle = hasTitle ? request.title().strip() : null;

        if (hasContent) {
            String content = request.content();
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            if (contentBytes.length > MAX_CONTENT_SIZE) {
                throw new ContentTooLargeException(contentBytes.length, MAX_CONTENT_SIZE);
            }

            if (document.getDocType() == DocType.HTML) {
                content = contentSanitizer.sanitize(content);
                contentBytes = content.getBytes(StandardCharsets.UTF_8);
            }

            newContentSize = contentBytes.length;
            String contentType = document.getDocType() == DocType.HTML ? "text/html" : "text/markdown";
            fileStorage.upload(document.getR2Key(), contentBytes, contentType);
        }

        // 비밀번호 처리: null=변경없음, ""=제거, 값=설정
        String passwordHash = null;
        boolean removePassword = false;
        if (hasPassword) {
            if (request.password().isEmpty()) {
                removePassword = true;
            } else {
                passwordHash = passwordEncoder.encode(request.password());
            }
        }

        Document updated = txService.updateDocument(document.getId(), userId, newTitle, newContentSize, oldContentSize, passwordHash, removePassword);
        return DocumentResponse.from(updated, frontendUrl);
    }

    private void verifyOwnership(Document document, UUID userId) {
        if (document.getUserId() == null || !document.getUserId().equals(userId)) {
            throw new ForbiddenException("문서 소유자가 아닌 사용자의 접근입니다");
        }
    }
}
