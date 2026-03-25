package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ContentTooLargeException extends BusinessException {

    public ContentTooLargeException(long actualBytes, long maxBytes) {
        super(HttpStatus.BAD_REQUEST, "CONTENT_TOO_LARGE",
                "문서 내용이 최대 크기를 초과했습니다. 현재: " + actualBytes + " bytes, 최대: " + maxBytes + " bytes");
    }
}
