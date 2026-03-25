package com.drafturl.api.domain.user.controller.response;

import com.drafturl.api.domain.user.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String profileImage,
        String plan,
        StorageUsageInfo storageUsage
) {

    public record StorageUsageInfo(
            long totalBytes,
            int documentCount
    ) {
    }

    public static UserResponse of(User user, StorageUsageInfo storageUsage) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImage(),
                user.getPlan().name().toLowerCase(),
                storageUsage
        );
    }
}
