package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.domain.document.controller.response.DocumentListResponse;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.tag.dto.TagDto;
import com.drafturl.api.domain.tag.entity.DocumentTag;
import com.drafturl.api.domain.tag.entity.Tag;
import com.drafturl.api.domain.tag.repository.DocumentTagRepository;
import com.drafturl.api.domain.tag.repository.TagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 내 문서 목록 조회 유스케이스.
 */
@Component
public class GetDocumentListUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;
    private final TagRepository tagRepository;
    private final String frontendUrl;

    public GetDocumentListUseCase(DocumentRepository documentRepository,
                                   DocumentTagRepository documentTagRepository,
                                   TagRepository tagRepository,
                                   @Value("${app.frontend-url}") String frontendUrl) {
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.tagRepository = tagRepository;
        this.frontendUrl = frontendUrl;
    }

    public DocumentListResponse execute(UUID userId, int page, int size) {
        return execute(userId, page, size, null, null, null);
    }

    @Transactional(readOnly = true)
    public DocumentListResponse execute(UUID userId, int page, int size, String search, Long tagId, String sort) {
        // null/빈 문자열 → "" 처리 (JPQL에서 :search = '' 조건으로 전체 조회)
        // NFC 정규화: macOS NFD 파일명으로 저장된 제목과 검색 입력(NFC)이 일치하도록
        String normalizedSearch = (search == null || search.isBlank()) ? ""
                : Normalizer.normalize(search.trim(), Normalizer.Form.NFC);

        Sort pageSort = switch (sort == null ? "createdAt" : sort) {
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            case "size"  -> Sort.by(Sort.Direction.DESC, "contentSize");
            default      -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Page<Document> documentPage = documentRepository.searchDocuments(
                userId, normalizedSearch, tagId,
                PageRequest.of(page, Math.min(size, 50), pageSort));

        // N+1 방지: 배치로 태그 조회
        List<String> docIds = documentPage.getContent().stream()
                .map(Document::getId)
                .toList();

        Map<String, List<TagDto>> tagsByDocId;
        if (docIds.isEmpty()) {
            tagsByDocId = Collections.emptyMap();
        } else {
            List<DocumentTag> documentTags = documentTagRepository.findById_DocumentIdIn(docIds);

            // tagId → Tag 매핑 구성
            List<Long> tagIds = documentTags.stream()
                    .map(dt -> dt.getId().getTagId())
                    .distinct()
                    .toList();
            Map<Long, Tag> tagMap = tagRepository.findAllById(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, t -> t));

            // documentId → List<TagDto> 매핑
            tagsByDocId = documentTags.stream()
                    .filter(dt -> tagMap.containsKey(dt.getId().getTagId()))
                    .collect(Collectors.groupingBy(
                            dt -> dt.getId().getDocumentId(),
                            Collectors.mapping(
                                    dt -> TagDto.from(tagMap.get(dt.getId().getTagId())),
                                    Collectors.toList()
                            )
                    ));
        }

        List<DocumentResponse> documents = documentPage.getContent().stream()
                .map(doc -> DocumentResponse.from(doc, frontendUrl,
                        tagsByDocId.getOrDefault(doc.getId(), Collections.emptyList())))
                .toList();

        return new DocumentListResponse(documents, new DocumentListResponse.PaginationInfo(
                documentPage.getNumber(), documentPage.getSize(),
                documentPage.getTotalElements(), documentPage.getTotalPages()));
    }
}
