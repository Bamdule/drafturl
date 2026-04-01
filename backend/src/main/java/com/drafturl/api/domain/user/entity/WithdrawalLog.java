package com.drafturl.api.domain.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_logs")
public class WithdrawalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String emailHash;

    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected WithdrawalLog() {
    }

    public WithdrawalLog(String emailHash, String reason) {
        this.emailHash = emailHash;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
