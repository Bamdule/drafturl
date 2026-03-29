package com.drafturl.api.domain.document.service;

import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlugGeneratorTest {

    @Mock private DocumentRepository documentRepository;

    @InjectMocks private SlugGenerator slugGenerator;

    private Document existingDocument() {
        return new Document("existing", "existing", null, "title", DocType.HTML,
                "r2key", 100L, DocumentStatus.ACTIVE, null, null);
    }

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("8자리 slug를 생성한다")
        void generates8CharSlug() {
            when(documentRepository.findBySlug(anyString())).thenReturn(Optional.empty());

            String slug = slugGenerator.generate();

            assertThat(slug).hasSize(8);
        }

        @Test
        @DisplayName("slug는 영문 대소문자와 숫자로만 구성된다")
        void slugContainsOnlyAlphanumeric() {
            when(documentRepository.findBySlug(anyString())).thenReturn(Optional.empty());

            String slug = slugGenerator.generate();

            assertThat(slug).matches("[A-Za-z0-9]{8}");
        }

        @Test
        @DisplayName("첫 번째 시도에서 고유 slug 생성")
        void generatesUniqueOnFirstAttempt() {
            when(documentRepository.findBySlug(anyString())).thenReturn(Optional.empty());

            String slug = slugGenerator.generate();

            assertThat(slug).isNotBlank();
            verify(documentRepository, times(1)).findBySlug(anyString());
        }

        @Test
        @DisplayName("충돌 시 재시도하여 성공")
        void retriesOnCollision() {
            when(documentRepository.findBySlug(anyString()))
                    .thenReturn(Optional.of(existingDocument()))
                    .thenReturn(Optional.empty());

            String slug = slugGenerator.generate();

            assertThat(slug).hasSize(8);
            verify(documentRepository, times(2)).findBySlug(anyString());
        }

        @Test
        @DisplayName("3회 모두 충돌 시 BusinessException")
        void throwsAfterMaxRetries() {
            when(documentRepository.findBySlug(anyString()))
                    .thenReturn(Optional.of(existingDocument()));

            assertThatThrownBy(() -> slugGenerator.generate())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INTERNAL_ERROR");

            verify(documentRepository, times(3)).findBySlug(anyString());
        }

        @Test
        @DisplayName("매번 다른 slug를 생성한다")
        void generatesDifferentSlugs() {
            when(documentRepository.findBySlug(anyString())).thenReturn(Optional.empty());

            String slug1 = slugGenerator.generate();
            String slug2 = slugGenerator.generate();

            // SecureRandom이므로 확률적으로 다름 (62^8 경우의 수)
            // 극히 드문 확률로 같을 수 있으나 실질적으로 항상 다름
            assertThat(slug1).isNotEqualTo(slug2);
        }
    }
}
