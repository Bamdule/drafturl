package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentLimitExceededException extends BusinessException {

    public DocumentLimitExceededException(int currentCount, int maxCount) {
        super(HttpStatus.FORBIDDEN, "DOCUMENT_LIMIT_EXCEEDED",
                "문서 생성 한도를 초과했습니다. 현재: " + currentCount + "개, 최대: " + maxCount + "개");
    }
}
