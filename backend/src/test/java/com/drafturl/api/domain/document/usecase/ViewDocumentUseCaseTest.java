package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentViewResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentExpiredException;
import com.drafturl.api.domain.document.exception.DocumentGoneException;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewDocumentUseCaseTest {

    private static final String CDN_BASE_URL = "https://cdn.drafturl.com";
    private static final String SLUG = "xK9mP2nQ";

    @Mock private DocumentRepository documentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private ViewDocumentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ViewDocumentUseCase(documentRepository, passwordEncoder, CDN_BASE_URL);
    }

    private Document buildDocument(UUID userId, DocumentStatus status,
                                   LocalDateTime expiresAt, String passwordHash) {
        return new Document(SLUG, SLUG, userId, "테스트 문서", DocType.HTML,
                "documents/" + SLUG + "/content.html", 100L, status, expiresAt, passwordHash);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("ACTIVE 문서 조회 성공")
        void viewActiveDocument() {
            UUID userId = UUID.randomUUID();
            Document doc = buildDocument(userId, DocumentStatus.ACTIVE, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            DocumentViewResponse response = useCase.execute(SLUG, userId);

            assertThat(response.id()).isEqualTo(SLUG);
            assertThat(response.contentUrl()).isEqualTo(CDN_BASE_URL + "/documents/" + SLUG + "/content.html");
            assertThat(response.isPasswordProtected()).isFalse();
        }

        @Test
        @DisplayName("비밀번호 보호 문서 - 소유자 조회 시 전체 응답")
        void ownerViewsProtectedDocument() {
            UUID ownerId = UUID.randomUUID();
            Document doc = buildDocument(ownerId, DocumentStatus.ACTIVE, null, "$2a$hash");
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            DocumentViewResponse response = useCase.execute(SLUG, ownerId);

            assertThat(response.contentUrl()).isNotNull();
            assertThat(response.isPasswordProtected()).isTrue();
        }

        @Test
        @DisplayName("비밀번호 보호 문서 - 비소유자 조회 시 메타데이터만 반환")
        void nonOwnerViewsProtectedDocument() {
            UUID ownerId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            Document doc = buildDocument(ownerId, DocumentStatus.ACTIVE, null, "$2a$hash");
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            DocumentViewResponse response = useCase.execute(SLUG, viewerId);

            assertThat(response.contentUrl()).isNull();
            assertThat(response.isPasswordProtected()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 문서 조회 시 DocumentNotFoundException")
        void documentNotFound() {
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("EXPIRED 상태 문서 조회 시 DocumentExpiredException")
        void expiredStatusDocument() {
            Document doc = buildDocument(UUID.randomUUID(), DocumentStatus.EXPIRED, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(DocumentExpiredException.class);
        }

        @Test
        @DisplayName("DELETED 상태 문서 조회 시 DocumentGoneException")
        void deletedStatusDocument() {
            Document doc = buildDocument(UUID.randomUUID(), DocumentStatus.DELETED, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(DocumentGoneException.class);
        }

        @Test
        @DisplayName("만료 시간이 지난 ACTIVE 문서 조회 시 DocumentExpiredException")
        void activeButExpiredDocument() {
            Document doc = buildDocument(UUID.randomUUID(), DocumentStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(DocumentExpiredException.class);
        }
    }

    @Nested
    @DisplayName("verifyAndView")
    class VerifyAndView {

        @Test
        @DisplayName("비밀번호 검증 성공 시 전체 응답")
        void correctPasswordReturnsFullResponse() {
            Document doc = buildDocument(UUID.randomUUID(), DocumentStatus.ACTIVE, null, "$2a$hash");
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(passwordEncoder.matches("secret", "$2a$hash")).thenReturn(true);

            DocumentViewResponse response = useCase.verifyAndView(SLUG, "secret");

            assertThat(response.contentUrl()).isNotNull();
        }

        @Test
        @DisplayName("비밀번호 불일치 시 INVALID_PASSWORD 예외")
        void wrongPasswordThrowsException() {
            Document doc = buildDocument(UUID.randomUUID(), DocumentStatus.ACTIVE, null, "$2a$hash");
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

            assertThatThrownBy(() -> useCase.verifyAndView(SLUG, "wrong"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_PASSWORD");
        }

        @Test
        @DisplayName("비밀번호 미설정 문서는 검증 없이 전체 응답")
        void nonProtectedDocumentReturnsDirectly() {
            Document doc = buildDocument(UUID.randomUUID(), DocumentStatus.ACTIVE, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            DocumentViewResponse response = useCase.verifyAndView(SLUG, null);

            assertThat(response.contentUrl()).isNotNull();
        }
    }
}
