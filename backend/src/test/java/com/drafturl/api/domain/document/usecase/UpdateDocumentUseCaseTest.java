package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.request.UpdateDocumentRequest;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.ContentTooLargeException;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.exception.MaliciousContentException;
import com.drafturl.api.domain.document.port.ContentSanitizer;
import com.drafturl.api.domain.document.port.ContentValidator;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.global.exception.BusinessException;
import com.drafturl.api.global.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateDocumentUseCaseTest {

    private static final String FRONTEND_URL = "https://drafturl.com";
    private static final String SLUG = "xK9mP2nQ";

    @Mock private DocumentRepository documentRepository;
    @Mock private ContentSanitizer contentSanitizer;
    @Mock private ContentValidator contentValidator;
    @Mock private FileStorage fileStorage;
    @Mock private DocumentTransactionService txService;
    @Mock private PasswordEncoder passwordEncoder;

    private UpdateDocumentUseCase useCase;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        useCase = new UpdateDocumentUseCase(documentRepository, contentSanitizer,
                contentValidator, fileStorage, txService, passwordEncoder, FRONTEND_URL);
        ownerId = UUID.randomUUID();
    }

    private Document buildDocument(DocType docType) {
        return new Document(SLUG, SLUG, ownerId, "원래 제목", docType,
                "documents/" + SLUG + "/content." + (docType == DocType.HTML ? "html" : "md"),
                100L, DocumentStatus.ACTIVE, null, null);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("제목만 수정 성공")
        void updateTitleOnly() {
            Document doc = buildDocument(DocType.HTML);
            Document updated = new Document(SLUG, SLUG, ownerId, "새 제목", DocType.HTML,
                    doc.getR2Key(), 100L, DocumentStatus.ACTIVE, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(txService.updateDocument(eq(SLUG), eq(ownerId), eq("새 제목"), eq(100L), eq(100L), isNull(), eq(false)))
                    .thenReturn(updated);

            var request = new UpdateDocumentRequest(null, "새 제목", null);
            DocumentResponse response = useCase.execute(SLUG, request, ownerId);

            assertThat(response.slug()).isEqualTo(SLUG);
            verify(fileStorage, never()).upload(anyString(), any(byte[].class), anyString());
        }

        @Test
        @DisplayName("HTML 콘텐츠 수정 시 새니타이징 적용")
        void sanitizesHtmlContent() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(contentSanitizer.sanitize("<p>new</p>")).thenReturn("<p>new</p>");
            when(txService.updateDocument(anyString(), any(), isNull(), anyLong(), anyLong(), any(), anyBoolean()))
                    .thenReturn(doc);

            var request = new UpdateDocumentRequest("<p>new</p>", null, null);
            useCase.execute(SLUG, request, ownerId);

            verify(contentSanitizer).sanitize("<p>new</p>");
            verify(fileStorage).upload(eq(doc.getR2Key()), any(byte[].class), eq("text/html"));
        }

        @Test
        @DisplayName("Markdown 콘텐츠 수정 시 새니타이징 미적용")
        void doesNotSanitizeMarkdown() {
            Document doc = buildDocument(DocType.MARKDOWN);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(txService.updateDocument(anyString(), any(), isNull(), anyLong(), anyLong(), any(), anyBoolean()))
                    .thenReturn(doc);

            var request = new UpdateDocumentRequest("# new", null, null);
            useCase.execute(SLUG, request, ownerId);

            verify(contentSanitizer, never()).sanitize(anyString());
            verify(fileStorage).upload(eq(doc.getR2Key()), any(byte[].class), eq("text/markdown"));
        }

        @Test
        @DisplayName("비밀번호 설정")
        void setPassword() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(passwordEncoder.encode("newpass")).thenReturn("$2a$encoded");
            when(txService.updateDocument(eq(SLUG), eq(ownerId), isNull(), eq(100L), eq(100L), eq("$2a$encoded"), eq(false)))
                    .thenReturn(doc);

            var request = new UpdateDocumentRequest(null, null, "newpass");
            useCase.execute(SLUG, request, ownerId);

            verify(passwordEncoder).encode("newpass");
        }

        @Test
        @DisplayName("비밀번호 제거 (빈 문자열)")
        void removePassword() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(txService.updateDocument(eq(SLUG), eq(ownerId), isNull(), eq(100L), eq(100L), isNull(), eq(true)))
                    .thenReturn(doc);

            var request = new UpdateDocumentRequest(null, null, "");
            useCase.execute(SLUG, request, ownerId);

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("수정할 내용 없으면 EMPTY_UPDATE 예외")
        void emptyUpdateThrows() {
            var request = new UpdateDocumentRequest(null, null, null);

            assertThatThrownBy(() -> useCase.execute(SLUG, request, ownerId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "EMPTY_UPDATE");
        }

        @Test
        @DisplayName("존재하지 않는 문서 수정 시 DocumentNotFoundException")
        void documentNotFound() {
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

            var request = new UpdateDocumentRequest(null, "새 제목", null);
            assertThatThrownBy(() -> useCase.execute(SLUG, request, ownerId))
                    .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("소유자가 아닌 사용자 수정 시 ForbiddenException")
        void nonOwnerCannotUpdate() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            UUID otherUser = UUID.randomUUID();
            var request = new UpdateDocumentRequest(null, "새 제목", null);
            assertThatThrownBy(() -> useCase.execute(SLUG, request, otherUser))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("5MB 초과 콘텐츠 시 ContentTooLargeException")
        void contentTooLarge() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            String large = "a".repeat(5 * 1024 * 1024 + 1);
            var request = new UpdateDocumentRequest(large, null, null);
            assertThatThrownBy(() -> useCase.execute(SLUG, request, ownerId))
                    .isInstanceOf(ContentTooLargeException.class);
        }

        @Test
        @DisplayName("HTML 문서 수정 시 ContentValidator를 호출한다")
        void callsContentValidatorForHtml() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(contentSanitizer.sanitize("<p>new</p>")).thenReturn("<p>new</p>");
            when(txService.updateDocument(anyString(), any(), isNull(), anyLong(), anyLong(), any(), anyBoolean()))
                    .thenReturn(doc);

            var request = new UpdateDocumentRequest("<p>new</p>", null, null);
            useCase.execute(SLUG, request, ownerId);

            verify(contentValidator).validate("<p>new</p>");
        }

        @Test
        @DisplayName("Markdown 문서 수정 시 ContentValidator를 호출하지 않는다")
        void skipsContentValidatorForMarkdown() {
            Document doc = buildDocument(DocType.MARKDOWN);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(txService.updateDocument(anyString(), any(), isNull(), anyLong(), anyLong(), any(), anyBoolean()))
                    .thenReturn(doc);

            var request = new UpdateDocumentRequest("# new", null, null);
            useCase.execute(SLUG, request, ownerId);

            verify(contentValidator, never()).validate(anyString());
        }

        @Test
        @DisplayName("ContentValidator가 예외를 던지면 문서가 수정되지 않는다")
        void blocksDocumentUpdateOnMaliciousContent() {
            Document doc = buildDocument(DocType.HTML);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            doThrow(new MaliciousContentException("피싱 탐지"))
                    .when(contentValidator).validate(anyString());

            var request = new UpdateDocumentRequest("<input type='password'>", null, null);
            assertThatThrownBy(() -> useCase.execute(SLUG, request, ownerId))
                    .isInstanceOf(MaliciousContentException.class);
            verify(fileStorage, never()).upload(anyString(), any(byte[].class), anyString());
        }
    }
}
