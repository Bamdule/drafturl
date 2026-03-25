package com.drafturl.api.domain.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "storage_usage")
public class StorageUsage {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private long totalBytes;

    @Column(nullable = false)
    private int documentCount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected StorageUsage() {
    }

    public StorageUsage(UUID userId) {
        this.userId = userId;
        this.totalBytes = 0;
        this.documentCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
