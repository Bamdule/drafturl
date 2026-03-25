package com.drafturl.api.global.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "API 호출 제한을 초과했습니다");
    }
}
