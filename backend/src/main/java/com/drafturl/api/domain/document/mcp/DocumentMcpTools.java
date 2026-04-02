package com.drafturl.api.domain.document.mcp;

import com.drafturl.api.domain.document.controller.request.CreateDocumentRequest;
import com.drafturl.api.domain.document.controller.request.UpdateDocumentRequest;
import com.drafturl.api.domain.document.controller.response.DocumentDeleteResponse;
import com.drafturl.api.domain.document.controller.response.DocumentEditResponse;
import com.drafturl.api.domain.document.controller.response.DocumentListResponse;
import com.drafturl.api.domain.document.controller.response.DocumentResponse;
import com.drafturl.api.domain.document.usecase.CreateDocumentUseCase;
import com.drafturl.api.domain.document.usecase.DeleteDocumentUseCase;
import com.drafturl.api.domain.document.usecase.GetDocumentForEditUseCase;
import com.drafturl.api.domain.document.usecase.GetDocumentListUseCase;
import com.drafturl.api.domain.document.usecase.UpdateDocumentUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * DraftURL MCP 도구.
 * 기존 UseCase를 직접 호출하여 MCP 클라이언트에서 문서 CRUD를 수행한다.
 */
@Component
public class DocumentMcpTools {

    private static final Logger log = LoggerFactory.getLogger(DocumentMcpTools.class);

    private final CreateDocumentUseCase createDocumentUseCase;
    private final GetDocumentForEditUseCase getDocumentForEditUseCase;
    private final UpdateDocumentUseCase updateDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final GetDocumentListUseCase getDocumentListUseCase;
    private final McpAuthContext mcpAuthContext;
    private final ObjectMapper objectMapper;

    public DocumentMcpTools(CreateDocumentUseCase createDocumentUseCase,
                            GetDocumentForEditUseCase getDocumentForEditUseCase,
                            UpdateDocumentUseCase updateDocumentUseCase,
                            DeleteDocumentUseCase deleteDocumentUseCase,
                            GetDocumentListUseCase getDocumentListUseCase,
                            McpAuthContext mcpAuthContext,
                            ObjectMapper objectMapper) {
        this.createDocumentUseCase = createDocumentUseCase;
        this.getDocumentForEditUseCase = getDocumentForEditUseCase;
        this.updateDocumentUseCase = updateDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.getDocumentListUseCase = getDocumentListUseCase;
        this.mcpAuthContext = mcpAuthContext;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "create_document", description = "Create a new HTML or Markdown document on DraftURL and get a shareable URL")
    public String createDocument(
            @ToolParam(description = "Document content (HTML or Markdown)") String content,
            @ToolParam(description = "Document type: 'html' or 'markdown'") String type,
            @ToolParam(description = "Document title (max 200 chars)", required = false) String title,
            @ToolParam(description = "Password to protect document (4-100 chars)", required = false) String password) {
        try {
            UUID userId = mcpAuthContext.getUserIdOrNull();
            CreateDocumentRequest request = new CreateDocumentRequest(content, type, title, password);
            DocumentResponse response = createDocumentUseCase.execute(request, userId);
            return toJson(response);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @Tool(name = "get_document", description = "Get a document's content and metadata by slug. Requires ownership.")
    public String getDocument(
            @ToolParam(description = "Document slug identifier (e.g., 'a1b2c3d4')") String slug) {
        try {
            UUID userId = mcpAuthContext.requireUserId();
            DocumentEditResponse response = getDocumentForEditUseCase.execute(slug, userId);
            return toJson(response);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @Tool(name = "update_document", description = "Update an existing document's content, title, or password. Requires ownership.")
    public String updateDocument(
            @ToolParam(description = "Document slug identifier") String slug,
            @ToolParam(description = "New content (HTML or Markdown)", required = false) String content,
            @ToolParam(description = "New title (max 200 chars)", required = false) String title,
            @ToolParam(description = "New password. Empty string removes password.", required = false) String password) {
        try {
            UUID userId = mcpAuthContext.requireUserId();
            UpdateDocumentRequest request = new UpdateDocumentRequest(content, title, password);
            DocumentResponse response = updateDocumentUseCase.execute(slug, request, userId);
            return toJson(response);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @Tool(name = "delete_document", description = "Delete a document (soft delete). Requires ownership.")
    public String deleteDocument(
            @ToolParam(description = "Document slug identifier") String slug) {
        try {
            UUID userId = mcpAuthContext.requireUserId();
            DocumentDeleteResponse response = deleteDocumentUseCase.execute(slug, userId);
            return toJson(response);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @Tool(name = "list_documents", description = "List your documents with pagination. Requires authentication.")
    public String listDocuments(
            @ToolParam(description = "Page number (0-based, default 0)", required = false) Integer page,
            @ToolParam(description = "Page size (max 50, default 20)", required = false) Integer size) {
        try {
            UUID userId = mcpAuthContext.requireUserId();
            int p = (page != null) ? page : 0;
            int s = (size != null) ? Math.min(size, 50) : 20;
            DocumentListResponse response = getDocumentListUseCase.execute(userId, p, s);
            return toJson(response);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("MCP 응답 직렬화 실패", e);
            return "{\"error\": \"Failed to serialize response\"}";
        }
    }

    private String errorResponse(Exception e) {
        log.warn("MCP 도구 실행 실패: {}", e.getMessage());
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"Internal error\"}";
        }
    }
}
