package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.user.entity.RefreshToken;
import com.drafturl.api.domain.user.service.AuthTransactionService;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃 유스케이스.
 * Refresh Token을 무효화한다.
 */
@Component
public class LogoutUseCase {

    private final AuthTransactionService txService;

    public LogoutUseCase(AuthTransactionService txService) {
        this.txService = txService;
    }

    @Transactional
    public void execute(String refreshTokenValue) {
        RefreshToken refreshToken = txService.findRefreshToken(refreshTokenValue);

        if (refreshToken.isRevoked()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                    "이미 무효화된 Refresh Token입니다");
        }

        refreshToken.revoke();
    }
}
