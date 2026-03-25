package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends BusinessException {

    public DocumentNotFoundException(String slug) {
        super(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다: " + slug);
    }
}
