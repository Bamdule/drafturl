package com.drafturl.api.domain.tag.service;

import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.tag.dto.TagDto;
import com.drafturl.api.domain.tag.dto.TagWithCountDto;
import com.drafturl.api.domain.tag.entity.DocumentTag;
import com.drafturl.api.domain.tag.entity.DocumentTagId;
import com.drafturl.api.domain.tag.entity.Tag;
import com.drafturl.api.domain.tag.repository.DocumentTagRepository;
import com.drafturl.api.domain.tag.repository.TagRepository;
import com.drafturl.api.global.exception.BusinessException;
import com.drafturl.api.global.exception.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TagService {

    private static final int MAX_TAGS_PER_USER = 50;
    private static final int MAX_TAGS_PER_DOCUMENT = 10;

    private final TagRepository tagRepository;
    private final DocumentTagRepository documentTagRepository;
    private final DocumentRepository documentRepository;

    public TagService(TagRepository tagRepository,
                      DocumentTagRepository documentTagRepository,
                      DocumentRepository documentRepository) {
        this.tagRepository = tagRepository;
        this.documentTagRepository = documentTagRepository;
        this.documentRepository = documentRepository;
    }

    public List<TagWithCountDto> getTagsWithCount(UUID userId) {
        return tagRepository.findTagsWithCountByUserId(userId).stream()
                .map(TagWithCountDto::from)
                .toList();
    }

    @Transactional
    public TagDto createTag(UUID userId, String name) {
        if (tagRepository.countByUserId(userId) >= MAX_TAGS_PER_USER) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TAG_LIMIT_EXCEEDED",
                    "태그는 최대 " + MAX_TAGS_PER_USER + "개까지 생성할 수 있습니다");
        }

        if (tagRepository.existsByUserIdAndNameIgnoreCase(userId, name.trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "TAG_DUPLICATE",
                    "이미 동일한 이름의 태그가 존재합니다: " + name.trim());
        }

        Tag tag = new Tag(userId, name.trim());
        tagRepository.save(tag);
        return TagDto.from(tag);
    }

    @Transactional
    public void deleteTag(UUID userId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND",
                        "태그를 찾을 수 없습니다"));

        if (!tag.getUserId().equals(userId)) {
            throw new ForbiddenException("해당 태그에 대한 권한이 없습니다");
        }

        tagRepository.delete(tag);
    }

    @Transactional
    public void addTagToDocument(UUID userId, String slug, Long tagId) {
        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND",
                        "문서를 찾을 수 없습니다"));

        if (!userId.equals(document.getUserId())) {
            throw new ForbiddenException("해당 문서에 대한 권한이 없습니다");
        }

        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND",
                        "태그를 찾을 수 없습니다"));

        // 이미 연결되어 있으면 무시
        if (documentTagRepository.existsById_DocumentIdAndId_TagId(document.getId(), tag.getId())) {
            return;
        }

        long currentTagCount = documentTagRepository.countById_DocumentId(document.getId());
        if (currentTagCount >= MAX_TAGS_PER_DOCUMENT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DOCUMENT_TAG_LIMIT_EXCEEDED",
                    "문서당 태그는 최대 " + MAX_TAGS_PER_DOCUMENT + "개까지 추가할 수 있습니다");
        }

        documentTagRepository.save(new DocumentTag(new DocumentTagId(document.getId(), tag.getId())));
    }

    @Transactional
    public void removeTagFromDocument(UUID userId, String slug, Long tagId) {
        Document document = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND",
                        "문서를 찾을 수 없습니다"));

        if (!userId.equals(document.getUserId())) {
            throw new ForbiddenException("해당 문서에 대한 권한이 없습니다");
        }

        DocumentTagId documentTagId = new DocumentTagId(document.getId(), tagId);
        documentTagRepository.findById(documentTagId)
                .ifPresent(documentTagRepository::delete);

        // 연결된 ACTIVE 문서가 없으면 태그 자동 삭제
        if (documentTagRepository.countActiveDocumentsForTag(tagId) == 0) {
            tagRepository.findById(tagId).ifPresent(tagRepository::delete);
        }
    }
}
