package com.drafturl.api.domain.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DocumentTagId implements Serializable {

    @Column(name = "document_id", length = 12)
    private String documentId;

    @Column(name = "tag_id")
    private Long tagId;

    protected DocumentTagId() {
    }

    public DocumentTagId(String documentId, Long tagId) {
        this.documentId = documentId;
        this.tagId = tagId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Long getTagId() {
        return tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentTagId that)) return false;
        return Objects.equals(documentId, that.documentId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, tagId);
    }
}
