package com.drafturl.api.domain.document.mcp;

import com.drafturl.api.global.auth.UserPrincipal;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MCP 도구에서 인증 정보를 추출하는 헬퍼.
 * {@code @McpTool} 메서드에서는 {@code @AuthenticationPrincipal}을 사용할 수 없으므로
 * SecurityContextHolder에서 직접 꺼낸다.
 */
@Component
public class McpAuthContext {

    public UUID requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED",
                    "Authentication required. Provide a valid JWT Bearer token.");
        }
        return principal.userId();
    }

    public UUID getUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}
