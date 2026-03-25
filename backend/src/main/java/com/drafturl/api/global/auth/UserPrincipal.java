package com.drafturl.api.global.auth;

import com.drafturl.api.domain.user.PlanType;

import java.util.UUID;

/**
 * SecurityContext의 Authentication에 담기는 사용자 정보.
 * JWT Claims에서 추출한 값을 보관한다.
 *
 * @param userId 사용자 고유 식별자
 * @param email  사용자 이메일
 * @param plan   사용자 요금제
 */
public record UserPrincipal(
        UUID userId,
        String email,
        PlanType plan
) {
}
