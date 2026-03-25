package com.drafturl.api.domain.user.controller.response;

import com.drafturl.api.domain.user.entity.User;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserInfo user
) {

    public record UserInfo(
            UUID id,
            String email,
            String name,
            String profileImage,
            String plan
    ) {

        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getProfileImage(),
                    user.getPlan().name().toLowerCase()
            );
        }
    }

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, User user) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, UserInfo.from(user));
    }
}
