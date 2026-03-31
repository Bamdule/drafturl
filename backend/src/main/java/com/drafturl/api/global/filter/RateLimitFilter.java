package com.drafturl.api.global.filter;

import com.drafturl.api.global.auth.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 필터.
 * 토큰 버킷 알고리즘으로 API 호출을 제한한다.
 * <p>
 * - 비로그인 IP: 분당 10회 (capacity=10, refillRate=10/60초)
 * - 로그인 사용자: 분당 30회 (capacity=30, refillRate=30/60초)
 * <p>
 * 인증 전에 실행되므로 SecurityContext에 인증 정보가 없을 수 있다.
 * 이 경우 IP 기반으로 제한한다. 인증 후에는 userId 기반으로 제한한다.
 * <p>
 * 참고: 이 필터는 JwtAuthenticationFilter 뒤에 배치되어
 * SecurityContext에서 인증 정보를 확인할 수 있다.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int ANONYMOUS_CAPACITY = 10;
    private static final double ANONYMOUS_REFILL_RATE = 10.0 / 60.0; // 10 tokens per 60 seconds

    private static final int AUTHENTICATED_CAPACITY = 30;
    private static final double AUTHENTICATED_REFILL_RATE = 30.0 / 60.0; // 30 tokens per 60 seconds

    private static final long BUCKET_EXPIRY_MILLIS = 5L * 60 * 1000; // 5 minutes

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/")
                || path.startsWith("/mcp/")
                || path.startsWith("/.well-known/")
                || path.startsWith("/api/v1/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String key;
        int capacity;
        double refillRate;

        // SecurityContext에서 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            key = "user:" + principal.userId().toString();
            capacity = AUTHENTICATED_CAPACITY;
            refillRate = AUTHENTICATED_REFILL_RATE;
        } else {
            key = "ip:" + resolveClientIp(request);
            capacity = ANONYMOUS_CAPACITY;
            refillRate = ANONYMOUS_REFILL_RATE;
        }

        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(capacity, refillRate));

        if (!bucket.tryConsume()) {
            long retryAfterSeconds = bucket.getSecondsUntilNextToken();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json;charset=UTF-8");

            Map<String, Object> errorBody = Map.of(
                    "success", false,
                    "error", Map.of(
                            "code", "RATE_LIMIT_EXCEEDED",
                            "message", "API 호출 제한을 초과했습니다"
                    )
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트 IP를 해석한다.
     * X-Forwarded-For 헤더가 있으면 첫 번째 IP를 사용하고,
     * 없으면 RemoteAddr을 사용한다.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For: client, proxy1, proxy2 -> client IP 사용
            return xForwardedFor.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    /**
     * 5분 이상 접근되지 않은 오래된 버킷을 정리한다.
     * 매 5분마다 실행된다.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupStaleBuckets() {
        long now = System.currentTimeMillis();
        int removed = 0;

        var iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue().getLastAccessTime() > BUCKET_EXPIRY_MILLIS) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Rate limit 버킷 정리: removed={}, remaining={}", removed, buckets.size());
        }
    }

    /**
     * 토큰 버킷 구현.
     * synchronized로 스레드 안전성을 보장한다.
     */
    static class TokenBucket {

        private double tokens;
        private long lastRefillTime;
        private long lastAccessTime;
        private final int capacity;
        private final double refillRate; // tokens per second

        TokenBucket(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
            this.lastAccessTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            this.lastAccessTime = System.currentTimeMillis();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        /**
         * 다음 토큰 리필까지 남은 시간(초)을 반환한다.
         */
        synchronized long getSecondsUntilNextToken() {
            refill();
            if (tokens >= 1.0) {
                return 0;
            }
            double tokensNeeded = 1.0 - tokens;
            double secondsNeeded = tokensNeeded / refillRate;
            return (long) Math.ceil(secondsNeeded);
        }

        synchronized long getLastAccessTime() {
            return lastAccessTime;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillTime;
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double newTokens = elapsedSeconds * refillRate;

            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTime = now;
            }
        }
    }
}
