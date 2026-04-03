package com.drafturl.api.global.config;

import com.drafturl.api.global.auth.JwtAuthenticationFilter;
import com.drafturl.api.global.auth.JwtProperties;
import com.drafturl.api.global.filter.MdcFilter;
import com.drafturl.api.global.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정.
 * 필터 체인 순서: CorsFilter -> JwtAuthenticationFilter -> RateLimitFilter -> MdcFilter -> SecurityFilterChain
 * <p>
 * 엔드포인트별 인가 규칙:
 * - POST /api/v1/documents: permitAll (비로그인 문서 생성)
 * - GET /api/v1/documents/{slug}/view: permitAll (공개 서빙)
 * - GET, PUT, DELETE /api/v1/documents/**: authenticated
 * - /api/v1/auth/**: permitAll
 * - Swagger UI: permitAll
 * - 그 외: denyAll
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, McpOAuth2Properties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final MdcFilter mdcFilter;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          MdcFilter mdcFilter,
                          @Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.mdcFilter = mdcFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentTypeOptions(opt -> {})
                        .frameOptions(frame -> frame.sameOrigin())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 회원탈퇴 - authenticated (auth/** permitAll보다 먼저 매칭)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/me").authenticated()

                        // 인증 API - permitAll
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 문서 공개 서빙 - permitAll
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/*/view").permitAll()

                        // 문서 비밀번호 검증 - permitAll
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/verify-password").permitAll()

                        // Sitemap용 공개 문서 목록 - permitAll
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/sitemap").permitAll()

                        // 비로그인 문서 생성 - permitAll
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents").permitAll()

                        // 게스트 문서 이관 - authenticated
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/claim").authenticated()

                        // 문서 CRUD (GET 목록/상세, PUT, DELETE) - authenticated
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/documents/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").authenticated()

                        // API 문서 (ReDoc) + Swagger UI - permitAll
                        .requestMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // OAuth2 Authorization Server 메타데이터
                        .requestMatchers("/.well-known/**").permitAll()

                        // OAuth2 인가/토큰 엔드포인트
                        .requestMatchers("/oauth2/**").permitAll()

                        // MCP 엔드포인트 - 도구 내부에서 개별 인증 검증
                        .requestMatchers("/mcp/**").permitAll()

                        // 문의/신고 - permitAll
                        .requestMatchers(HttpMethod.POST, "/api/v1/inquiries").permitAll()

                        // 헬스체크
                        .requestMatchers("/api/v1/health").permitAll()

                        // 그 외 - denyAll
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(mdcFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        CorsConfiguration mcpCors = new CorsConfiguration();
        mcpCors.setAllowedOrigins(List.of("*"));
        mcpCors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        mcpCors.setAllowedHeaders(List.of("*"));
        mcpCors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/mcp/**", mcpCors);
        source.registerCorsConfiguration("/.well-known/**", mcpCors);
        source.registerCorsConfiguration("/oauth2/**", mcpCors);
        return source;
    }
}
