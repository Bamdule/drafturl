package com.drafturl.api.infra.storage;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * R2 스토리지 작업 중 발생하는 예외.
 * BusinessException을 상속하여 GlobalExceptionHandler에서 일관되게 처리된다.
 */
public class StorageException extends BusinessException {

    public StorageException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", message);
    }

    public StorageException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", message, cause);
    }
}
