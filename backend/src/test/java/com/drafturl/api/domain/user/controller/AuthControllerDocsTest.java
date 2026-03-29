package com.drafturl.api.domain.user.controller;

import com.drafturl.api.domain.user.controller.request.OAuthCallbackRequest;
import com.drafturl.api.domain.user.controller.request.RefreshTokenRequest;
import com.drafturl.api.domain.user.controller.response.AuthResponse;
import com.drafturl.api.domain.user.controller.response.UserResponse;
import com.drafturl.api.domain.user.usecase.EmailLoginUseCase;
import com.drafturl.api.domain.user.usecase.EmailSignupUseCase;
import com.drafturl.api.domain.user.usecase.GetCurrentUserUseCase;
import com.drafturl.api.domain.user.usecase.LogoutUseCase;
import com.drafturl.api.domain.user.usecase.OAuthLoginUseCase;
import com.drafturl.api.domain.user.usecase.RefreshTokenUseCase;
import com.drafturl.api.global.auth.OAuthStateProvider;
import com.drafturl.api.global.auth.UserPrincipal;
import com.drafturl.api.domain.user.PlanType;
import com.drafturl.api.global.exception.GlobalExceptionHandler;
import com.drafturl.api.support.RestDocsSupport;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.UUID;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerDocsTest extends RestDocsSupport {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final OAuthLoginUseCase oAuthLoginUseCase = mock(OAuthLoginUseCase.class);
    private final EmailSignupUseCase emailSignupUseCase = mock(EmailSignupUseCase.class);
    private final EmailLoginUseCase emailLoginUseCase = mock(EmailLoginUseCase.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private final GetCurrentUserUseCase getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
    private final OAuthStateProvider oAuthStateProvider = mock(OAuthStateProvider.class);

    @Override
    protected Object initController() {
        return new AuthController(oAuthLoginUseCase, emailSignupUseCase, emailLoginUseCase,
                refreshTokenUseCase, logoutUseCase, getCurrentUserUseCase, oAuthStateProvider);
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
    @DisplayName("POST /api/v1/auth/oauth2/callback/{provider} - OAuth2 콜백")
    void oauthCallback() throws Exception {
        OAuthCallbackRequest request = new OAuthCallbackRequest(
                "auth_code_abc123", "http://localhost:3000/auth/callback", "random_state_value"
        );

        AuthResponse response = new AuthResponse(
                "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcklkIjoiMDAwMDAwMDAifQ.example",
                "rt_abcdef123456",
                3600L,
                new AuthResponse.UserInfo(USER_ID, "user@example.com", "홍길동", "https://example.com/avatar.jpg", "free")
        );

        given(oAuthLoginUseCase.execute(eq("github"), anyString(), anyString(), anyString()))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/auth/oauth2/callback/{provider}", "github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("auth-oauth-callback",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth API")
                                .summary("OAuth2 콜백 처리")
                                .description("OAuth2 인가 코드를 사용하여 로그인 또는 회원가입을 처리하고 JWT 토큰을 발급합니다.")
                                .pathParameters(
                                        parameterWithName("provider").description("OAuth2 Provider (github, google)")
                                )
                                .requestFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("OAuth2 인가 코드"),
                                        fieldWithPath("redirectUri").type(JsonFieldType.STRING).description("리다이렉트 URI"),
                                        fieldWithPath("state").type(JsonFieldType.STRING).description("CSRF 방지를 위한 state 값")
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("JWT Access Token"),
                                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                        fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER).description("Access Token 만료 시간 (초)"),
                                        fieldWithPath("data.user.id").type(JsonFieldType.STRING).description("사용자 ID"),
                                        fieldWithPath("data.user.email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("data.user.name").type(JsonFieldType.STRING).description("이름"),
                                        fieldWithPath("data.user.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                        fieldWithPath("data.user.plan").type(JsonFieldType.STRING).description("요금제 (free, pro)"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - 토큰 갱신")
    void refresh() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("rt_abcdef123456");

        AuthResponse response = new AuthResponse(
                "eyJhbGciOiJIUzI1NiJ9.new_access_token.example",
                "rt_newtoken789",
                3600L,
                new AuthResponse.UserInfo(USER_ID, "user@example.com", "홍길동", "https://example.com/avatar.jpg", "free")
        );

        given(refreshTokenUseCase.execute("rt_abcdef123456")).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("auth-refresh",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth API")
                                .summary("토큰 갱신")
                                .description("Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다.")
                                .requestFields(
                                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("Refresh Token")
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새로운 JWT Access Token"),
                                        fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새로운 Refresh Token"),
                                        fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER).description("Access Token 만료 시간 (초)"),
                                        fieldWithPath("data.user.id").type(JsonFieldType.STRING).description("사용자 ID"),
                                        fieldWithPath("data.user.email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("data.user.name").type(JsonFieldType.STRING).description("이름"),
                                        fieldWithPath("data.user.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                        fieldWithPath("data.user.plan").type(JsonFieldType.STRING).description("요금제 (free, pro)"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - 로그아웃")
    void logout() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("rt_abcdef123456");

        willDoNothing().given(logoutUseCase).execute("rt_abcdef123456");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("auth-logout",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth API")
                                .summary("로그아웃")
                                .description("Refresh Token을 무효화하여 로그아웃을 처리합니다.")
                                .requestFields(
                                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("무효화할 Refresh Token")
                                )
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 (로그아웃 시 null)"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - 현재 사용자 조회")
    void me() throws Exception {
        UserResponse response = new UserResponse(
                USER_ID,
                "user@example.com",
                "홍길동",
                "https://example.com/avatar.jpg",
                "free",
                new UserResponse.StorageUsageInfo(102400L, 5)
        );

        given(getCurrentUserUseCase.execute(eq(USER_ID))).willReturn(response);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andDo(document("auth-me",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth API")
                                .summary("현재 사용자 조회")
                                .description("현재 로그인한 사용자의 정보와 스토리지 사용량을 조회합니다. 인증이 필요합니다.")
                                .responseFields(
                                        fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("사용자 ID"),
                                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                        fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                        fieldWithPath("data.plan").type(JsonFieldType.STRING).description("요금제 (free, pro)"),
                                        fieldWithPath("data.storageUsage.totalBytes").type(JsonFieldType.NUMBER).description("총 사용 용량 (바이트)"),
                                        fieldWithPath("data.storageUsage.documentCount").type(JsonFieldType.NUMBER).description("문서 수"),
                                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)")
                                )
                                .build()
                        )
                ));
    }
}
