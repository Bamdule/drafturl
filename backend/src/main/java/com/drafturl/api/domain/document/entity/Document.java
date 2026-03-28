package com.drafturl.api.domain.document.entity;

import com.drafturl.api.global.common.BaseEntity;
import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

// MVP에서는 id=slug. Phase 2 커스텀 slug 도입 시 분리 예정
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @Id
    @Column(length = 8)
    private String id;

    @Column(nullable = false, unique = true, length = 8)
    private String slug;

    @Column
    private UUID userId;

    @Column
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocType docType;

    @Column(name = "r2_key", nullable = false)
    private String r2Key;

    @Column(nullable = false)
    private long contentSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column
    private LocalDateTime expiresAt;

    @Column(name = "password_hash")
    private String passwordHash;

    protected Document() {
    }

    public Document(String id, String slug, UUID userId, String title,
                    DocType docType, String r2Key, long contentSize,
                    DocumentStatus status, LocalDateTime expiresAt,
                    String passwordHash) {
        this.id = id;
        this.slug = slug;
        this.userId = userId;
        this.title = title;
        this.docType = docType;
        this.r2Key = r2Key;
        this.contentSize = contentSize;
        this.status = status;
        this.expiresAt = expiresAt;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public DocType getDocType() {
        return docType;
    }

    public String getR2Key() {
        return r2Key;
    }

    public long getContentSize() {
        return contentSize;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void activate() {
        if (this.status != DocumentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 문서만 활성화할 수 있습니다. 현재: " + this.status);
        }
        this.status = DocumentStatus.ACTIVE;
    }

    public void updateContentSize(long contentSize) {
        this.contentSize = contentSize;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void markDeleted() {
        if (this.status != DocumentStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태의 문서만 삭제할 수 있습니다. 현재: " + this.status);
        }
        this.status = DocumentStatus.DELETED;
    }

    public void expire() {
        if (this.status != DocumentStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태의 문서만 만료 처리할 수 있습니다. 현재: " + this.status);
        }
        this.status = DocumentStatus.EXPIRED;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isPasswordProtected() {
        return passwordHash != null;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
