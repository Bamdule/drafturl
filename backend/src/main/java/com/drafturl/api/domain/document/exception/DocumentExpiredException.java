package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentExpiredException extends BusinessException {

    public DocumentExpiredException(String slug) {
        super(HttpStatus.GONE, "DOCUMENT_EXPIRED", "만료된 문서입니다: " + slug);
    }
}
