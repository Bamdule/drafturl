package com.drafturl.api.infra.oauth2;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * OAuth2 Provider와의 통신 중 발생하는 예외.
 */
public class OAuth2Exception extends BusinessException {

    public OAuth2Exception(String message) {
        super(HttpStatus.BAD_GATEWAY, "OAUTH2_ERROR", message);
    }

    public OAuth2Exception(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
