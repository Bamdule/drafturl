package com.drafturl.api.domain.document.controller.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class ContentSizeValidator implements ConstraintValidator<ContentSize, String> {

    private long maxBytes;

    @Override
    public void initialize(ContentSize constraintAnnotation) {
        this.maxBytes = constraintAnnotation.maxBytes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotBlank handles null check
        }
        return value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
    }
}
