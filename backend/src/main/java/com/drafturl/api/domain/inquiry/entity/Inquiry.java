package com.drafturl.api.domain.inquiry.entity;

import com.drafturl.api.domain.inquiry.InquiryStatus;
import com.drafturl.api.domain.inquiry.InquiryType;
import com.drafturl.api.global.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inquiries")
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryType type;

    @Column(nullable = false)
    private String email;

    private String name;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String message;

    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status = InquiryStatus.PENDING;

    protected Inquiry() {}

    public Inquiry(InquiryType type, String email, String name, String subject, String message, String documentId) {
        this.type = type;
        this.email = email;
        this.name = name;
        this.subject = subject;
        this.message = message;
        this.documentId = documentId;
        this.status = InquiryStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public InquiryType getType() {
        return type;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getDocumentId() {
        return documentId;
    }

    public InquiryStatus getStatus() {
        return status;
    }
}
