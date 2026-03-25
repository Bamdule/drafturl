package com.drafturl.api.infra.sanitizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsoupContentSanitizerTest {

    private final JsoupContentSanitizer sanitizer = new JsoupContentSanitizer();

    @Nested
    @DisplayName("null/빈 입력 처리")
    class NullAndEmptyInput {

        @Test
        @DisplayName("null 입력 시 null을 반환한다")
        void returnsNullForNullInput() {
            assertThat(sanitizer.sanitize(null)).isNull();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 문자열을 반환한다")
        void returnsEmptyForBlankInput() {
            assertThat(sanitizer.sanitize("   ")).isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("위험 태그 제거")
    class DangerousTagRemoval {

        @Test
        @DisplayName("iframe 태그를 제거한다")
        void removesIframeTag() {
            String html = "<html><body><p>hello</p><iframe src='http://evil.com'></iframe></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).doesNotContain("<iframe");
            assertThat(result).contains("hello");
        }

        @Test
        @DisplayName("object 태그를 제거한다")
        void removesObjectTag() {
            String html = "<html><body><object data='malware.swf'></object><p>safe</p></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).doesNotContain("<object");
            assertThat(result).contains("safe");
        }

        @Test
        @DisplayName("embed 태그를 제거한다")
        void removesEmbedTag() {
            String html = "<html><body><embed src='malware.swf'><p>safe</p></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).doesNotContain("<embed");
            assertThat(result).contains("safe");
        }
    }

    @Nested
    @DisplayName("javascript: URL 제거")
    class JavascriptUrlRemoval {

        @Test
        @DisplayName("href의 javascript: URL을 제거한다")
        void removesJavascriptHref() {
            String html = "<html><body><a href='javascript:alert(1)'>click</a></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).doesNotContain("javascript:");
            assertThat(result).contains("click");
        }

        @Test
        @DisplayName("img src의 javascript: URL을 제거한다")
        void removesJavascriptImgSrc() {
            String html = "<html><body><img src='javascript:alert(1)'></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).doesNotContain("javascript:");
        }
    }

    @Nested
    @DisplayName("허용 태그 보존")
    class AllowedTagPreservation {

        @Test
        @DisplayName("script 태그를 보존한다")
        void preservesScriptTag() {
            String html = "<html><body><script>console.log('hello');</script><p>text</p></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<script>");
            assertThat(result).contains("console.log");
        }

        @Test
        @DisplayName("style 태그를 보존한다")
        void preservesStyleTag() {
            String html = "<html><body><style>.red { color: red; }</style><p>text</p></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<style>");
            assertThat(result).contains("color: red");
        }

        @Test
        @DisplayName("일반 HTML 태그를 보존한다")
        void preservesNormalHtmlTags() {
            String html = "<html><body><h1>Title</h1><p class='intro'>Hello</p><div><span>World</span></div></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<h1>");
            assertThat(result).contains("<p");
            assertThat(result).contains("class=\"intro\"");
            assertThat(result).contains("<div>");
            assertThat(result).contains("<span>");
        }

        @Test
        @DisplayName("SVG 요소를 보존한다")
        void preservesSvgElements() {
            String html = "<html><body><svg viewBox='0 0 100 100'><circle cx='50' cy='50' r='40' fill='red'/></svg></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<svg");
            assertThat(result).contains("<circle");
        }

        @Test
        @DisplayName("canvas 태그를 보존한다")
        void preservesCanvasTag() {
            String html = "<html><body><canvas width='400' height='300'></canvas></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<canvas");
        }
    }

    @Nested
    @DisplayName("head 영역 새니타이징")
    class HeadSanitization {

        @Test
        @DisplayName("head 내 script 태그를 제거한다")
        void removesScriptFromHead() {
            String html = "<html><head><script>alert('xss')</script><title>Test</title></head><body><p>hello</p></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<title>Test</title>");
            // head 내의 script만 제거, body script는 이 테스트 범위 아님
            // head 영역에서 script가 제거되었는지 확인
            String headSection = result.substring(result.indexOf("<head>"), result.indexOf("</head>"));
            assertThat(headSection).doesNotContain("<script>");
        }

        @Test
        @DisplayName("head 내 meta/style/link 태그를 보존한다")
        void preservesAllowedHeadTags() {
            String html = "<html><head><meta charset='utf-8'><style>body{margin:0}</style><link rel='stylesheet' href='https://example.com/style.css'></head><body></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<meta");
            assertThat(result).contains("<style>");
            assertThat(result).contains("<link");
        }
    }

    @Nested
    @DisplayName("HTML 문서 구조 보존")
    class DocumentStructurePreservation {

        @Test
        @DisplayName("완전한 HTML 문서 구조를 유지한다")
        void preservesDocumentStructure() {
            String html = "<html lang='ko'><head><title>Test</title></head><body><p>hello</p></body></html>";
            String result = sanitizer.sanitize(html);
            assertThat(result).contains("<html");
            assertThat(result).contains("<head>");
            assertThat(result).contains("<body>");
            assertThat(result).contains("lang=\"ko\"");
        }

        @Test
        @DisplayName("복합적인 HTML 문서를 올바르게 새니타이징한다")
        void sanitizesComplexDocument() {
            String html = """
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <title>Chart Demo</title>
                        <script src="https://evil.com/tracker.js"></script>
                        <style>body { font-family: sans-serif; }</style>
                    </head>
                    <body>
                        <h1>Dashboard</h1>
                        <canvas id="chart" width="400" height="300"></canvas>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                        <script>new Chart(document.getElementById('chart'), {});</script>
                        <iframe src="https://evil.com"></iframe>
                        <a href="javascript:alert(1)">XSS</a>
                        <p>Safe content</p>
                    </body>
                    </html>
                    """;
            String result = sanitizer.sanitize(html);

            // head: script 제거, meta/title/style 보존
            String headSection = result.substring(result.indexOf("<head>"), result.indexOf("</head>"));
            assertThat(headSection).doesNotContain("<script>");
            assertThat(headSection).contains("<meta");
            assertThat(headSection).contains("<title>");
            assertThat(headSection).contains("<style>");

            // body: script 보존, iframe 제거, javascript: URL 제거
            assertThat(result).contains("chart.js");
            assertThat(result).doesNotContain("<iframe");
            assertThat(result).doesNotContain("javascript:");
            assertThat(result).contains("Safe content");
            assertThat(result).contains("<canvas");
        }
    }
}
