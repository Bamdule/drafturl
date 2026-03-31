package com.drafturl.api.domain.inquiry.usecase;

import com.drafturl.api.domain.inquiry.InquiryType;
import com.drafturl.api.domain.inquiry.controller.request.CreateInquiryRequest;
import com.drafturl.api.domain.inquiry.controller.response.InquiryResponse;
import com.drafturl.api.domain.inquiry.entity.Inquiry;
import com.drafturl.api.domain.inquiry.repository.InquiryRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateInquiryUseCase {

    private final InquiryRepository inquiryRepository;

    public CreateInquiryUseCase(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    @Transactional
    public InquiryResponse execute(CreateInquiryRequest request) {
        InquiryType type = parseType(request.type());

        if (type == InquiryType.CONTENT_REPORT && (request.documentId() == null || request.documentId().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DOCUMENT_ID_REQUIRED",
                    "콘텐츠 신고 시 문서 ID는 필수입니다");
        }

        Inquiry inquiry = new Inquiry(
                type,
                request.email(),
                request.name(),
                request.subject(),
                request.message(),
                request.documentId()
        );

        inquiry = inquiryRepository.save(inquiry);
        return InquiryResponse.from(inquiry);
    }

    private InquiryType parseType(String type) {
        try {
            return InquiryType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INQUIRY_TYPE",
                    "유효하지 않은 문의 유형입니다: " + type);
        }
    }
}
