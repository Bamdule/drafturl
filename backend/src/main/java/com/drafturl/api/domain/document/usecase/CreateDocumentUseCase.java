package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.controller.request.CreateDocumentRequest;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.ContentTooLargeException;
import com.drafturl.api.domain.document.exception.DocumentLimitExceededException;
import com.drafturl.api.domain.document.port.ContentSanitizer;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.domain.document.service.SlugGenerator;
import com.drafturl.api.domain.storage.service.StorageUsageService;
import com.drafturl.api.global.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 문서 생성 유스케이스 (2단계 커밋 패턴).
 *
 * 흐름: 검증 → slug 생성 → 새니타이징 → DB INSERT(PENDING) → R2 업로드 → DB UPDATE(ACTIVE)
 */
@Component
public class CreateDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateDocumentUseCase.class);
    private static final long MAX_CONTENT_SIZE = 5L * 1024 * 1024;
    private static final int FREE_PLAN_MAX_DOCUMENTS = 30;

    private final SlugGenerator slugGenerator;
    private final ContentSanitizer contentSanitizer;
    private final FileStorage fileStorage;
    private final DocumentTransactionService txService;
    private final PasswordEncoder passwordEncoder;
    private final StorageUsageService storageUsageService;
    private final String frontendUrl;

    public CreateDocumentUseCase(SlugGenerator slugGenerator,
                                  ContentSanitizer contentSanitizer,
                                  FileStorage fileStorage,
                                  DocumentTransactionService txService,
                                  PasswordEncoder passwordEncoder,
                                  StorageUsageService storageUsageService,
                                  @Value("${app.frontend-url}") String frontendUrl) {
        this.slugGenerator = slugGenerator;
        this.contentSanitizer = contentSanitizer;
        this.fileStorage = fileStorage;
        this.txService = txService;
        this.passwordEncoder = passwordEncoder;
        this.storageUsageService = storageUsageService;
        this.frontendUrl = frontendUrl;
    }

    public DocumentResponse execute(CreateDocumentRequest request, UUID userId) {
        // 문서 생성 제한 검사 (로그인 사용자만)
        if (userId != null) {
            var usageInfo = storageUsageService.getUsageInfo(userId);
            if (usageInfo.documentCount() >= FREE_PLAN_MAX_DOCUMENTS) {
                throw new DocumentLimitExceededException(usageInfo.documentCount(), FREE_PLAN_MAX_DOCUMENTS);
            }
        }

        DocType docType = parseDocType(request.type());

        String content = request.content();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        if (contentBytes.length > MAX_CONTENT_SIZE) {
            throw new ContentTooLargeException(contentBytes.length, MAX_CONTENT_SIZE);
        }

        String slug = slugGenerator.generate();
        String id = slug;

        // HTML 새니타이징: iframe sandbox에 더해 서버 측 다층 방어 적용.
        // form/input 등 피싱 요소를 제거하여 Google Safe Browsing 경고를 방지한다.
        if (docType == DocType.HTML) {
            content = contentSanitizer.sanitize(content);
            contentBytes = content.getBytes(StandardCharsets.UTF_8);
        }

        String ext = docType == DocType.HTML ? "html" : "md";
        String r2Key = "documents/" + slug + "/content." + ext;
        long contentSize = contentBytes.length;
        LocalDateTime expiresAt = (userId == null) ? LocalDateTime.now().plusHours(24) : null;
        String title = (request.title() != null && !request.title().isBlank())
                ? request.title().strip()
                : java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 문서";

        String passwordHash = (request.password() != null && !request.password().isBlank())
                ? passwordEncoder.encode(request.password())
                : null;

        // DB INSERT (PENDING)
        Document document = txService.insertPendingDocument(id, slug, userId, title, docType, r2Key, contentSize, expiresAt, passwordHash);

        // R2 업로드
        String contentType = docType == DocType.HTML ? "text/html" : "text/markdown";
        try {
            fileStorage.upload(r2Key, contentBytes, contentType);
        } catch (Exception e) {
            log.error("R2 업로드 실패, PENDING 유지: slug={}", slug, e);
            throw e;
        }

        // DB UPDATE (ACTIVE) + StorageUsage
        try {
            document = txService.activateDocument(document.getId(), userId, contentSize);
        } catch (Exception e) {
            log.error("문서 활성화 실패, PENDING 유지: slug={}", slug, e);
            throw e;
        }

        return DocumentResponse.from(document, frontendUrl);
    }

    private DocType parseDocType(String type) {
        if (type == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DOC_TYPE", "문서 타입을 지정해주세요");
        }
        return switch (type.toLowerCase()) {
            case "html" -> DocType.HTML;
            case "markdown" -> DocType.MARKDOWN;
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DOC_TYPE",
                    "지원하지 않는 문서 타입입니다: " + type);
        };
    }
}
