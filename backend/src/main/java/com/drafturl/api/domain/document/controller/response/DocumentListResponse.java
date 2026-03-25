package com.drafturl.api.domain.document.controller.response;

import java.util.List;

public record DocumentListResponse(
        List<DocumentResponse> documents,
        PaginationInfo pagination
) {

    public record PaginationInfo(
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
