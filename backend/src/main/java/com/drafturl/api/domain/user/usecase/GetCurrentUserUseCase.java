package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.storage.service.StorageUsageService;
import com.drafturl.api.domain.user.controller.response.UserResponse;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.service.AuthTransactionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 현재 사용자 정보 조회 유스케이스.
 */
@Component
public class GetCurrentUserUseCase {

    private final AuthTransactionService txService;
    private final StorageUsageService storageUsageService;

    public GetCurrentUserUseCase(AuthTransactionService txService,
                                  StorageUsageService storageUsageService) {
        this.txService = txService;
        this.storageUsageService = storageUsageService;
    }

    @Transactional(readOnly = true)
    public UserResponse execute(UUID userId) {
        User user = txService.findUser(userId);
        UserResponse.StorageUsageInfo storageUsageInfo = storageUsageService.getUsageInfo(userId);
        return UserResponse.of(user, storageUsageInfo);
    }
}
