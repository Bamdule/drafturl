package com.drafturl.api.domain.oauth2.controller;

import com.drafturl.api.domain.oauth2.AuthorizationCodeStore;
import com.drafturl.api.domain.oauth2.AuthorizationCode;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.repository.UserRepository;
import com.drafturl.api.global.auth.JwtProvider;
import com.drafturl.api.global.auth.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.UUID;

@Controller
public class OAuthAuthorizeController {

    private static final String SESSION_COOKIE = "mcp_auth_session";

    private final AuthorizationCodeStore codeStore;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public OAuthAuthorizeController(AuthorizationCodeStore codeStore,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    JwtProvider jwtProvider) {
        this.codeStore = codeStore;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/oauth2/authorize")
    public void authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "code_challenge_method", defaultValue = "S256") String codeChallengeMethod,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "scope", defaultValue = "document:read document:write") String scope,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (!"code".equals(responseType)) {
            redirectError(response, redirectUri, state, "unsupported_response_type", "Only 'code' is supported");
            return;
        }
        if (!"S256".equals(codeChallengeMethod)) {
            redirectError(response, redirectUri, state, "invalid_request", "Only S256 code_challenge_method is supported");
            return;
        }
        if (!isValidRedirectUri(redirectUri)) {
            response.sendError(400, "Invalid redirect_uri: must be localhost or HTTPS");
            return;
        }

        // 이미 JWT로 인증된 상태 (브라우저 쿠키)
        if (principal != null) {
            renderConsentPage(response, clientId, redirectUri, codeChallenge, state, scope, principal.userId());
            return;
        }

        // 세션 쿠키 확인
        UUID sessionUserId = extractSessionUserId(request);
        if (sessionUserId != null) {
            renderConsentPage(response, clientId, redirectUri, codeChallenge, state, scope, sessionUserId);
            return;
        }

        // 로그인 화면 렌더링
        renderLoginPage(response, clientId, redirectUri, codeChallenge, state, scope, null);
    }

    @PostMapping(value = "/oauth2/authorize/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "scope", defaultValue = "document:read document:write") String scope,
            HttpServletResponse response) throws IOException {

        User user = userRepository.findByProviderAndProviderId("email", email).orElse(null);
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            renderLoginPage(response, clientId, redirectUri, codeChallenge, state, scope,
                    "Invalid email or password");
            return;
        }

        // 세션 쿠키 발급 (5분)
        String sessionToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getPlan());
        Cookie cookie = new Cookie(SESSION_COOKIE, sessionToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/oauth2/authorize");
        cookie.setMaxAge(300);
        response.addCookie(cookie);

        renderConsentPage(response, clientId, redirectUri, codeChallenge, state, scope, user.getId());
    }

    @PostMapping(value = "/oauth2/authorize/consent", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void consent(
            @RequestParam("approved") boolean approved,
            @RequestParam("user_id") String userIdStr,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "scope", defaultValue = "document:read document:write") String scope,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 세션 쿠키 또는 JWT로 사용자 확인
        UUID sessionUserId = extractSessionUserId(request);
        UUID userId = UUID.fromString(userIdStr);

        if (sessionUserId == null || !sessionUserId.equals(userId)) {
            response.sendError(403, "Session expired. Please try again.");
            return;
        }

        if (!approved) {
            redirectError(response, redirectUri, state, "access_denied", "User denied the request");
            return;
        }

        AuthorizationCode authCode = codeStore.generate(userId, clientId, redirectUri, codeChallenge, scope);

        String redirect = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", authCode.code())
                .queryParamIfPresent("state", java.util.Optional.ofNullable(state))
                .build().toUriString();

        response.sendRedirect(redirect);
    }

    private UUID extractSessionUserId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                Claims claims = jwtProvider.validateAndGetClaims(cookie.getValue());
                if (claims != null) {
                    return UUID.fromString(claims.getSubject());
                }
            }
        }
        // access_token 쿠키도 확인 (프론트엔드에서 로그인된 상태)
        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                Claims claims = jwtProvider.validateAndGetClaims(cookie.getValue());
                if (claims != null) {
                    return UUID.fromString(claims.getSubject());
                }
            }
        }
        return null;
    }

    private boolean isValidRedirectUri(String uri) {
        try {
            URI parsed = URI.create(uri);
            String host = parsed.getHost();
            String scheme = parsed.getScheme();
            return "localhost".equals(host) || "127.0.0.1".equals(host)
                    || "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private void redirectError(HttpServletResponse response, String redirectUri, String state,
                                String error, String description) throws IOException {
        String redirect = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", error)
                .queryParam("error_description", description)
                .queryParamIfPresent("state", java.util.Optional.ofNullable(state))
                .build().toUriString();
        response.sendRedirect(redirect);
    }

    private void renderLoginPage(HttpServletResponse response, String clientId, String redirectUri,
                                  String codeChallenge, String state, String scope, String error) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DraftURL - Sign In</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                               background: #0a0a0a; color: #e5e5e5; display: flex; align-items: center;
                               justify-content: center; min-height: 100vh; }
                        .card { background: #171717; border: 1px solid #262626; border-radius: 12px;
                                padding: 40px; width: 400px; max-width: 90vw; }
                        h1 { font-size: 24px; margin-bottom: 8px; color: #fff; }
                        .subtitle { color: #a3a3a3; margin-bottom: 24px; font-size: 14px; }
                        .client { color: #60a5fa; font-weight: 600; }
                        label { display: block; font-size: 14px; color: #a3a3a3; margin-bottom: 6px; }
                        input[type="email"], input[type="password"] {
                            width: 100%; padding: 10px 12px; border: 1px solid #404040;
                            border-radius: 8px; background: #262626; color: #fff;
                            font-size: 14px; margin-bottom: 16px; outline: none; }
                        input:focus { border-color: #60a5fa; }
                        .error { color: #ef4444; font-size: 13px; margin-bottom: 16px; }
                        button { width: 100%; padding: 12px; background: #2563eb; color: #fff;
                                 border: none; border-radius: 8px; font-size: 15px; font-weight: 600;
                                 cursor: pointer; }
                        button:hover { background: #1d4ed8; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>DraftURL</h1>
                        <p class="subtitle">Sign in to authorize <span class="client">""" + escapeHtml(clientId) + """
                </span></p>
                """ + (error != null ? "<p class=\"error\">" + escapeHtml(error) + "</p>" : "") + """
                        <form method="POST" action="/oauth2/authorize/login">
                            <input type="hidden" name="client_id" value=\"""" + escapeHtml(clientId) + """
                ">
                            <input type="hidden" name="redirect_uri" value=\"""" + escapeHtml(redirectUri) + """
                ">
                            <input type="hidden" name="code_challenge" value=\"""" + escapeHtml(codeChallenge) + """
                ">
                            <input type="hidden" name="state" value=\"""" + escapeHtml(state != null ? state : "") + """
                ">
                            <input type="hidden" name="scope" value=\"""" + escapeHtml(scope) + """
                ">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" required autofocus>
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" required>
                            <button type="submit">Sign In</button>
                        </form>
                    </div>
                </body>
                </html>
                """);
    }

    private void renderConsentPage(HttpServletResponse response, String clientId, String redirectUri,
                                    String codeChallenge, String state, String scope, UUID userId) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DraftURL - Authorize</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                               background: #0a0a0a; color: #e5e5e5; display: flex; align-items: center;
                               justify-content: center; min-height: 100vh; }
                        .card { background: #171717; border: 1px solid #262626; border-radius: 12px;
                                padding: 40px; width: 420px; max-width: 90vw; }
                        h1 { font-size: 24px; margin-bottom: 8px; color: #fff; }
                        .subtitle { color: #a3a3a3; margin-bottom: 20px; font-size: 14px; }
                        .client { color: #60a5fa; font-weight: 600; }
                        .permissions { background: #262626; border-radius: 8px; padding: 16px;
                                       margin-bottom: 24px; }
                        .permissions h3 { font-size: 13px; color: #a3a3a3; margin-bottom: 10px; }
                        .perm-item { padding: 6px 0; font-size: 14px; color: #d4d4d4; }
                        .perm-item::before { content: "\\2713 "; color: #22c55e; }
                        .buttons { display: flex; gap: 12px; }
                        button { flex: 1; padding: 12px; border: none; border-radius: 8px;
                                 font-size: 15px; font-weight: 600; cursor: pointer; }
                        .deny { background: #404040; color: #e5e5e5; }
                        .deny:hover { background: #525252; }
                        .allow { background: #2563eb; color: #fff; }
                        .allow:hover { background: #1d4ed8; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>DraftURL</h1>
                        <p class="subtitle"><span class="client">""" + escapeHtml(clientId) + """
                </span> wants to access your documents</p>
                        <div class="permissions">
                            <h3>This will allow the application to:</h3>
                            <div class="perm-item">Read your documents</div>
                            <div class="perm-item">Create and update documents</div>
                            <div class="perm-item">Delete documents</div>
                        </div>
                        <div class="buttons">
                            <form method="POST" action="/oauth2/authorize/consent" style="flex:1">
                                <input type="hidden" name="approved" value="false">
                                <input type="hidden" name="user_id" value=\"""" + userId + """
                ">
                                <input type="hidden" name="client_id" value=\"""" + escapeHtml(clientId) + """
                ">
                                <input type="hidden" name="redirect_uri" value=\"""" + escapeHtml(redirectUri) + """
                ">
                                <input type="hidden" name="code_challenge" value=\"""" + escapeHtml(codeChallenge) + """
                ">
                                <input type="hidden" name="state" value=\"""" + escapeHtml(state != null ? state : "") + """
                ">
                                <input type="hidden" name="scope" value=\"""" + escapeHtml(scope) + """
                ">
                                <button type="submit" class="deny" style="width:100%">Deny</button>
                            </form>
                            <form method="POST" action="/oauth2/authorize/consent" style="flex:1">
                                <input type="hidden" name="approved" value="true">
                                <input type="hidden" name="user_id" value=\"""" + userId + """
                ">
                                <input type="hidden" name="client_id" value=\"""" + escapeHtml(clientId) + """
                ">
                                <input type="hidden" name="redirect_uri" value=\"""" + escapeHtml(redirectUri) + """
                ">
                                <input type="hidden" name="code_challenge" value=\"""" + escapeHtml(codeChallenge) + """
                ">
                                <input type="hidden" name="state" value=\"""" + escapeHtml(state != null ? state : "") + """
                ">
                                <input type="hidden" name="scope" value=\"""" + escapeHtml(scope) + """
                ">
                                <button type="submit" class="allow" style="width:100%">Allow</button>
                            </form>
                        </div>
                    </div>
                </body>
                </html>
                """);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
