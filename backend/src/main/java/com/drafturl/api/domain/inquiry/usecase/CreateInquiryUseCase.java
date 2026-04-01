package com.drafturl.api.domain.inquiry.usecase;

import com.drafturl.api.domain.inquiry.InquiryType;
import com.drafturl.api.domain.inquiry.controller.request.CreateInquiryRequest;
import com.drafturl.api.domain.inquiry.controller.response.InquiryResponse;
import com.drafturl.api.domain.inquiry.entity.Inquiry;
import com.drafturl.api.domain.inquiry.repository.InquiryRepository;
import com.drafturl.api.global.exception.BusinessException;
import com.drafturl.api.infra.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateInquiryUseCase {

    private final InquiryRepository inquiryRepository;
    private final EmailService emailService;
    private final String adminEmail;

    public CreateInquiryUseCase(InquiryRepository inquiryRepository,
                                 EmailService emailService,
                                 @Value("${app.resend.admin-email:drafturl.team@gmail.com}") String adminEmail) {
        this.inquiryRepository = inquiryRepository;
        this.emailService = emailService;
        this.adminEmail = adminEmail;
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

        sendNotification(inquiry);

        return InquiryResponse.from(inquiry);
    }

    private void sendNotification(Inquiry inquiry) {
        String subject = "[DraftURL] " + inquiry.getType().name() + ": " + inquiry.getSubject();
        String html = "<h3>" + inquiry.getSubject() + "</h3>"
                + "<p><b>유형:</b> " + inquiry.getType().name() + "</p>"
                + "<p><b>발신자:</b> " + inquiry.getEmail()
                + (inquiry.getName() != null ? " (" + inquiry.getName() + ")" : "") + "</p>"
                + (inquiry.getDocumentId() != null ? "<p><b>문서:</b> " + inquiry.getDocumentId() + "</p>" : "")
                + "<hr><p>" + inquiry.getMessage().replace("\n", "<br>") + "</p>";

        emailService.send(adminEmail, subject, html);
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
