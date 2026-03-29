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
import com.drafturl.api.domain.user.PlanType;
import com.drafturl.api.global.exception.GlobalExceptionHandler;
import com.drafturl.api.support.RestDocsSupport;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerDocsTest extends RestDocsSupport {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final CreateDocumentUseCase createDocumentUseCase = mock(CreateDocumentUseCase.class);
    private final ViewDocumentUseCase viewDocumentUseCase = mock(ViewDocumentUseCase.class);
    private final GetDocumentListUseCase getDocumentListUseCase = mock(GetDocumentListUseCase.class);
    private final GetDocumentForEditUseCase getDocumentForEditUseCase = mock(GetDocumentForEditUseCase.class);
    private final UpdateDocumentUseCase updateDocumentUseCase = mock(UpdateDocumentUseCase.class);
    private final DeleteDocumentUseCase deleteDocumentUseCase = mock(DeleteDocumentUseCase.class);

    @Override
    protected Object initController() {
        return new DocumentController(createDocumentUseCase, viewDocumentUseCase,
                getDocumentListUseCase, getDocumentForEditUseCase,
                updateDocumentUseCase, deleteDocumentUseCase);
    }

    @Override
    protected Object[] initControllerAdvice() {
        return new Object[]{new GlobalExceptionHandler()};
    }

    @Override
    protected Object defaultPrincipal() {
        return new UserPrincipal(USER_ID, "user@example.com", PlanType.FREE);
    }

    @Test
    @DisplayName("POST /api/v1/documents - 문서 생성")
    void createDocument() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "<h1>Hello World</h1>", "html", "내 첫 문서", null
        );

        DocumentResponse response = new DocumentResponse(
                "abc12345", "abc12345", "http://localhost:3000/abc12345",
                "내 첫 문서", "html", 20L, "active", false, null,
                LocalDateTime.of(2026, 3, 22, 10, 0, 0),
                LocalDateTime.of(2026, 3, 22, 10, 0, 0)
        );

        given(createDocumentUseCase.execute(any(CreateDocumentRequest.class), any()))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("document-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Document API")
                                .summary("문서 생성")
                                .description("새 문서를 생성합니다. 로그인 사용자는 영구 문서, 비로그인 사용자는 24시간 만료 임시 문서가 생성됩니다.")
                                .requestFields(
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("문서 내용 (최대 5MB)"),
                                        fieldWithPath("type").type(JsonFieldType.STRING).description("문서 타입 (html 또는 markdown)"),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("문서 제목 (선택)").optional(),
                                        fieldWithPath("password").type(JsonFieldType.NULL).description("문서 비밀번호 (선택, 4~100자)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문서 ID"),
                                        fieldWithPath("data.slug").type(JsonFieldType.STRING).description("문서 슬러그 (URL 경로)"),
                                        fieldWithPath("data.url").type(JsonFieldType.STRING).description("문서 공유 URL"),
                                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("문서 제목"),
                                        fieldWithPath("data.docType").type(JsonFieldType.STRING).description("문서 타입"),
                                        fieldWithPath("data.contentSize").type(JsonFieldType.NUMBER).description("콘텐츠 크기 (바이트)"),
                                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("문서 상태 (active, expired, deleted)"),
                                        fieldWithPath("data.isPasswordProtected").type(JsonFieldType.BOOLEAN).description("비밀번호 보호 여부"),
                                        fieldWithPath("data.expiresAt").type(JsonFieldType.NULL).description("만료 시간 (비로그인 시 24시간 후)").optional(),
                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{slug}/view - 문서 서빙 (공개 조회)")
    void viewDocument() throws Exception {
        DocumentViewResponse response = new DocumentViewResponse(
                "abc12345", "내 첫 문서", "html",
                "https://cdn.drafturl.com/documents/abc12345/content.html",
                false,
                LocalDateTime.of(2026, 3, 22, 10, 0, 0),
                LocalDateTime.of(2026, 3, 22, 12, 0, 0)
        );

        given(viewDocumentUseCase.execute(eq("abc12345"), any())).willReturn(response);

        mockMvc.perform(get("/api/v1/documents/{slug}/view", "abc12345"))
                .andExpect(status().isOk())
                .andDo(document("document-view",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Document API")
                                .summary("문서 서빙 (공개 조회)")
                                .description("공유 URL로 접근 시 문서를 조회합니다. 인증이 필요하지 않습니다.")
                                .pathParameters(
                                        parameterWithName("slug").description("문서 슬러그")
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문서 ID"),
                                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("문서 제목"),
                                        fieldWithPath("data.docType").type(JsonFieldType.STRING).description("문서 타입"),
                                        fieldWithPath("data.contentUrl").type(JsonFieldType.STRING).description("CDN 콘텐츠 URL"),
                                        fieldWithPath("data.isPasswordProtected").type(JsonFieldType.BOOLEAN).description("비밀번호 보호 여부"),
                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/documents - 내 문서 목록 조회")
    void getMyDocuments() throws Exception {
        DocumentResponse doc1 = new DocumentResponse(
                "abc12345", "abc12345", "http://localhost:3000/abc12345",
                "첫 번째 문서", "html", 1024L, "active", false, null,
                LocalDateTime.of(2026, 3, 22, 10, 0, 0),
                LocalDateTime.of(2026, 3, 22, 10, 0, 0)
        );
        DocumentResponse doc2 = new DocumentResponse(
                "def67890", "def67890", "http://localhost:3000/def67890",
                "두 번째 문서", "markdown", 2048L, "active", false, null,
                LocalDateTime.of(2026, 3, 21, 9, 0, 0),
                LocalDateTime.of(2026, 3, 21, 15, 30, 0)
        );

        DocumentListResponse response = new DocumentListResponse(
                List.of(doc1, doc2),
                new DocumentListResponse.PaginationInfo(0, 20, 2, 1)
        );

        given(getDocumentListUseCase.execute(eq(USER_ID), anyInt(), anyInt()))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/documents")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("document-list",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Document API")
                                .summary("내 문서 목록 조회")
                                .description("로그인한 사용자의 ACTIVE 상태 문서 목록을 페이지네이션으로 조회합니다. 인증이 필요합니다.")
                                .queryParameters(
                                        parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                                        parameterWithName("size").description("페이지 크기 (기본값: 20, 최대: 50)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.documents[].id").type(JsonFieldType.STRING).description("문서 ID"),
                                        fieldWithPath("data.documents[].slug").type(JsonFieldType.STRING).description("문서 슬러그"),
                                        fieldWithPath("data.documents[].url").type(JsonFieldType.STRING).description("문서 공유 URL"),
                                        fieldWithPath("data.documents[].title").type(JsonFieldType.STRING).description("문서 제목"),
                                        fieldWithPath("data.documents[].docType").type(JsonFieldType.STRING).description("문서 타입"),
                                        fieldWithPath("data.documents[].contentSize").type(JsonFieldType.NUMBER).description("콘텐츠 크기 (바이트)"),
                                        fieldWithPath("data.documents[].status").type(JsonFieldType.STRING).description("문서 상태"),
                                        fieldWithPath("data.documents[].isPasswordProtected").type(JsonFieldType.BOOLEAN).description("비밀번호 보호 여부"),
                                        fieldWithPath("data.documents[].expiresAt").type(JsonFieldType.NULL).description("만료 시간").optional(),
                                        fieldWithPath("data.documents[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                                        fieldWithPath("data.documents[].updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                                        fieldWithPath("data.pagination.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                        fieldWithPath("data.pagination.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                        fieldWithPath("data.pagination.totalElements").type(JsonFieldType.NUMBER).description("전체 문서 수"),
                                        fieldWithPath("data.pagination.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{slug} - 편집용 문서 상세 조회")
    void getDocumentForEdit() throws Exception {
        DocumentEditResponse response = new DocumentEditResponse(
                "abc12345", "abc12345", "http://localhost:3000/abc12345",
                "내 첫 문서", "html", "<h1>Hello World</h1>", 20L, "active",
                false, null,
                LocalDateTime.of(2026, 3, 22, 10, 0, 0),
                LocalDateTime.of(2026, 3, 22, 10, 0, 0)
        );

        given(getDocumentForEditUseCase.execute(eq("abc12345"), eq(USER_ID)))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/documents/{slug}", "abc12345"))
                .andExpect(status().isOk())
                .andDo(document("document-edit-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Document API")
                                .summary("편집용 문서 상세 조회")
                                .description("소유자가 문서를 편집하기 위해 상세 정보를 조회합니다. R2에서 원본 콘텐츠를 함께 로드합니다. 인증이 필요합니다.")
                                .pathParameters(
                                        parameterWithName("slug").description("문서 슬러그")
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문서 ID"),
                                        fieldWithPath("data.slug").type(JsonFieldType.STRING).description("문서 슬러그"),
                                        fieldWithPath("data.url").type(JsonFieldType.STRING).description("문서 공유 URL"),
                                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("문서 제목"),
                                        fieldWithPath("data.docType").type(JsonFieldType.STRING).description("문서 타입"),
                                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("문서 원본 내용"),
                                        fieldWithPath("data.contentSize").type(JsonFieldType.NUMBER).description("콘텐츠 크기 (바이트)"),
                                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("문서 상태"),
                                        fieldWithPath("data.isPasswordProtected").type(JsonFieldType.BOOLEAN).description("비밀번호 보호 여부"),
                                        fieldWithPath("data.expiresAt").type(JsonFieldType.NULL).description("만료 시간").optional(),
                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("PUT /api/v1/documents/{slug} - 문서 수정")
    void updateDocument() throws Exception {
        UpdateDocumentRequest request = new UpdateDocumentRequest(
                "<h1>Updated Content</h1>", "수정된 제목", null
        );

        DocumentResponse response = new DocumentResponse(
                "abc12345", "abc12345", "http://localhost:3000/abc12345",
                "수정된 제목", "html", 25L, "active", false, null,
                LocalDateTime.of(2026, 3, 22, 10, 0, 0),
                LocalDateTime.of(2026, 3, 22, 11, 30, 0)
        );

        given(updateDocumentUseCase.execute(eq("abc12345"), any(UpdateDocumentRequest.class), eq(USER_ID)))
                .willReturn(response);

        mockMvc.perform(put("/api/v1/documents/{slug}", "abc12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("document-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Document API")
                                .summary("문서 수정")
                                .description("소유자가 문서를 수정합니다. content, title, password 중 최소 하나는 포함해야 합니다. 인증이 필요합니다.")
                                .pathParameters(
                                        parameterWithName("slug").description("문서 슬러그")
                                )
                                .requestFields(
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 문서 내용 (선택)").optional(),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("수정할 문서 제목 (선택)").optional(),
                                        fieldWithPath("password").type(JsonFieldType.NULL).description("비밀번호 (null=변경없음, \"\"=제거, 값=설정)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문서 ID"),
                                        fieldWithPath("data.slug").type(JsonFieldType.STRING).description("문서 슬러그"),
                                        fieldWithPath("data.url").type(JsonFieldType.STRING).description("문서 공유 URL"),
                                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("문서 제목"),
                                        fieldWithPath("data.docType").type(JsonFieldType.STRING).description("문서 타입"),
                                        fieldWithPath("data.contentSize").type(JsonFieldType.NUMBER).description("콘텐츠 크기 (바이트)"),
                                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("문서 상태"),
                                        fieldWithPath("data.isPasswordProtected").type(JsonFieldType.BOOLEAN).description("비밀번호 보호 여부"),
                                        fieldWithPath("data.expiresAt").type(JsonFieldType.NULL).description("만료 시간").optional(),
                                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("DELETE /api/v1/documents/{slug} - 문서 삭제")
    void deleteDocument() throws Exception {
        DocumentDeleteResponse deleteResponse = new DocumentDeleteResponse(
                "abc12345", "abc12345", LocalDateTime.of(2026, 3, 22, 14, 0, 0)
        );

        given(deleteDocumentUseCase.execute(eq("abc12345"), eq(USER_ID)))
                .willReturn(deleteResponse);

        mockMvc.perform(delete("/api/v1/documents/{slug}", "abc12345"))
                .andExpect(status().isOk())
                .andDo(document("document-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Document API")
                                .summary("문서 삭제")
                                .description("소유자가 문서를 삭제합니다. soft delete(status=DELETED)로 처리됩니다. 인증이 필요합니다.")
                                .pathParameters(
                                        parameterWithName("slug").description("문서 슬러그")
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("삭제된 문서 ID"),
                                        fieldWithPath("data.slug").type(JsonFieldType.STRING).description("삭제된 문서 슬러그"),
                                        fieldWithPath("data.deletedAt").type(JsonFieldType.STRING).description("삭제 일시"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }
}
