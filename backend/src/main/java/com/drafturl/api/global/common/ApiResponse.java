package com.drafturl.api.global.common;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error
) {

    public record ErrorDetail(String code, String message) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<?> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }
}
