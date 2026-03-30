package com.drafturl.api.infra.sanitizer;

import com.drafturl.api.domain.document.exception.MaliciousContentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class JsoupContentValidatorTest {

    private final JsoupContentValidator validator = new JsoupContentValidator();

    @Nested
    @DisplayName("피싱 탐지: 브랜드 사칭 + password input")
    class PhishingDetection {

        @Test
        @DisplayName("네이버 브랜드 + password input → 차단")
        void detectsNaverPhishing() {
            String html = """
                <html><body>
                <h1>네이버 로그인</h1>
                <input type="password" name="pw">
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("Google 브랜드 + password input → 차단")
        void detectsGooglePhishing() {
            String html = """
                <html><body>
                <h1>Sign in to Google</h1>
                <form><input type="password"></form>
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("카카오 브랜드 + password input → 차단")
        void detectsKakaoPhishing() {
            String html = """
                <html><body>
                <div>Kakao 계정으로 로그인</div>
                <input type="password">
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("브랜드명 없이 password input만 있으면 허용")
        void allowsPasswordWithoutBrand() {
            String html = """
                <html><body>
                <h1>제품 추가</h1>
                <input type="password" name="secret">
                </body></html>
                """;
            assertThatCode(() -> validator.validate(html)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("브랜드명은 있지만 password input이 없으면 허용")
        void allowsBrandWithoutPassword() {
            String html = """
                <html><body>
                <h1>네이버 뉴스 모아보기</h1>
                <p>오늘의 뉴스</p>
                </body></html>
                """;
            assertThatCode(() -> validator.validate(html)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("대소문자 무관하게 브랜드를 탐지한다")
        void detectsCaseInsensitiveBrand() {
            String html = """
                <html><body>
                <h1>NAVER Login</h1>
                <input type="password">
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }
    }

    @Nested
    @DisplayName("악성 리다이렉트 탐지")
    class MaliciousRedirectDetection {

        @Test
        @DisplayName("meta refresh로 외부 URL 리다이렉트 → 차단")
        void detectsMetaRefreshRedirect() {
            String html = """
                <html><head>
                <meta http-equiv="refresh" content="0;url=https://evil.com">
                </head><body></body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("window.location으로 외부 URL 리다이렉트 → 차단")
        void detectsWindowLocationRedirect() {
            String html = """
                <html><body>
                <script>window.location = "https://evil.com";</script>
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("window.location.href로 외부 URL 리다이렉트 → 차단")
        void detectsWindowLocationHrefRedirect() {
            String html = """
                <html><body>
                <script>window.location.href = "https://evil.com/phish";</script>
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("document.location으로 외부 URL 리다이렉트 → 차단")
        void detectsDocumentLocationRedirect() {
            String html = """
                <html><body>
                <script>document.location = "https://evil.com";</script>
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("top.location으로 외부 URL 리다이렉트 → 차단")
        void detectsTopLocationRedirect() {
            String html = """
                <html><body>
                <script>top.location = "https://evil.com";</script>
                </body></html>
                """;
            assertThatThrownBy(() -> validator.validate(html))
                    .isInstanceOf(MaliciousContentException.class);
        }

        @Test
        @DisplayName("상대 경로 meta refresh는 허용한다")
        void allowsRelativeMetaRefresh() {
            String html = """
                <html><head>
                <meta http-equiv="refresh" content="5;url=/other-page">
                </head><body></body></html>
                """;
            assertThatCode(() -> validator.validate(html)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("내부 URL 참조하는 script는 허용한다")
        void allowsInternalLocationChange() {
            String html = """
                <html><body>
                <script>window.location = "/dashboard";</script>
                </body></html>
                """;
            assertThatCode(() -> validator.validate(html)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("외부 script src 로드는 허용한다 (CDN)")
        void allowsExternalScriptSrc() {
            String html = """
                <html><body>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                </body></html>
                """;
            assertThatCode(() -> validator.validate(html)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("null/빈 입력 처리")
    class NullAndEmptyInput {

        @Test
        @DisplayName("null 입력 시 예외 없이 통과한다")
        void allowsNull() {
            assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 예외 없이 통과한다")
        void allowsBlank() {
            assertThatCode(() -> validator.validate("  ")).doesNotThrowAnyException();
        }
    }
}
