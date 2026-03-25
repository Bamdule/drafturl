package com.drafturl.api.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

// BaseEntity를 상속하지 않는 것은 의도적. updatedAt이 필요 없으므로 별도 createdAt만 관리한다.
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(UUID userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.isRevoked = false;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void revoke() {
        this.isRevoked = true;
    }
}
