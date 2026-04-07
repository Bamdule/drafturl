package com.drafturl.api.domain.tag.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_tags")
public class DocumentTag {

    @EmbeddedId
    private DocumentTagId id;

    protected DocumentTag() {
    }

    public DocumentTag(DocumentTagId id) {
        this.id = id;
    }

    public DocumentTagId getId() {
        return id;
    }
}
