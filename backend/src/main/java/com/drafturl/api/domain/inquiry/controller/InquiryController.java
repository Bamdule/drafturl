package com.drafturl.api.domain.inquiry.controller;

import com.drafturl.api.domain.inquiry.controller.request.CreateInquiryRequest;
import com.drafturl.api.domain.inquiry.controller.response.InquiryResponse;
import com.drafturl.api.domain.inquiry.usecase.CreateInquiryUseCase;
import com.drafturl.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final CreateInquiryUseCase createInquiryUseCase;

    public InquiryController(CreateInquiryUseCase createInquiryUseCase) {
        this.createInquiryUseCase = createInquiryUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> createInquiry(
            @Valid @RequestBody CreateInquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createInquiryUseCase.execute(request)));
    }
}
