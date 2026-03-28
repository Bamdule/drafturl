package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentViewResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentExpiredException;
import com.drafturl.api.domain.document.exception.DocumentGoneException;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 공개 문서 서빙 유스케이스.
 * 상태/만료 확인 후 CDN URL을 생성하여 반환한다.
 * 비밀번호 보호 문서는 소유자가 아닌 경우 메타데이터만 반환한다.
 */
@Component
public class ViewDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String cdnBaseUrl;

    public ViewDocumentUseCase(DocumentRepository documentRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.cdn.base-url}") String cdnBaseUrl) {
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    /**
     * 문서 조회. 비밀번호 보호 문서는 소유자가 아닌 경우 contentUrl 없이 메타데이터만 반환.
     */
    public DocumentViewResponse execute(String slug, UUID viewerUserId) {
        Document document = findActiveDocument(slug);

        boolean isOwner = viewerUserId != null && viewerUserId.equals(document.getUserId());

        if (document.isPasswordProtected() && !isOwner) {
            return DocumentViewResponse.protectedMetadata(document);
        }

        return DocumentViewResponse.from(document, cdnBaseUrl);
    }

    /**
     * 비밀번호 검증 후 문서 조회.
     */
    public DocumentViewResponse verifyAndView(String slug, String password) {
        Document document = findActiveDocument(slug);

        if (!document.isPasswordProtected()) {
            return DocumentViewResponse.from(document, cdnBaseUrl);
        }

        if (!passwordEncoder.matches(password, document.getPasswordHash())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "INVALID_PASSWORD",
                    "비밀번호가 올바르지 않습니다");
        }

        return DocumentViewResponse.from(document, cdnBaseUrl);
    }

    private Document findActiveDocument(String slug) {
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

        return document;
    }
}
