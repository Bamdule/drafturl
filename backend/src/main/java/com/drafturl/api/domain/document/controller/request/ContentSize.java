package com.drafturl.api.domain.document.controller.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 문자열의 UTF-8 바이트 크기가 지정된 최대값을 초과하지 않는지 검증한다.
 */
@Documented
@Constraint(validatedBy = ContentSizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContentSize {

    long maxBytes() default 5 * 1024 * 1024; // 5MB

    String message() default "문서 내용이 최대 크기를 초과했습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
