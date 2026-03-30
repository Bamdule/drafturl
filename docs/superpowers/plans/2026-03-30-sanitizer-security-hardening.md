# Sanitizer 보안 강화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HTML sanitizer의 보안을 강화하여 CSP 주입, password 입력 무력화, 피싱/악성 리다이렉트 탐지 및 업로드 차단을 구현한다.

**Architecture:** sanitizer는 HTML을 정화(수정)하는 역할, validator는 악성 콘텐츠를 탐지하여 거부하는 역할로 분리한다. sanitizer에서 CSP 주입과 password→text 변환을 수행하고, 새로운 ContentValidator가 피싱/리다이렉트 패턴을 탐지하여 업로드를 차단한다.

**Tech Stack:** Jsoup (HTML 파싱), Spring Boot, JUnit 5, AssertJ, Mockito

---

### Task 1: 기존 sanitizer 테스트 업데이트 (폼 태그 허용 반영)

**Files:**
- Modify: `backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizerTest.java:137-208`

폼 태그 제거 정책이 허용으로 변경되었으므로, 기존 `PhishingFormRemoval` 테스트를 폼 태그 보존 테스트로 교체한다.

- [ ] **Step 1: PhishingFormRemoval 테스트를 FormTagPreservation으로 교체**

`JsoupContentSanitizerTest.java`의 `PhishingFormRemoval` 클래스(137-208줄)를 다음으로 교체:

```java
@Nested
@DisplayName("폼 태그 보존")
class FormTagPreservation {

    @Test
    @DisplayName("form, input, button 태그를 보존한다")
    void preservesFormElements() {
        String html = "<html><body><form action='/submit'><input type='text' name='name'><button type='submit'>전송</button></form></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("<form");
        assertThat(result).contains("<input");
        assertThat(result).contains("<button");
    }

    @Test
    @DisplayName("select, textarea, label 태그를 보존한다")
    void preservesSelectTextareaLabel() {
        String html = "<html><body><label for='name'>이름</label><textarea name='desc'>설명</textarea><select name='tier'><option value='1'>1티어</option></select></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("<label");
        assertThat(result).contains("<textarea");
        assertThat(result).contains("<select");
        assertThat(result).contains("<option");
    }

    @Test
    @DisplayName("fieldset, legend, datalist, output 태그를 보존한다")
    void preservesFieldsetAndRelated() {
        String html = "<html><body><fieldset><legend>정보</legend></fieldset><datalist id='d'><option value='a'></datalist><output>결과</output></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("<fieldset");
        assertThat(result).contains("<legend");
        assertThat(result).contains("<datalist");
        assertThat(result).contains("<output");
    }

    @Test
    @DisplayName("폼 요소의 속성을 보존한다")
    void preservesFormAttributes() {
        String html = "<html><body><input type='text' name='email' placeholder='이메일' required disabled></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("type=\"text\"");
        assertThat(result).contains("name=\"email\"");
        assertThat(result).contains("placeholder=\"이메일\"");
        assertThat(result).contains("required");
        assertThat(result).contains("disabled");
    }
}
```

- [ ] **Step 2: 테스트 실행하여 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentSanitizerTest" -i`
Expected: 전체 PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizerTest.java
git commit -m "test: 폼 태그 허용 정책에 맞게 sanitizer 테스트 업데이트"
```

---

### Task 2: CSP `connect-src 'none'` 주입

**Files:**
- Modify: `backend/src/main/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizer.java:158-176`
- Modify: `backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizerTest.java`

sanitizer가 조립하는 HTML head에 CSP meta 태그를 주입하여 fetch/XHR/WebSocket을 차단한다.

- [ ] **Step 1: CSP 주입 테스트 작성**

`JsoupContentSanitizerTest.java`에 새 Nested 클래스 추가:

```java
@Nested
@DisplayName("CSP 주입")
class CspInjection {

    @Test
    @DisplayName("connect-src 'none' CSP meta 태그를 head에 주입한다")
    void injectsConnectSrcNoneCsp() {
        String html = "<html><head><title>Test</title></head><body><p>hello</p></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("<meta http-equiv=\"Content-Security-Policy\" content=\"connect-src 'none';\">");
    }

    @Test
    @DisplayName("CSP meta 태그가 head의 가장 앞에 위치한다")
    void cspIsFirstInHead() {
        String html = "<html><head><title>Test</title><meta charset='utf-8'></head><body></body></html>";
        String result = sanitizer.sanitize(html);
        String headContent = result.substring(result.indexOf("<head>") + 6, result.indexOf("</head>"));
        assertThat(headContent.trim()).startsWith("<meta http-equiv=\"Content-Security-Policy\"");
    }

    @Test
    @DisplayName("공격자가 삽입한 CSP를 완화할 수 없다 (교집합 정책)")
    void attackerCannotRelaxCsp() {
        String html = "<html><head><meta http-equiv='Content-Security-Policy' content=\"connect-src *;\"><title>Test</title></head><body></body></html>";
        String result = sanitizer.sanitize(html);
        // 우리 CSP가 먼저 위치하므로, 브라우저는 교집합(connect-src 'none' ∩ connect-src *)을 적용 → none 유지
        String headContent = result.substring(result.indexOf("<head>") + 6, result.indexOf("</head>"));
        assertThat(headContent.trim()).startsWith("<meta http-equiv=\"Content-Security-Policy\" content=\"connect-src 'none';\">");
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentSanitizerTest.CspInjection" -i`
Expected: FAIL — CSP meta 태그가 아직 주입되지 않음

- [ ] **Step 3: assembleDocument에 CSP 주입 구현**

`JsoupContentSanitizer.java`의 `assembleDocument` 메서드에서 head에 CSP를 prepend:

```java
private static final String CSP_META = "<meta http-equiv=\"Content-Security-Policy\" content=\"connect-src 'none';\">";

private String assembleDocument(Document originalDocument, String headContent,
                                String bodyContent) {
    Document result = Document.createShell("");

    Element originalHtml = originalDocument.selectFirst("html");
    if (originalHtml != null) {
        originalHtml.attributes().forEach(attr ->
                result.selectFirst("html").attr(attr.getKey(), attr.getValue()));
    }

    // CSP meta 태그를 head 최상단에 주입 (connect-src 'none'으로 fetch/XHR/WebSocket 차단)
    result.head().html(CSP_META + "\n" + headContent);
    result.body().html(bodyContent);

    return result.html();
}
```

`CSP_META` 상수는 클래스 상단(`HEAD_ALLOWED_TAGS` 아래)에 추가한다.

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentSanitizerTest" -i`
Expected: 전체 PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizer.java
git add backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizerTest.java
git commit -m "feat: sanitizer에 CSP connect-src 'none' meta 태그 주입"
```

---

### Task 3: `type="password"` → `type="text"` 변환

**Files:**
- Modify: `backend/src/main/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizer.java`
- Modify: `backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizerTest.java`

password input의 마스킹을 제거하여 피싱 효과를 무력화한다.

- [ ] **Step 1: password 변환 테스트 작성**

`JsoupContentSanitizerTest.java`에 새 Nested 클래스 추가:

```java
@Nested
@DisplayName("password input 무력화")
class PasswordInputNeutralization {

    @Test
    @DisplayName("type=password를 type=text로 변환한다")
    void convertsPasswordToText() {
        String html = "<html><body><input type='password' name='pw'></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("type=\"text\"");
        assertThat(result).doesNotContain("type=\"password\"");
    }

    @Test
    @DisplayName("대소문자 구분 없이 PASSWORD도 변환한다")
    void convertsCaseInsensitive() {
        String html = "<html><body><input type='Password' name='pw'></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("type=\"text\"");
        assertThat(result).doesNotContain("Password");
    }

    @Test
    @DisplayName("type=text인 input은 변경하지 않는다")
    void doesNotChangeTextInput() {
        String html = "<html><body><input type='text' name='name' value='hello'></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("type=\"text\"");
        assertThat(result).contains("name=\"name\"");
    }

    @Test
    @DisplayName("다른 타입의 input은 변경하지 않는다")
    void doesNotChangeOtherInputTypes() {
        String html = "<html><body><input type='email' name='email'><input type='number' name='num'></body></html>";
        String result = sanitizer.sanitize(html);
        assertThat(result).contains("type=\"email\"");
        assertThat(result).contains("type=\"number\"");
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentSanitizerTest.PasswordInputNeutralization" -i`
Expected: FAIL — password가 아직 text로 변환되지 않음

- [ ] **Step 3: sanitize 메서드에 password 변환 구현**

`JsoupContentSanitizer.java`의 `sanitize()` 메서드에서 body 영역 clean 전에 password input을 변환:

```java
@Override
public String sanitize(String html) {
    if (html == null || html.isBlank()) {
        return html;
    }

    Document document = Jsoup.parse(html);

    // 1. head 영역 새니타이징
    String sanitizedHead = sanitizeHead(document);

    // 2. body 영역: password input 무력화 후 Safelist 기반 clean() 적용
    Element body = document.body();
    if (body != null) {
        neutralizePasswordInputs(body);
    }
    String bodyHtml = body != null ? body.html() : "";
    String sanitizedBody = Jsoup.clean(bodyHtml, "", BODY_SAFELIST,
            new Document.OutputSettings().prettyPrint(false));

    // 3. 완전한 HTML 문서로 재조립
    return assembleDocument(document, sanitizedHead, sanitizedBody);
}
```

새 private 메서드 추가 (`sanitizeHead` 메서드 위에):

```java
/**
 * 피싱 방지를 위해 type="password" input을 type="text"로 변환한다.
 * 마스킹(●●●●)을 제거하여 사용자가 진짜 로그인 폼으로 착각하는 것을 방지한다.
 */
private void neutralizePasswordInputs(Element parent) {
    for (Element input : parent.select("input[type=password]")) {
        input.attr("type", "text");
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentSanitizerTest" -i`
Expected: 전체 PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizer.java
git add backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentSanitizerTest.java
git commit -m "feat: type=password를 type=text로 변환하여 피싱 UI 무력화"
```

---

### Task 4: ContentValidator 포트 및 MaliciousContentException 생성

**Files:**
- Create: `backend/src/main/java/com/drafturl/api/domain/document/port/ContentValidator.java`
- Create: `backend/src/main/java/com/drafturl/api/domain/document/exception/MaliciousContentException.java`

콘텐츠 검증 포트 인터페이스와 악성 콘텐츠 예외를 정의한다.

- [ ] **Step 1: MaliciousContentException 생성**

```java
package com.drafturl.api.domain.document.exception;

import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MaliciousContentException extends BusinessException {

    public MaliciousContentException(String reason) {
        super(HttpStatus.BAD_REQUEST, "MALICIOUS_CONTENT",
                "보안 정책에 의해 차단된 콘텐츠입니다: " + reason);
    }
}
```

- [ ] **Step 2: ContentValidator 포트 인터페이스 생성**

```java
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
```

- [ ] **Step 3: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/document/port/ContentValidator.java
git add backend/src/main/java/com/drafturl/api/domain/document/exception/MaliciousContentException.java
git commit -m "feat: ContentValidator 포트 및 MaliciousContentException 추가"
```

---

### Task 5: 피싱 탐지 구현 (브랜드 사칭 + password)

**Files:**
- Create: `backend/src/main/java/com/drafturl/api/infra/sanitizer/JsoupContentValidator.java`
- Create: `backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentValidatorTest.java`

브랜드명(네이버, 구글, 카카오 등) + password input 조합을 탐지하여 업로드를 거부한다.

- [ ] **Step 1: 피싱 탐지 테스트 작성**

```java
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
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentValidatorTest" -i`
Expected: FAIL — 클래스가 아직 없음

- [ ] **Step 3: JsoupContentValidator 구현**

```java
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

    /**
     * 피싱 탐지 대상 브랜드 키워드 (소문자).
     */
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

    /**
     * 피싱 탐지: 브랜드명 + password input 조합.
     * 둘 다 존재하면 피싱으로 판단한다.
     */
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

    /**
     * 악성 리다이렉트 탐지: meta refresh 외부 URL, JS location 변경.
     */
    private void detectMaliciousRedirect(String html) {
        if (META_REFRESH_PATTERN.matcher(html).find()) {
            throw new MaliciousContentException("외부 URL로의 자동 리다이렉트 (meta refresh)");
        }

        if (JS_REDIRECT_PATTERN.matcher(html).find()) {
            throw new MaliciousContentException("외부 URL로의 자동 리다이렉트 (JavaScript)");
        }
    }
}
```

- [ ] **Step 4: 테스트 실행하여 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentValidatorTest" -i`
Expected: 전체 PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/infra/sanitizer/JsoupContentValidator.java
git add backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentValidatorTest.java
git commit -m "feat: 피싱 탐지 구현 — 브랜드 사칭 + password input 조합 차단"
```

---

### Task 6: 악성 리다이렉트 탐지 테스트 추가

**Files:**
- Modify: `backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentValidatorTest.java`

Task 5에서 리다이렉트 탐지 로직을 이미 구현했으므로, 테스트만 추가한다.

- [ ] **Step 1: 악성 리다이렉트 탐지 테스트 추가**

`JsoupContentValidatorTest.java`에 새 Nested 클래스 추가:

```java
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
```

- [ ] **Step 2: 테스트 실행하여 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.infra.sanitizer.JsoupContentValidatorTest" -i`
Expected: 전체 PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/drafturl/api/infra/sanitizer/JsoupContentValidatorTest.java
git commit -m "test: 악성 리다이렉트 탐지 테스트 추가"
```

---

### Task 7: UseCase에 ContentValidator 통합

**Files:**
- Modify: `backend/src/main/java/com/drafturl/api/domain/document/usecase/CreateDocumentUseCase.java`
- Modify: `backend/src/main/java/com/drafturl/api/domain/document/usecase/UpdateDocumentUseCase.java`
- Modify: `backend/src/test/java/com/drafturl/api/domain/document/usecase/CreateDocumentUseCaseTest.java`
- Modify: `backend/src/test/java/com/drafturl/api/domain/document/usecase/UpdateDocumentUseCaseTest.java`

문서 생성/수정 시 sanitize 전에 validate를 호출하여 악성 콘텐츠를 사전 차단한다.

- [ ] **Step 1: CreateDocumentUseCaseTest에 검증 테스트 추가**

`CreateDocumentUseCaseTest.java`에서:

1. `@Mock private ContentValidator contentValidator;` 추가
2. `setUp()`의 `new CreateDocumentUseCase(...)` 호출에 `contentValidator` 파라미터 추가
3. 새 Nested 클래스 추가:

```java
@Nested
@DisplayName("악성 콘텐츠 검증")
class MaliciousContentValidation {

    @Test
    @DisplayName("HTML 문서 생성 시 ContentValidator를 호출한다")
    void callsContentValidatorForHtml() {
        UUID userId = UUID.randomUUID();
        var request = new CreateDocumentRequest("<h1>Hello</h1>", "html", "테스트", null);
        Document doc = buildDocument(userId, "테스트", DocType.HTML, 15L, DocumentStatus.ACTIVE, null, null);

        when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                any(DocType.class), anyString(), anyLong(), any(), any())).thenReturn(doc);
        when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

        useCase.execute(request, userId);

        verify(contentValidator).validate("<h1>Hello</h1>");
    }

    @Test
    @DisplayName("Markdown 문서 생성 시 ContentValidator를 호출하지 않는다")
    void skipsContentValidatorForMarkdown() {
        UUID userId = UUID.randomUUID();
        var request = new CreateDocumentRequest("# Hello", "markdown", "MD", null);
        Document doc = buildDocument(userId, "MD", DocType.MARKDOWN, 7L, DocumentStatus.ACTIVE, null, null);

        when(txService.insertPendingDocument(anyString(), anyString(), any(), anyString(),
                eq(DocType.MARKDOWN), anyString(), anyLong(), any(), any())).thenReturn(doc);
        when(txService.activateDocument(anyString(), any(), anyLong())).thenReturn(doc);

        useCase.execute(request, userId);

        verify(contentValidator, never()).validate(anyString());
    }

    @Test
    @DisplayName("ContentValidator가 예외를 던지면 문서가 생성되지 않는다")
    void blocksDocumentCreationOnMaliciousContent() {
        UUID userId = UUID.randomUUID();
        var request = new CreateDocumentRequest("<input type='password'>", "html", "피싱", null);

        doThrow(new MaliciousContentException("피싱 탐지"))
                .when(contentValidator).validate(anyString());

        assertThatThrownBy(() -> useCase.execute(request, userId))
                .isInstanceOf(MaliciousContentException.class);
        verify(txService, never()).insertPendingDocument(
                anyString(), anyString(), any(), anyString(),
                any(DocType.class), anyString(), anyLong(), any(), any());
    }
}
```

- [ ] **Step 2: 테스트 실행하여 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.drafturl.api.domain.document.usecase.CreateDocumentUseCaseTest.MaliciousContentValidation" -i`
Expected: FAIL — 생성자에 contentValidator 파라미터가 없음

- [ ] **Step 3: CreateDocumentUseCase에 ContentValidator 통합**

`CreateDocumentUseCase.java`에서:

1. `ContentValidator` 필드 및 생성자 파라미터 추가
2. `execute()` 메서드의 sanitize 호출 전에 validate 호출 추가:

```java
// HTML 콘텐츠 보안 검증: 피싱, 악성 리다이렉트 탐지
if (docType == DocType.HTML) {
    contentValidator.validate(content);
    content = contentSanitizer.sanitize(content);
    contentBytes = content.getBytes(StandardCharsets.UTF_8);
}
```

- [ ] **Step 4: UpdateDocumentUseCase에도 동일하게 통합**

`UpdateDocumentUseCase.java`에서:

1. `ContentValidator` 필드 및 생성자 파라미터 추가
2. sanitize 호출 전에 validate 호출 추가:

```java
if (document.getDocType() == DocType.HTML) {
    contentValidator.validate(content);
    content = contentSanitizer.sanitize(content);
    contentBytes = content.getBytes(StandardCharsets.UTF_8);
}
```

- [ ] **Step 5: CreateDocumentUseCaseTest setUp 업데이트**

```java
@Mock private ContentValidator contentValidator;

@BeforeEach
void setUp() {
    useCase = new CreateDocumentUseCase(
            slugGenerator, contentSanitizer, contentValidator, fileStorage,
            txService, passwordEncoder, storageUsageService, FRONTEND_URL);
    lenient().when(slugGenerator.generate()).thenReturn(TEST_SLUG);
    lenient().when(contentSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
}
```

import 추가:
```java
import com.drafturl.api.domain.document.port.ContentValidator;
import com.drafturl.api.domain.document.exception.MaliciousContentException;
```

- [ ] **Step 6: 전체 테스트 실행하여 통과 확인**

Run: `cd backend && ./gradlew test -i`
Expected: 전체 PASS

- [ ] **Step 7: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/document/usecase/CreateDocumentUseCase.java
git add backend/src/main/java/com/drafturl/api/domain/document/usecase/UpdateDocumentUseCase.java
git add backend/src/test/java/com/drafturl/api/domain/document/usecase/CreateDocumentUseCaseTest.java
git add backend/src/test/java/com/drafturl/api/domain/document/usecase/UpdateDocumentUseCaseTest.java
git commit -m "feat: UseCase에 ContentValidator 통합 — 악성 콘텐츠 업로드 사전 차단"
```

---

### Task 8: Javadoc 및 보안 문서 업데이트

**Files:**
- Modify: `backend/src/main/java/com/drafturl/api/domain/document/port/ContentSanitizer.java`
- Modify: `backend/src/main/java/com/drafturl/api/domain/document/usecase/CreateDocumentUseCase.java` (주석만)

- [ ] **Step 1: ContentSanitizer Javadoc 업데이트**

`ContentSanitizer.java`의 Javadoc을 현재 정책에 맞게 업데이트:

```java
/**
 * HTML 콘텐츠 새니타이저 Port 인터페이스.
 * 도메인 계층에서 정의하며, 인프라 계층(JsoupContentSanitizer)에서 구현한다.
 *
 * <p>보안 정책:
 * <ul>
 *   <li>head 영역: script 태그 제거, meta/title/style/link 허용</li>
 *   <li>body 영역: Safelist 기반 clean() 적용 (iframe/object/embed/javascript: URL 제거)</li>
 *   <li>script/style/폼 태그는 body에서 허용 (sandbox iframe에서 격리)</li>
 *   <li>type="password" input을 type="text"로 변환 (피싱 UI 무력화)</li>
 *   <li>CSP connect-src 'none' meta 태그 주입 (fetch/XHR/WebSocket 차단)</li>
 * </ul>
 */
```

- [ ] **Step 2: CreateDocumentUseCase 주석 업데이트**

기존 sanitize 관련 주석을 업데이트:

```java
// HTML 콘텐츠 보안: 악성 패턴 탐지(validate) → 정화(sanitize)
// validate: 피싱(브랜드 사칭 + password), 악성 리다이렉트 탐지 → 차단
// sanitize: CSP 주입, password→text 변환, 위험 태그 제거
if (docType == DocType.HTML) {
    contentValidator.validate(content);
    content = contentSanitizer.sanitize(content);
    contentBytes = content.getBytes(StandardCharsets.UTF_8);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/document/port/ContentSanitizer.java
git add backend/src/main/java/com/drafturl/api/domain/document/usecase/CreateDocumentUseCase.java
git commit -m "docs: sanitizer/validator 보안 정책 Javadoc 업데이트"
```
