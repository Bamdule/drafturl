package com.drafturl.api.domain.document.port;

/**
 * HTML 콘텐츠 보안 검증 Port 인터페이스.
 * 피싱, 악성 리다이렉트 등 위험한 콘텐츠를 탐지한다.
 *
 * <p>ContentSanitizer와의 차이:
 * <ul>
 *   <li>ContentSanitizer: HTML을 정화(수정)하여 반환</li>
 *   <li>ContentValidator: 악성 패턴 탐지 시 예외를 던져 업로드를 거부</li>
 * </ul>
 */
public interface ContentValidator {

    /**
     * HTML 콘텐츠의 보안 위험을 검증한다.
     * 악성 패턴 탐지 시 MaliciousContentException을 던진다.
     *
     * @param html 검증할 HTML 콘텐츠
     * @throws com.drafturl.api.domain.document.exception.MaliciousContentException 악성 콘텐츠 탐지 시
     */
    void validate(String html);
}
