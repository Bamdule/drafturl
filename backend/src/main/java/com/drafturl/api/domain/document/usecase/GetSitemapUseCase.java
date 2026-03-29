package com.drafturl.api.domain.document.usecase;

import com.drafturl.api.domain.document.controller.response.SitemapEntry;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sitemap용 공개 문서 slug 목록 조회 유스케이스.
 * 영구 문서이면서 비밀번호가 없는 ACTIVE 문서만 반환한다.
 */
@Component
public class GetSitemapUseCase {

    private static final int MAX_ENTRIES = 1000;

    private final DocumentRepository documentRepository;

    public GetSitemapUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<SitemapEntry> execute() {
        return documentRepository.findSitemapEntries(PageRequest.of(0, MAX_ENTRIES));
    }
}
