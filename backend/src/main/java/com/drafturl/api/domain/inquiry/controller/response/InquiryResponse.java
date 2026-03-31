package com.drafturl.api.domain.inquiry.controller.response;

import com.drafturl.api.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;
import java.util.UUID;

public record InquiryResponse(
        UUID id,
        String type,
        LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getType().name(),
                inquiry.getCreatedAt()
        );
    }
}
