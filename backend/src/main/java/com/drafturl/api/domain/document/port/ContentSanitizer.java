package com.drafturl.api.domain.document.port;

/**
 * HTML 콘텐츠 새니타이저 Port 인터페이스.
 * 도메인 계층에서 정의하며, 인프라 계층(JsoupContentSanitizer)에서 구현한다.
 */
public interface ContentSanitizer {

    /**
     * HTML 콘텐츠를 새니타이징한다.
     *
     * <p>head 영역: script 태그 제거, meta/title/style/link 허용
     * <p>body 영역: Safelist 기반 clean() 적용 (iframe/object/embed/javascript: URL 제거)
     * <p>script/style 태그는 body에서 허용 (sandbox iframe에서 격리)
     *
     * @param html 원본 HTML
     * @return 새니타이징된 HTML (완전한 HTML 문서 구조 유지)
     */
    String sanitize(String html);
}
