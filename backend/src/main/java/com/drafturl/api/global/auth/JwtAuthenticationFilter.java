package com.drafturl.api.global.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터.
 * Authorization 헤더 또는 httpOnly 쿠키에서 JWT를 추출하여 검증한다.
 * 유효한 토큰이면 SecurityContext에 Authentication을 설정하고,
 * 유효하지 않으면 필터 체인을 계속 진행한다 (익명 요청 허용).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token == null) {
            token = extractTokenFromCookie(request);
        }

        if (token != null) {
            Claims claims = jwtProvider.validateAndGetClaims(token);
            if (claims != null) {
                UserPrincipal principal = jwtProvider.extractUserPrincipal(claims);
                setAuthentication(principal, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출한다.
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * httpOnly 쿠키에서 Access Token을 추출한다.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * SecurityContext에 인증 정보를 설정한다.
     */
    private void setAuthentication(UserPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT authentication set for user: {}", principal.userId());
    }
}
