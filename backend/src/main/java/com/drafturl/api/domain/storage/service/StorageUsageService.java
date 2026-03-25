package com.drafturl.api.domain.storage.service;

import com.drafturl.api.domain.storage.entity.StorageUsage;
import com.drafturl.api.domain.storage.repository.StorageUsageRepository;
import com.drafturl.api.domain.user.controller.response.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * StorageUsage 도메인의 비즈니스 로직을 담당하는 서비스.
 * DocumentTransactionService와 AuthService에서 StorageUsageRepository 직접 참조를 대체한다.
 */
@Service
public class StorageUsageService {

    private final StorageUsageRepository storageUsageRepository;

    public StorageUsageService(StorageUsageRepository storageUsageRepository) {
        this.storageUsageRepository = storageUsageRepository;
    }

    /**
     * 사용량을 증가시킨다. StorageUsage 레코드가 없으면 생성한다.
     */
    @Transactional
    public void incrementUsage(UUID userId, long bytes, int count) {
        if (storageUsageRepository.findByUserId(userId).isEmpty()) {
            storageUsageRepository.save(new StorageUsage(userId));
        }
        storageUsageRepository.incrementUsage(userId, bytes, count);
    }

    /**
     * 사용량을 감소시킨다.
     */
    @Transactional
    public void decrementUsage(UUID userId, long bytes, int count) {
        storageUsageRepository.decrementUsage(userId, bytes, count);
    }

    /**
     * 사용자의 스토리지 사용량 정보를 반환한다.
     */
    @Transactional(readOnly = true)
    public UserResponse.StorageUsageInfo getUsageInfo(UUID userId) {
        StorageUsage storageUsage = storageUsageRepository.findByUserId(userId)
                .orElse(null);

        return storageUsage != null
                ? new UserResponse.StorageUsageInfo(storageUsage.getTotalBytes(), storageUsage.getDocumentCount())
                : new UserResponse.StorageUsageInfo(0, 0);
    }
}
