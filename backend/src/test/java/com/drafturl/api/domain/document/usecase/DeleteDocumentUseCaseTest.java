package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentDeleteResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.exception.DocumentNotFoundException;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.document.service.DocumentTransactionService;
import com.drafturl.api.global.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteDocumentUseCaseTest {

    private static final String SLUG = "xK9mP2nQ";

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentTransactionService txService;
    @Mock private FileStorage fileStorage;

    @InjectMocks private DeleteDocumentUseCase useCase;

    private Document buildDocument(UUID userId) {
        return new Document(SLUG, SLUG, userId, "테스트", DocType.HTML,
                "documents/" + SLUG + "/content.html", 100L,
                DocumentStatus.ACTIVE, null, null);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("문서 삭제 성공")
        void deleteSuccessfully() {
            UUID userId = UUID.randomUUID();
            Document doc = buildDocument(userId);
            Document deleted = new Document(SLUG, SLUG, userId, "테스트", DocType.HTML,
                    "documents/" + SLUG + "/content.html", 100L, DocumentStatus.DELETED, null, null);

            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(txService.softDeleteDocument(SLUG, userId, 100L)).thenReturn(deleted);

            DocumentDeleteResponse response = useCase.execute(SLUG, userId);

            assertThat(response.id()).isEqualTo(SLUG);
            assertThat(response.slug()).isEqualTo(SLUG);
            verify(fileStorage).delete("documents/" + SLUG + "/content.html");
        }

        @Test
        @DisplayName("R2 삭제 실패해도 정상 응답")
        void succeedsEvenWhenR2DeleteFails() {
            UUID userId = UUID.randomUUID();
            Document doc = buildDocument(userId);
            Document deleted = new Document(SLUG, SLUG, userId, "테스트", DocType.HTML,
                    "documents/" + SLUG + "/content.html", 100L, DocumentStatus.DELETED, null, null);

            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));
            when(txService.softDeleteDocument(SLUG, userId, 100L)).thenReturn(deleted);
            doThrow(new RuntimeException("R2 error")).when(fileStorage).delete(anyString());

            DocumentDeleteResponse response = useCase.execute(SLUG, userId);

            assertThat(response.slug()).isEqualTo(SLUG);
        }

        @Test
        @DisplayName("존재하지 않는 문서 삭제 시 DocumentNotFoundException")
        void documentNotFound() {
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("소유자가 아닌 사용자의 삭제 시 ForbiddenException")
        void nonOwnerCannotDelete() {
            UUID ownerId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Document doc = buildDocument(ownerId);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> useCase.execute(SLUG, otherUserId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("userId가 null인 문서 삭제 시 ForbiddenException")
        void anonymousDocumentCannotBeDeleted() {
            Document doc = buildDocument(null);
            when(documentRepository.findBySlug(SLUG)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> useCase.execute(SLUG, UUID.randomUUID()))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
