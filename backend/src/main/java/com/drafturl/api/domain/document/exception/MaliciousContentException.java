package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MaliciousContentException extends BusinessException {

    public MaliciousContentException(String reason) {
        super(HttpStatus.BAD_REQUEST, "MALICIOUS_CONTENT",
                "보안 정책에 의해 차단된 콘텐츠입니다: " + reason);
    }
}
