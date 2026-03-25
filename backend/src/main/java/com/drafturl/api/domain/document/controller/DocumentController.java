package com.drafturl.api.domain.document.controller;

import com.drafturl.api.domain.document.controller.request.CreateDocumentRequest;
import com.drafturl.api.domain.document.controller.request.UpdateDocumentRequest;
import com.drafturl.api.domain.document.controller.response.DocumentDeleteResponse;
import com.drafturl.api.domain.document.controller.response.DocumentEditResponse;
import com.drafturl.api.domain.document.controller.response.DocumentListResponse;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.controller.response.DocumentViewResponse;
import com.drafturl.api.domain.document.usecase.CreateDocumentUseCase;
import com.drafturl.api.domain.document.usecase.DeleteDocumentUseCase;
import com.drafturl.api.domain.document.usecase.GetDocumentForEditUseCase;
import com.drafturl.api.domain.document.usecase.GetDocumentListUseCase;
import com.drafturl.api.domain.document.usecase.UpdateDocumentUseCase;
import com.drafturl.api.domain.document.usecase.ViewDocumentUseCase;
import com.drafturl.api.global.auth.UserPrincipal;
import com.drafturl.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final CreateDocumentUseCase createDocumentUseCase;
    private final ViewDocumentUseCase viewDocumentUseCase;
    private final GetDocumentListUseCase getDocumentListUseCase;
    private final GetDocumentForEditUseCase getDocumentForEditUseCase;
    private final UpdateDocumentUseCase updateDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;

    public DocumentController(CreateDocumentUseCase createDocumentUseCase,
                               ViewDocumentUseCase viewDocumentUseCase,
                               GetDocumentListUseCase getDocumentListUseCase,
                               GetDocumentForEditUseCase getDocumentForEditUseCase,
                               UpdateDocumentUseCase updateDocumentUseCase,
                               DeleteDocumentUseCase deleteDocumentUseCase) {
        this.createDocumentUseCase = createDocumentUseCase;
        this.viewDocumentUseCase = viewDocumentUseCase;
        this.getDocumentListUseCase = getDocumentListUseCase;
        this.getDocumentForEditUseCase = getDocumentForEditUseCase;
        this.updateDocumentUseCase = updateDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UUID userId = (userPrincipal != null) ? userPrincipal.userId() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createDocumentUseCase.execute(request, userId)));
    }

    @GetMapping("/{slug}/view")
    public ResponseEntity<ApiResponse<DocumentViewResponse>> viewDocument(@PathVariable String slug) {
        DocumentViewResponse response = viewDocumentUseCase.execute(slug);
        String etag = Integer.toHexString(response.updatedAt().hashCode());
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=300, s-maxage=3600, stale-while-revalidate=86400")
                .header("ETag", "\"" + etag + "\"")
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DocumentListResponse>> getMyDocuments(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(50) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                getDocumentListUseCase.execute(userPrincipal.userId(), page, size)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<DocumentEditResponse>> getDocumentForEdit(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(
                getDocumentForEditUseCase.execute(slug, userPrincipal.userId())));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable String slug,
            @Valid @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(
                updateDocumentUseCase.execute(slug, request, userPrincipal.userId())));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<ApiResponse<DocumentDeleteResponse>> deleteDocument(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(
                deleteDocumentUseCase.execute(slug, userPrincipal.userId())));
    }
}
