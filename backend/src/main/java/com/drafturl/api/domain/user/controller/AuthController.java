package com.drafturl.api.domain.user.controller;

import com.drafturl.api.domain.user.controller.request.EmailLoginRequest;
import com.drafturl.api.domain.user.controller.request.EmailSignupRequest;
import com.drafturl.api.domain.user.controller.request.OAuthCallbackRequest;
import com.drafturl.api.domain.user.controller.request.RefreshTokenRequest;
import com.drafturl.api.domain.user.controller.response.AuthResponse;
import com.drafturl.api.domain.user.controller.response.OAuthStateResponse;
import com.drafturl.api.domain.user.controller.response.UserResponse;
import com.drafturl.api.domain.user.usecase.EmailLoginUseCase;
import com.drafturl.api.domain.user.usecase.EmailSignupUseCase;
import com.drafturl.api.domain.user.usecase.GetCurrentUserUseCase;
import com.drafturl.api.domain.user.usecase.LogoutUseCase;
import com.drafturl.api.domain.user.usecase.OAuthLoginUseCase;
import com.drafturl.api.domain.user.usecase.RefreshTokenUseCase;
import com.drafturl.api.global.auth.OAuthStateProvider;
import com.drafturl.api.global.auth.UserPrincipal;
import com.drafturl.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OAuthLoginUseCase oAuthLoginUseCase;
    private final EmailSignupUseCase emailSignupUseCase;
    private final EmailLoginUseCase emailLoginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final OAuthStateProvider oAuthStateProvider;

    public AuthController(OAuthLoginUseCase oAuthLoginUseCase,
                           EmailSignupUseCase emailSignupUseCase,
                           EmailLoginUseCase emailLoginUseCase,
                           RefreshTokenUseCase refreshTokenUseCase,
                           LogoutUseCase logoutUseCase,
                           GetCurrentUserUseCase getCurrentUserUseCase,
                           OAuthStateProvider oAuthStateProvider) {
        this.oAuthLoginUseCase = oAuthLoginUseCase;
        this.emailSignupUseCase = emailSignupUseCase;
        this.emailLoginUseCase = emailLoginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.oAuthStateProvider = oAuthStateProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody EmailSignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                emailSignupUseCase.execute(request.email(), request.password(), request.name())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody EmailLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                emailLoginUseCase.execute(request.email(), request.password())));
    }

    @GetMapping("/oauth2/state")
    public ResponseEntity<ApiResponse<OAuthStateResponse>> generateOAuthState() {
        String state = oAuthStateProvider.generateState();
        return ResponseEntity.ok(ApiResponse.success(new OAuthStateResponse(state)));
    }

    @PostMapping("/oauth2/callback/{provider}")
    public ResponseEntity<ApiResponse<AuthResponse>> oauthCallback(
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                oAuthLoginUseCase.execute(provider, request.code(), request.redirectUri(), request.state())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                refreshTokenUseCase.execute(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        logoutUseCase.execute(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                getCurrentUserUseCase.execute(principal.userId())));
    }
}
