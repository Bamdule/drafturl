# C-4 JsoupContentSanitizer Safelist 미사용 -- 수정 및 자체 리뷰

> 작성일: 2026-03-23
> 상태: 완료

---

## 수정 요약

`JsoupContentSanitizer`에서 `Jsoup.parse()` + 수동 태그 제거 방식을 `Jsoup.clean()` Safelist 기반으로 전환하여 다층 방어를 구현했다.

### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `JsoupContentSanitizer.java` | Safelist 기반 clean() 적용, head/body 분리 처리 |
| `ContentSanitizer.java` | 인터페이스 Javadoc 업데이트 |
| `JsoupContentSanitizerTest.java` | 신규 -- 17개 테스트 케이스 |

---

## 수정 내용

### 1. 핵심 변경: Jsoup.clean() 적용

- **이전**: `Jsoup.parse()` + `document.select("iframe, object, embed").remove()` -- 수동 제거 방식
- **이후**: `Jsoup.clean(bodyHtml, "", BODY_SAFELIST, ...)` -- Safelist 기반 화이트리스트 방식

### 2. head/body 분리 처리

`Jsoup.clean()`은 body fragment만 반환하므로, 전체 HTML 문서 구조를 보존하기 위해 분리 처리:

- **head**: meta/title/style/link만 허용, script 제거 (head 내 script는 트래커 등 위험성 높음)
- **body**: BODY_SAFELIST로 clean() 적용 (script/style 허용, iframe/object/embed 차단)
- **재조립**: `Document.createShell()`로 html 속성까지 복원한 완전한 HTML 문서 생성

### 3. BODY_SAFELIST 구성

`Safelist.relaxed()` 기반으로 다음을 추가:
- script/style/canvas -- LLM 생성 HTML의 인터랙티브 요소 지원
- SVG 관련 태그 및 속성 -- 차트, 다이어그램 지원
- HTML5 시맨틱 태그 (nav, header, footer, section, article 등)
- media 태그 (video, audio, source)
- protocol 화이트리스트: http/https/mailto/data만 허용 (javascript: 자동 차단)

---

## 자체 리뷰

### 검증 항목

| 항목 | 결과 | 비고 |
|------|------|------|
| 설계 정책 준수 (security.md 섹션 5) | OK | script 허용 + iframe/object/embed 제거 + javascript: URL 차단 |
| Jsoup.clean() 실제 사용 | OK | body 영역에 BODY_SAFELIST 적용 |
| 미사용 코드 없음 | OK | SAFELIST가 실제로 clean()에 전달됨 |
| HTML 문서 구조 보존 | OK | head/body 분리 + 재조립으로 완전한 HTML 유지 |
| ConcurrentModification 방지 | OK | stream().toList() 수집 후 제거 |
| 컴파일 성공 | OK | `./gradlew compileJava` 통과 |

### 잠재적 고려사항

1. **HEAD_ALLOWED_TAGS의 contains() 방식**: `"meta, title, style, link".contains(tagName)` 방식은 `"ink"` 같은 부분 문자열도 매칭될 수 있으나, HTML 표준 태그명에는 이런 충돌이 없어 실질적 위험은 없다. 향후 Set 기반으로 전환 고려 가능.

2. **onEvent 인라인 핸들러**: `<div onclick="alert(1)">` 같은 인라인 이벤트 핸들러는 Safelist에 해당 속성이 없으므로 자동 제거된다. 기존 코드에서는 제거되지 않았던 부분으로, 보안이 강화되었다.

3. **data-* 속성**: Safelist에 명시적으로 추가한 data-* 속성만 허용된다. 임의의 data-* 속성은 제거된다. LLM이 생성하는 다양한 data-* 속성이 필요하면 추후 확장 필요.
