package com.drafturl.api.infra.sanitizer;

import com.drafturl.api.domain.document.exception.MaliciousContentException;
import com.drafturl.api.domain.document.port.ContentValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Jsoup 기반 HTML 콘텐츠 보안 검증 구현체.
 *
 * <p>탐지 규칙:
 * <ul>
 *   <li>피싱: 유명 브랜드명 + password input 조합</li>
 *   <li>악성 리다이렉트: meta refresh 외부 URL, JS 기반 location 변경</li>
 * </ul>
 */
@Component
public class JsoupContentValidator implements ContentValidator {

    private static final List<String> BRAND_KEYWORDS = List.of(
            "naver", "네이버", "google", "구글", "kakao", "카카오",
            "facebook", "페이스북", "apple", "애플", "microsoft", "마이크로소프트",
            "instagram", "인스타그램", "twitter", "트위터",
            "samsung", "삼성", "daum", "다음", "toss", "토스",
            "shinhan", "신한", "kookmin", "국민", "woori", "우리",
            "hana", "하나", "paypal", "amazon", "아마존"
    );

    private static final Pattern META_REFRESH_PATTERN = Pattern.compile(
            "<meta[^>]+http-equiv\\s*=\\s*[\"']?refresh[\"']?[^>]+url\\s*=\\s*[\"']?https?://",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JS_REDIRECT_PATTERN = Pattern.compile(
            "(window|document|top|self)\\s*\\.\\s*location\\s*(\\.href)?\\s*=\\s*[\"'`]https?://",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void validate(String html) {
        if (html == null || html.isBlank()) {
            return;
        }

        detectPhishing(html);
        detectMaliciousRedirect(html);
    }

    private void detectPhishing(String html) {
        Document document = Jsoup.parse(html);
        boolean hasPasswordInput = !document.select("input[type=password]").isEmpty();
        if (!hasPasswordInput) {
            return;
        }

        String bodyText = document.body() != null ? document.body().text().toLowerCase() : "";
        String titleText = document.title().toLowerCase();
        String combinedText = titleText + " " + bodyText;

        for (String brand : BRAND_KEYWORDS) {
            if (combinedText.contains(brand)) {
                throw new MaliciousContentException("브랜드 사칭 피싱 의심 (" + brand + ")");
            }
        }
    }

    private void detectMaliciousRedirect(String html) {
        if (META_REFRESH_PATTERN.matcher(html).find()) {
            throw new MaliciousContentException("외부 URL로의 자동 리다이렉트 (meta refresh)");
        }

        if (JS_REDIRECT_PATTERN.matcher(html).find()) {
            throw new MaliciousContentException("외부 URL로의 자동 리다이렉트 (JavaScript)");
        }
    }
}
