package com.drafturl.api.domain.tag.dto;

import com.drafturl.api.domain.tag.repository.TagRepository;

public record TagWithCountDto(Long id, String name, Long documentCount) {

    public static TagWithCountDto from(TagRepository.TagWithCountProjection projection) {
        return new TagWithCountDto(projection.getId(), projection.getName(), projection.getDocumentCount());
    }
}
