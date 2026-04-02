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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
public class OAuthAuthorizeController {

    private static final String SESSION_COOKIE = "mcp_auth_session";

    private final AuthorizationCodeStore codeStore;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final String frontendUrl;

    public OAuthAuthorizeController(AuthorizationCodeStore codeStore,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    JwtProvider jwtProvider,
                                    @Value("${app.frontend-url}") String frontendUrl) {
        this.codeStore = codeStore;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.frontendUrl = frontendUrl;
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
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "scope", defaultValue = "document:read document:write") String scope,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 세션에서만 사용자 ID 추출
        UUID userId = extractSessionUserId(request);
        if (userId == null) {
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
        String currentAuthorizeUrl = "/oauth2/authorize?"
                + "response_type=code"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8)
                + "&code_challenge_method=S256"
                + (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "")
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
        String googleLoginUrl = frontendUrl + "/auth/login?returnTo=" + URLEncoder.encode(currentAuthorizeUrl, StandardCharsets.UTF_8);

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
                        .divider { text-align: center; margin: 20px 0; position: relative; }
                        .divider::before { content: ''; position: absolute; top: 50%%; left: 0; right: 0; height: 1px; background: #404040; }
                        .divider span { background: #171717; padding: 0 12px; position: relative; color: #a3a3a3; font-size: 13px; }
                        .google-btn { display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%%; padding: 12px; background: #fff; color: #333; border-radius: 8px; font-size: 14px; font-weight: 600; text-decoration: none; }
                        .google-btn:hover { background: #f5f5f5; }
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
                        <div class="divider"><span>or</span></div>
                        <a href=\"""" + escapeHtml(googleLoginUrl) + """
                " class="google-btn">
                            <svg viewBox="0 0 24 24" width="18" height="18">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                            </svg>
                            Continue with Google
                        </a>
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
