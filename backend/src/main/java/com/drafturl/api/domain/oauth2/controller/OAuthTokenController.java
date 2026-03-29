package com.drafturl.api.domain.oauth2.controller;

import com.drafturl.api.domain.oauth2.AuthorizationCode;
import com.drafturl.api.domain.oauth2.AuthorizationCodeStore;
import com.drafturl.api.domain.oauth2.PkceValidator;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.service.AuthTransactionService;
import com.drafturl.api.domain.user.usecase.RefreshTokenUseCase;
import com.drafturl.api.global.auth.JwtProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class OAuthTokenController {

    private final AuthorizationCodeStore codeStore;
    private final PkceValidator pkceValidator;
    private final JwtProvider jwtProvider;
    private final AuthTransactionService authTxService;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public OAuthTokenController(AuthorizationCodeStore codeStore,
                                PkceValidator pkceValidator,
                                JwtProvider jwtProvider,
                                AuthTransactionService authTxService,
                                RefreshTokenUseCase refreshTokenUseCase) {
        this.codeStore = codeStore;
        this.pkceValidator = pkceValidator;
        this.jwtProvider = jwtProvider;
        this.authTxService = authTxService;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "refresh_token", required = false) String refreshToken) {

        return switch (grantType) {
            case "authorization_code" -> handleAuthorizationCode(code, redirectUri, codeVerifier, clientId);
            case "refresh_token" -> handleRefreshToken(refreshToken);
            default -> errorResponse("unsupported_grant_type", "Unsupported grant_type: " + grantType);
        };
    }

    private ResponseEntity<Map<String, Object>> handleAuthorizationCode(
            String code, String redirectUri, String codeVerifier, String clientId) {

        if (code == null || codeVerifier == null) {
            return errorResponse("invalid_request", "code and code_verifier are required");
        }

        AuthorizationCode authCode = codeStore.consume(code)
                .orElse(null);
        if (authCode == null) {
            return errorResponse("invalid_grant", "Authorization code is invalid or expired");
        }

        if (!authCode.redirectUri().equals(redirectUri)) {
            return errorResponse("invalid_grant", "redirect_uri does not match");
        }

        if (clientId != null && !authCode.clientId().equals(clientId)) {
            return errorResponse("invalid_grant", "client_id does not match");
        }

        if (!pkceValidator.verify(codeVerifier, authCode.codeChallenge())) {
            return errorResponse("invalid_grant", "PKCE code_verifier verification failed");
        }

        User user = authTxService.findUser(authCode.userId());

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getPlan());
        String refreshTokenValue = jwtProvider.createRefreshToken(user.getId());

        LocalDateTime refreshExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtProvider.getRefreshTokenExpiry() / 1000);
        authTxService.saveRefreshToken(user.getId(), refreshTokenValue, refreshExpiresAt);

        return ResponseEntity.ok(tokenResponse(accessToken, refreshTokenValue));
    }

    private ResponseEntity<Map<String, Object>> handleRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return errorResponse("invalid_request", "refresh_token is required");
        }

        try {
            var authResponse = refreshTokenUseCase.execute(refreshToken);
            return ResponseEntity.ok(tokenResponse(authResponse.accessToken(), authResponse.refreshToken()));
        } catch (Exception e) {
            return errorResponse("invalid_grant", e.getMessage());
        }
    }

    private Map<String, Object> tokenResponse(String accessToken, String refreshToken) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", jwtProvider.getAccessTokenExpiry() / 1000);
        response.put("refresh_token", refreshToken);
        return response;
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
