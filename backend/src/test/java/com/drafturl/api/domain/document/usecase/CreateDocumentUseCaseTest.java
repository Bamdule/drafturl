package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.request.CreateDocumentRequest;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.ContentTooLargeException;
import com.drafturl.api.domain.document.exception.MaliciousContentException;
import com.drafturl.api.domain.document.port.ContentSanitizer;
import com.drafturl.api.domain.document.port.ContentValidator;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.domain.document.service.SlugGenerator;
import com.drafturl.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.drafturl.api.domain.storage.service.StorageUsageService;
import com.drafturl.api.domain.user.controller.response.UserResponse;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateDocumentUseCaseTest {

    private static final String FRONTEND_URL = "https://drafturl.com";
    private static final String TEST_SLUG = "xK9mP2nQ";

    @Mock private SlugGenerator slugGenerator;
    @Mock private ContentSanitizer contentSanitizer;
    @Mock private ContentValidator contentValidator;
    @Mock private FileStorage fileStorage;
    @Mock private DocumentTransactionService txService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StorageUsageService storageUsageService;

    private CreateDocumentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateDocumentUseCase(
                slugGenerator, contentSanitizer, contentValidator, fileStorage,
                txService, passwordEncoder, storageUsageService, FRONTEND_URL);
        lenient().when(slugGenerator.generate()).thenReturn(TEST_SLUG);
        lenient().when(contentSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(storageUsageService.getUsageInfo(any()))
                .thenReturn(new UserResponse.StorageUsageInfo(0L, 0));
    }

    private Document buildDocument(UUID userId, String title, DocType docType,
                                   long contentSize, DocumentStatus status,
                                   LocalDateTime expiresAt, String passwordHash) {
        String ext = docType == DocType.HTML ? "html" : "md";
        String r2Key = "documents/" + TEST_SLUG + "/content." + ext;
        return new Document(TEST_SLUG, TEST_SLUG, userId, title, docType, r2Key,
                contentSize, status, expiresAt, passwordHash);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("HTML 문서 생성 성공")
        void createHtmlDocument() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<h1>Hello</h1>", "html", "테스트", null);
            Document doc = buildDocument(userId, "테스트", DocType.HTML, 15L, DocumentStatus.ACTIVE, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            DocumentResponse response = useCase.execute(request, userId);

            assertThat(response.slug()).isEqualTo(TEST_SLUG);
            assertThat(response.url()).isEqualTo(FRONTEND_URL + "/" + TEST_SLUG);
            assertThat(response.docType()).isEqualTo("html");
            verify(fileStorage).upload(eq("documents/" + TEST_SLUG + "/content.html"), any(byte[].class), eq("text/html"));
        }

        @Test
        @DisplayName("Markdown 문서 생성 성공")
        void createMarkdownDocument() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("# Hello", "markdown", "MD 문서", null);
            Document doc = buildDocument(userId, "MD 문서", DocType.MARKDOWN, 7L, DocumentStatus.ACTIVE, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    eq(DocType.MARKDOWN), anyString(), anyLong(), any(), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            DocumentResponse response = useCase.execute(request, userId);

            assertThat(response.docType()).isEqualTo("markdown");
            verify(fileStorage).upload(eq("documents/" + TEST_SLUG + "/content.md"), any(byte[].class), eq("text/markdown"));
        }

        @Test
        @DisplayName("비로그인 사용자 문서 생성 시 24시간 만료 설정")
        void anonymousUserGets24HourExpiry() {
            var request = new CreateDocumentRequest("<p>임시</p>", "html", "임시", null);
            LocalDateTime before = LocalDateTime.now().plusHours(23).plusMinutes(59);
            Document doc = buildDocument(null, "임시", DocType.HTML, 10L, DocumentStatus.ACTIVE,
                    LocalDateTime.now().plusHours(24), null);

            when(txService.insertPendingDocument(anyString(), anyString(), isNull(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(LocalDateTime.class), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), isNull(), anyLong())).thenReturn(doc);

            useCase.execute(request, null);

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(txService).insertPendingDocument(anyString(), anyString(), isNull(), anyString(),
                    any(DocType.class), anyString(), anyLong(), captor.capture(), any());
            LocalDateTime after = LocalDateTime.now().plusHours(24).plusMinutes(1);
            assertThat(captor.getValue()).isBetween(before, after);
        }

        @Test
        @DisplayName("제목 미입력 시 자동 생성")
        void autoGeneratesTitleWhenBlank() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<p>내용</p>", "html", "  ", null);
            Document doc = buildDocument(userId, "자동", DocType.HTML, 10L, DocumentStatus.ACTIVE, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            useCase.execute(request, userId);

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(txService).insertPendingDocument(anyString(), anyString(), any(), titleCaptor.capture(),
                    any(DocType.class), anyString(), anyLong(), any(), any());
            assertThat(titleCaptor.getValue()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2} 문서");
        }

        @Test
        @DisplayName("비밀번호 설정 시 인코딩")
        void encodesPasswordWhenProvided() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<p>비밀</p>", "html", "비밀문서", "mypass");
            when(passwordEncoder.encode("mypass")).thenReturn("$2a$encoded");

            Document doc = buildDocument(userId, "비밀문서", DocType.HTML, 10L, DocumentStatus.ACTIVE, null, "$2a$encoded");
            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), eq("$2a$encoded"))).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            DocumentResponse response = useCase.execute(request, userId);

            verify(passwordEncoder).encode("mypass");
            assertThat(response.isPasswordProtected()).isTrue();
        }

        @Test
        @DisplayName("비밀번호 미설정 시 null")
        void passwordIsNullWhenNotProvided() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<p>공개</p>", "html", "공개", null);
            Document doc = buildDocument(userId, "공개", DocType.HTML, 10L, DocumentStatus.ACTIVE, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), isNull())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            useCase.execute(request, userId);

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("R2 업로드 실패 시 예외 전파")
        void propagatesUploadFailure() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<p>내용</p>", "html", "제목", null);
            Document doc = buildDocument(userId, "제목", DocType.HTML, 10L, DocumentStatus.PENDING, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), any())).thenReturn(doc);
            doThrow(new RuntimeException("R2 connection failed"))
                    .when(fileStorage).upload(anyString(), any(byte[].class), anyString());

            assertThatThrownBy(() -> useCase.execute(request, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("R2 connection failed");
            verify(txService, never()).activateDocument(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("활성화 실패 시 예외 전파")
        void propagatesActivationFailure() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<p>내용</p>", "html", "제목", null);
            Document doc = buildDocument(userId, "제목", DocType.HTML, 10L, DocumentStatus.PENDING, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong()))
                    .thenThrow(new RuntimeException("DB connection lost"));

            assertThatThrownBy(() -> useCase.execute(request, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");
            verify(fileStorage).upload(anyString(), any(byte[].class), anyString());
        }
    }

    @Nested
    @DisplayName("parseDocType")
    class ParseDocType {

        @Test
        @DisplayName("null 타입은 INVALID_DOC_TYPE 예외")
        void nullType() {
            var request = new CreateDocumentRequest("<p>내용</p>", null, "제목", null);
            assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_DOC_TYPE");
        }

        @Test
        @DisplayName("지원하지 않는 타입은 INVALID_DOC_TYPE 예외")
        void unsupportedType() {
            var request = new CreateDocumentRequest("<p>내용</p>", "pdf", "제목", null);
            assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_DOC_TYPE");
        }
    }

    @Nested
    @DisplayName("contentSize")
    class ContentSize {

        @Test
        @DisplayName("5MB 초과 시 ContentTooLargeException")
        void throwsWhenContentExceeds5MB() {
            String large = "a".repeat(5 * 1024 * 1024 + 1);
            var request = new CreateDocumentRequest(large, "html", "대용량", null);
            assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                    .isInstanceOf(ContentTooLargeException.class);
        }
    }

    @Nested
    @DisplayName("악성 콘텐츠 검증")
    class MaliciousContentValidation {

        @Test
        @DisplayName("HTML 문서 생성 시 ContentValidator를 호출한다")
        void callsContentValidatorForHtml() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<h1>Hello</h1>", "html", "테스트", null);
            Document doc = buildDocument(userId, "테스트", DocType.HTML, 15L, DocumentStatus.ACTIVE, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            useCase.execute(request, userId);

            verify(contentValidator).validate("<h1>Hello</h1>");
        }

        @Test
        @DisplayName("Markdown 문서 생성 시 ContentValidator를 호출하지 않는다")
        void skipsContentValidatorForMarkdown() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("# Hello", "markdown", "MD", null);
            Document doc = buildDocument(userId, "MD", DocType.MARKDOWN, 7L, DocumentStatus.ACTIVE, null, null);

            when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                    eq(DocType.MARKDOWN), anyString(), anyLong(), any(), any())).thenReturn(doc);
            when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

            useCase.execute(request, userId);

            verify(contentValidator, never()).validate(anyString());
        }

        @Test
        @DisplayName("ContentValidator가 예외를 던지면 문서가 생성되지 않는다")
        void blocksDocumentCreationOnMaliciousContent() {
            UUID userId = UUID.randomUUID();
            var request = new CreateDocumentRequest("<input type='password'>", "html", "피싱", null);

            doThrow(new MaliciousContentException("피싱 탐지"))
                    .when(contentValidator).validate(anyString());

            assertThatThrownBy(() -> useCase.execute(request, userId))
                    .isInstanceOf(MaliciousContentException.class);
            verify(txService, never()).insertPendingDocument(
                    anyString(), anyString(), any(), anyString(),
                    any(DocType.class), anyString(), anyLong(), any(), any());
        }
    }
}
