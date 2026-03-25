package com.drafturl.api.global.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근이 거부되었습니다");
    }

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
