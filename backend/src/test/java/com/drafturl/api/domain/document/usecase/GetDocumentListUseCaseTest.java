package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentListResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDocumentListUseCaseTest {

    private static final String FRONTEND_URL = "https://drafturl.com";

    @Mock private DocumentRepository documentRepository;

    private GetDocumentListUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDocumentListUseCase(documentRepository, FRONTEND_URL);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("문서 목록 조회 성공")
        void listDocuments() {
            UUID userId = UUID.randomUUID();
            Document doc1 = new Document("slug1", "slug1", userId, "문서1", DocType.HTML,
                    "documents/slug1/content.html", 100L, DocumentStatus.ACTIVE, null, null);
            Document doc2 = new Document("slug2", "slug2", userId, "문서2", DocType.MARKDOWN,
                    "documents/slug2/content.md", 200L, DocumentStatus.ACTIVE, null, null);

            var page = new PageImpl<>(List.of(doc1, doc2), PageRequest.of(0, 10), 2);
            when(documentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    eq(userId), eq(DocumentStatus.ACTIVE), any(PageRequest.class))).thenReturn(page);

            DocumentListResponse response = useCase.execute(userId, 0, 10);

            assertThat(response.documents()).hasSize(2);
            assertThat(response.pagination().totalElements()).isEqualTo(2);
            assertThat(response.pagination().page()).isEqualTo(0);
        }

        @Test
        @DisplayName("빈 목록 반환")
        void emptyList() {
            UUID userId = UUID.randomUUID();
            var page = new PageImpl<Document>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(documentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    eq(userId), eq(DocumentStatus.ACTIVE), any(PageRequest.class))).thenReturn(page);

            DocumentListResponse response = useCase.execute(userId, 0, 10);

            assertThat(response.documents()).isEmpty();
            assertThat(response.pagination().totalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("페이지 크기 50 초과 시 50으로 제한")
        void capsPageSizeAt50() {
            UUID userId = UUID.randomUUID();
            var page = new PageImpl<Document>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(documentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    eq(userId), eq(DocumentStatus.ACTIVE), any(PageRequest.class))).thenReturn(page);

            useCase.execute(userId, 0, 100);

            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(documentRepository).findByUserIdAndStatusOrderByCreatedAtDesc(
                    eq(userId), eq(DocumentStatus.ACTIVE), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        }
    }
}
