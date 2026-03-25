package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentGoneException extends BusinessException {

    public DocumentGoneException(String slug) {
        super(HttpStatus.GONE, "DOCUMENT_GONE", "삭제된 문서입니다: " + slug);
    }
}
