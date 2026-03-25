package com.drafturl.api.global.filter;

import com.drafturl.api.global.auth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString());
            MDC.put(USER_ID_KEY, extractUserId());
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserPrincipal principal) {
                return principal.userId().toString();
            }
        }
        return "anonymous";
    }
}
