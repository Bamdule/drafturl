package com.drafturl.api.domain.tag.controller;

import com.drafturl.api.domain.tag.dto.AddTagToDocumentRequest;
import com.drafturl.api.domain.tag.dto.CreateTagRequest;
import com.drafturl.api.domain.tag.dto.TagDto;
import com.drafturl.api.domain.tag.dto.TagWithCountDto;
import com.drafturl.api.domain.tag.service.TagService;
import com.drafturl.api.global.auth.UserPrincipal;
import com.drafturl.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagWithCountDto>>> getTags(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(
                tagService.getTagsWithCount(userPrincipal.userId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TagDto>> createTag(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tagService.createTag(userPrincipal.userId(), request.name())));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tagId) {
        tagService.deleteTag(userPrincipal.userId(), tagId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/documents/{slug}/tags")
    public ResponseEntity<ApiResponse<Void>> addTagToDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String slug,
            @Valid @RequestBody AddTagToDocumentRequest request) {
        tagService.addTagToDocument(userPrincipal.userId(), slug, request.tagId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/documents/{slug}/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> removeTagFromDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String slug,
            @PathVariable Long tagId) {
        tagService.removeTagFromDocument(userPrincipal.userId(), slug, tagId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
