package com.drafturl.api.domain.tag.dto;

import com.drafturl.api.domain.tag.entity.Tag;

public record TagDto(Long id, String name) {

    public static TagDto from(Tag tag) {
        return new TagDto(tag.getId(), tag.getName());
    }
}
