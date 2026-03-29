package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentEditResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.global.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDocumentForEditUseCaseTest {

    private static final String FRONTEND_URL = "https://drafturl.com";
    private static final String SLUG = "xK9mP2nQ";

    @Mock private DocumentRepository documentRepository;
    @Mock private FileStorage fileStorage;

    private GetDocumentForEditUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDocumentForEditUseCase(documentRepository, fileStorage, FRONTEND_URL);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("편집용 문서 조회 성공")
        void getDocumentForEdit() {
            UUID userId = UUID.randomUUID();
            String r2Key = "documents/" + SLUG + "/content.html";
            Document doc = new Document(SLUG, SLUG, userId, "테스트", DocType.HTML,
                    r2Key, 100L, DocumentStatus.ACTIVE, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(fileStorage.download(r2Key)).thenReturn("<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8));

            DocumentEditResponse response = useCase.execute(SLUG, userId);

            assertThat(response.slug()).isEqualTo(SLUG);
            assertThat(response.content()).isEqualTo("<h1>Hello</h1>");
            assertThat(response.url()).isEqualTo(FRONTEND_URL + "/" + SLUG);
        }

        @Test
        @DisplayName("존재하지 않는 문서 조회 시 DocumentNotFoundException")
        void documentNotFound() {
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("소유자가 아닌 사용자의 조회 시 ForbiddenException")
        void nonOwnerCannotAccess() {
            UUID ownerId = UUID.randomUUID();
            UUID otherUser = UUID.randomUUID();
            Document doc = new Document(SLUG, SLUG, ownerId, "테스트", DocType.HTML,
                    "documents/" + SLUG + "/content.html", 100L, DocumentStatus.ACTIVE, null, null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> useCase.execute(SLUG, otherUser))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
