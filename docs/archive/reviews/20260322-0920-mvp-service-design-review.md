## 검토 결과

> 검토일: 2026-03-21
> 검토 대상: [architecture/](../architecture/) (구 design/mvp-service-design.md, Spring Boot + Next.js 분리 아키텍처)
> 참조 문서: [product.md](../product.md) (구 plans/service-plan.md)

### 이전 검토 결과(Critical 3건) 반영 확인

이전 검토에서 식별된 Critical 3건이 모두 적절히 반영되었다.

| 검토 ID | 이전 문제 | 반영 방법 | 평가 |
|---------|----------|-----------|------|
| C-1 | 고아 R2 파일 (R2 업로드 성공 후 DB INSERT 실패) | PENDING/ACTIVE 2단계 커밋 + 스케줄러 정리 (섹션 6.1, 6.5, DR-9) | 완전 반영 |
| C-2 | `allow-same-origin` sandbox 무력화 | `sandbox="allow-scripts"`만 사용, 트레이드오프 명시 (섹션 6.2) | 완전 반영 |
| C-3 | CRON 엔드포인트 외부 HTTP 인증 누락 | Spring `@Scheduled`로 전환하여 외부 엔드포인트 자체를 제거 (섹션 6.5) | 구조적으로 해결 |

이전 Warning 6건, Suggestion 7건 반영 여부도 각 섹션에 "(기존 W-N 반영)", "(기존 S-N 반영)" 주석으로 명시되어 있어 추적 가능성이 높다.

---

### Critical (치명적 문제)

#### C-1: OAuth2 state 파라미터 검증 로직 명세 누락 (CSRF 취약점)

**위치**: 섹션 5.1 OAuth2 인증 플로우, 섹션 4.4 `POST /api/v1/auth/oauth2/callback/{provider}`

**문제**: 시퀀스 다이어그램에서 `state={csrf_token}`을 OAuth2 요청에 포함한다고 명시했지만, Spring Boot의 콜백 핸들러(`POST /api/v1/auth/oauth2/callback/{provider}`)가 `state` 파라미터를 수신하고 검증하는 방법이 전혀 설계되어 있지 않다.

현재 콜백 API의 요청 body:
```json
{
  "code": "authorization_code_from_oauth_provider",
  "redirectUri": "https://{서비스도메인}/auth/callback"
}
```

`state` 필드가 없다. `state`를 검증하지 않으면 공격자가 피해자를 자신의 code로 로그인시켜 세션을 탈취하는 CSRF 공격(OAuth2 인가 코드 주입)이 가능하다.

**개선 방법**:
1. 로그인 시작 시 Next.js에서 cryptographically random `state` 값을 생성하여 `httpOnly` 세션 쿠키에 저장한다.
2. OAuth2 요청 URL에 `state` 파라미터를 포함한다.
3. 콜백 시 URL의 `state`와 쿠키의 `state`를 비교하여 일치하지 않으면 요청을 거부한다.
4. 콜백 API 요청 body에 `state` 필드를 추가하고, `OAuthCallbackRequest.java`에 포함한다.

```json
// 수정된 콜백 요청 body
{
  "code": "authorization_code_from_oauth_provider",
  "redirectUri": "https://{서비스도메인}/auth/callback",
  "state": "랜덤 state 값"
}
```

Spring Boot에서는 `AuthService`의 OAuth2 콜백 처리에 state 검증 단계를 명시적으로 추가해야 한다.

---

#### C-2: Refresh Token Rotation 구현 명세 불완전 (보안 취약점)

**위치**: 섹션 11.3 인증 보안 (`리프레시 토큰 재사용` 항목), 섹션 5.3 Refresh Token 테이블

**문제**: 11.3에 "사용 시 교체 (rotation)"라고 언급했지만, 구체적인 구현 명세가 없다. rotation의 핵심인 이전 토큰 즉시 무효화와 새 토큰 발급이 원자적으로 처리되지 않으면 Race Condition이 발생할 수 있다.

또한 현재 `refresh_tokens` 테이블 스키마에 `revoked` 또는 `used_at` 컬럼이 없어서 rotation 구현이 불가능하다.

**개선 방법**:

```sql
-- refresh_tokens 테이블에 컬럼 추가
ALTER TABLE refresh_tokens ADD COLUMN is_revoked BOOLEAN NOT NULL DEFAULT FALSE;
```

Rotation 로직:
1. 클라이언트가 `POST /api/v1/auth/refresh`로 Refresh Token 전송
2. DB에서 토큰 조회: `is_revoked = true`이면 즉시 거부 (재사용 의심 → 해당 사용자의 모든 토큰 무효화 검토)
3. 만료 확인
4. 새 Access Token + 새 Refresh Token 발급
5. 기존 Refresh Token: `is_revoked = true` 업데이트
6. 새 Refresh Token DB INSERT
7. 응답 반환

Refresh Token 테이블 설계와 `AuthService` 명세에 이 흐름을 명시해야 한다.

---

### Warning (주요 개선사항)

#### W-1: 편집용 문서 조회 API에서 대용량 콘텐츠 JSON body 포함 문제

**위치**: 섹션 4.3 `GET /api/v1/documents/{slug}` 응답, 섹션 10.3 `DocumentService.createDocument`

**문제**: 편집용 문서 조회 응답에 `content` 필드(최대 5MB HTML 문자열)가 JSON body에 포함된다. 5MB 텍스트를 JSON 문자열로 직렬화하면 이스케이핑으로 인해 실제 페이로드가 더 커질 수 있으며, Spring Boot와 Next.js 양쪽에서 메모리에 올려 처리해야 하므로 대용량 문서에서 응답 지연과 메모리 압박이 발생한다.

**개선 방법**: 두 가지 선택지가 있다.
- 방법 A: 콘텐츠를 별도 API로 분리한다. `GET /api/v1/documents/{slug}/content`에서 `Content-Type: text/plain` (HTML) 또는 `text/markdown`으로 원본 텍스트를 직접 반환한다.
- 방법 B: 현재 설계를 유지하되, `@Size` 검증 외에 Spring Boot의 `spring.servlet.multipart.max-request-size`를 명시적으로 설정하고, Jackson의 스트리밍 API 사용을 검토한다.

MVP 규모(일 100개 문서, 평균 50KB)에서는 방법 B로 충분하나, 5MB 문서를 자주 편집하는 사용자가 있을 경우 방법 A가 필요하다. 현재 문서에 이 트레이드오프가 언급되어 있지 않으므로 의도적 선택임을 명시해야 한다.

---

#### W-2: sandbox iframe에서 외부 네트워크 요청 차단 누락

**위치**: 섹션 6.2 HTML 서빙 래퍼, 섹션 11.2 HTML 서빙 보안

**문제**: `sandbox="allow-scripts"`를 사용하면 스크립트 실행을 허용하므로 iframe 내 JavaScript가 `fetch()`, `XMLHttpRequest`로 외부 서버에 요청을 보낼 수 있다. 악의적인 HTML 업로더가 방문자의 브라우저 정보, IP, 또는 브라우저에 저장된 일부 데이터를 외부로 유출할 수 있다.

`allow-scripts`가 네트워크 요청을 차단하지 않는다는 점은 설계 문서에 언급되어 있지 않다.

**개선 방법**:
```html
<!-- Content-Security-Policy 헤더 추가 -->
Content-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'
```

Next.js의 `[slug]/page.tsx`에서 HTML 서빙 래퍼 페이지를 렌더링할 때 위 CSP 헤더를 추가하면 iframe 내에서 외부 리소스 로드와 네트워크 요청을 차단할 수 있다. 단, LLM 생성 HTML이 외부 CDN의 CSS/JS를 참조하는 경우 렌더링이 깨질 수 있으므로 이 트레이드오프를 문서에 명시해야 한다.

---

#### W-3: 만료 체크 시 동시 요청에 의한 중복 DB UPDATE

**위치**: 섹션 6.2 `getDocumentForView` 로직, 섹션 10.3

**문제**: `getDocumentForView`에서 `expiresAt < now`이면 `status = EXPIRED`로 DB를 업데이트하고 응답을 반환한다. 같은 만료 직후 슬러그에 다수의 동시 요청이 들어오면 여러 스레드가 동시에 같은 문서에 대해 `status = EXPIRED` UPDATE를 시도한다. 기능적으로 문제없지만(결과는 동일), 불필요한 DB 쓰기가 증가한다.

**개선 방법**: 스케줄러가 이미 만료 처리를 담당하므로, 서빙 경로에서는 상태 변경 없이 만료 판단만 하는 방식을 검토할 수 있다.

```java
// 수정 예시: DB 업데이트 없이 만료 판단
if (document.getExpiresAt() != null && document.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new DocumentExpiredException();
}
// status=EXPIRED 업데이트는 스케줄러에만 위임
```

이렇게 하면 서빙 경로가 읽기 전용이 되어 성능이 향상되고, 스케줄러가 만료 처리의 단일 책임자가 된다. 단, 스케줄러 주기(1시간)만큼 `status` 컬럼의 상태가 늦게 반영되므로, 만료 판단은 항상 `expiresAt` 값 기준으로 해야 함을 명시해야 한다.

---

#### W-4: `storageUsage.documentCount` 부정확성 위험

**위치**: 섹션 3.2 StorageUsage 엔티티, 섹션 10.3 `StorageUsageService`

**문제**: `documentCount`는 사용자의 문서 수를 캐싱한 카운터다. `createDocument`, `updateDocument`, `deleteDocument`, 스케줄러의 만료 처리 등 여러 경로에서 이 카운터를 업데이트한다. 트랜잭션 범위 외부에서 R2 작업이 실패하고 재시도가 없을 경우, 또는 스케줄러가 `EXPIRED`로 처리하면서 카운터를 감소시키지 않으면 실제 ACTIVE 문서 수와 `documentCount`가 불일치할 수 있다.

**개선 방법**:
1. `DocumentCleanupScheduler`의 만료 처리 시 로그인 사용자(`user_id IS NOT NULL`)의 문서에 대해 `storage_usage.document_count` 감소 처리를 명시적으로 추가한다.
2. 대시보드의 "사용량" 표시는 `storage_usage` 카운터 대신 `SELECT COUNT(*) FROM documents WHERE user_id = ? AND status = 'active'` 쿼리로 실시간 조회하는 방안을 검토한다 (MVP 규모에서 성능 문제 없음).

---

#### W-5: Java용 nanoid 라이브러리 의존성 명세 없음

**위치**: 섹션 10.1 패키지 구조 (`SlugGenerator.java`), 섹션 14.1 구현 순서 (Step 2 의존성 설정)

**문제**: 구현 가이드 Step 2에 "nanoid" 의존성을 추가하라고 명시했지만, JavaScript 원본 nanoid(`github.com/ai/nanoid`)는 Java 라이브러리가 아니다. Java/Kotlin에서 사용 가능한 nanoid 구현체가 별도로 필요한데, 어떤 라이브러리를 사용할지 명세가 없다.

**개선 방법**: 다음 중 하나를 선택하고 명시해야 한다.
- `com.aventrix.jnanoid:jnanoid` (Gradle 의존성 추가)
- `UUID.randomUUID()`를 Base62 인코딩하여 직접 구현 (외부 의존성 없음, URL-safe 보장)
- `SecureRandom` + 커스텀 문자셋으로 직접 구현

직접 구현 예시:
```java
private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
private static final SecureRandom RANDOM = new SecureRandom();

public String generate(int size) {
    StringBuilder sb = new StringBuilder(size);
    for (int i = 0; i < size; i++) {
        sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return sb.toString();
}
```

---

### Suggestion (사소한 개선사항)

#### S-1: HtmlSanitizer 구현 방법 재검토 필요

**위치**: 섹션 14.2 주의사항 (`HTML 새니타이징` 항목)

**문제**: "Java에서 정규식 기반 경량 처리"로 구현한다고 명시했다. 그러나 HTML 파싱을 정규식으로 처리하면 nested 태그, 속성 내 특수문자, 비표준 HTML 등에서 우회가 가능하다. 문서 자체에서도 "jsoup 등 HTML 파서 라이브러리 사용도 검토 가능"이라고 언급하고 있어, 정규식 선택의 근거가 약하다.

**개선 방법**: jsoup는 Spring Boot 생태계에서 널리 사용되며 의존성 추가가 간단하다. MVP에서 `jsoup` 사용을 권장한다.

```gradle
implementation 'org.jsoup:jsoup:1.17.2'
```

```java
// jsoup 화이트리스트 기반 새니타이징 예시
Whitelist whitelist = Whitelist.relaxed()
    .removeAttributes(":all", "onload", "onclick", "onerror"); // 인라인 이벤트 제거
    // iframe, object, embed는 relaxed에서 기본 제외됨
String cleaned = Jsoup.clean(rawHtml, whitelist);
```

단, 섹션 7.3에서 "나머지는 그대로 유지 (`<script>`, `<style>`, 인라인 이벤트 허용)"라고 명시했는데, jsoup의 `Whitelist.relaxed()`는 `<script>` 태그를 제거한다. `allow-scripts` sandbox가 있으므로 `<script>` 허용은 의도적 설계 결정이다. jsoup를 사용하면서 `<script>` 태그를 허용하려면 커스텀 화이트리스트 설정이 필요하다. 이 트레이드오프를 명시적으로 기록해야 한다.

---

#### S-2: `DELETE` 응답에서 `deletedAt` 필드의 출처 명확화

**위치**: 섹션 4.3 `DELETE /api/v1/documents/{slug}` 응답

**문제**: 응답에 `deletedAt`이 포함되어 있고 "soft delete 시점의 `updatedAt` 값"이라고 명시했다. 별도 `deleted_at` 컬럼을 두지 않는 결정은 합리적이나, `DocumentResponse` DTO에서 `deletedAt`이 `updatedAt`의 alias임을 코드 수준에서 명시하지 않으면 혼란이 생길 수 있다.

**개선 방법**: `DocumentResponse.java`의 `deletedAt` 필드는 삭제 성공 응답 전용 DTO인 `DeleteDocumentResponse.java`로 분리하여 일반 `DocumentResponse`와의 혼용을 방지한다. 또는 응답에서 `deletedAt`을 제거하고 `updatedAt`으로 통일한다.

---

#### S-3: 스케줄러 배치 처리에서 R2 삭제 실패 시 재시도 전략 미명세

**위치**: 섹션 6.5 `DocumentCleanupScheduler`

**문제**: 스케줄러가 R2 파일 삭제 + DB 상태 변경을 순차 처리하는데, R2 삭제가 실패하면 DB UPDATE만 성공하여 DB는 EXPIRED지만 R2에 파일이 남는 역방향 고아 파일이 발생할 수 있다. 로직의 "R2 파일 삭제 시도"에서 실패 시 처리 방법이 명세에 없다.

**개선 방법**: R2 삭제 실패 시 해당 문서를 skip하고 다음 배치에서 재시도하도록 명시한다. DB 상태 변경은 R2 삭제 성공 후에만 수행하거나, R2 삭제 실패를 로깅하고 별도 데드레터 처리 방안을 기록한다.

---

#### S-4: `GET /api/v1/documents` 목록에서 `status` 필터 기본값 미명시

**위치**: 섹션 4.3 `GET /api/v1/documents` 요청 파라미터

**문제**: 내 문서 목록 API의 쿼리 파라미터에 `status` 필터가 없다. `DELETED`, `EXPIRED`, `PENDING` 문서가 목록에 노출되는지 여부가 명시되어 있지 않다. 기본적으로 `ACTIVE` 문서만 반환해야 하지만 이 제약이 API 명세에 없다.

**개선 방법**: API 명세에 "인증된 사용자의 `ACTIVE` 상태 문서만 반환한다"를 명시적으로 추가한다. 또는 선택적 `status` 쿼리 파라미터를 추가하여 기본값을 `active`로 설정한다.

---

#### S-5: `middleware.ts` 토큰 만료 감지 방법 미명세

**위치**: 섹션 9.4 대시보드, 섹션 5.4 프론트엔드 토큰 관리

**문제**: `middleware.ts`에서 JWT 쿠키 존재 여부를 확인한다고 명시했지만, 쿠키가 존재하더라도 Access Token이 만료된 경우의 처리가 middleware 레벨에서 어떻게 이루어지는지 설명이 없다. 만료 토큰으로 Spring Boot API를 호출하면 401이 반환되고, 이를 `client.ts`에서 catch하여 refresh를 시도한다고 섹션 9.2에 명시되어 있으나, server component에서 호출할 때 middleware와 client.ts의 역할 분담이 불명확하다.

**개선 방법**: middleware에서는 쿠키 존재 여부만 확인하고 만료 여부 검증은 `client.ts`의 401 처리 로직에 위임한다는 결정을 명시한다. 또는 middleware에서 JWT decode(검증 없이)로 `exp` 클레임을 확인하여 만료 임박 시 미리 refresh를 시도하는 방식을 기술한다.

---

### 전체 평가

이 설계 문서는 이전 검토 결과를 충실히 반영했으며, Spring Boot 아키텍처로의 전환 근거와 트레이드오프가 명확하게 기술되어 있다. 도메인 규칙(DR-1~DR-9), 에러 코드 총괄, 트랜잭션 범위 명시, 의사결정 기록(섹션 16) 등 구현 착수에 필요한 상세도를 갖추고 있다.

신규 Critical 2건(C-1: OAuth2 state 검증 누락, C-2: Refresh Token Rotation 불완전)은 보안 관련 항목으로 구현 전 반드시 해결해야 한다. 나머지 Warning과 Suggestion은 구현 진행 중 또는 이후에 반영해도 무방하다.

| 심각도 | 건수 | 이전 검토 대비 |
|--------|------|---------------|
| Critical | 2건 | 이전 3건 모두 해결, 신규 2건 발생 |
| Warning | 5건 | 신규 발견 |
| Suggestion | 5건 | 신규 발견 |

---

## 수정 이력

> 수정일: 2026-03-21
> 수정 근거: 검토 결과 섹션의 Critical 2건 + Warning 5건 반영 + 서비스명 변경

### Critical 반영 (2건)

| 검토 ID | 수정 내용 | 수정 위치 |
|---------|----------|-----------|
| C-1 | OAuth2 state 검증 흐름 추가 | 섹션 4.4 콜백 API 요청 body에 `state` 필드 추가 + 검증 실패 에러 응답 추가. 섹션 5.1에 `5.1.1 OAuth2 State (CSRF 방어) 흐름` 서브섹션 신설하여 state 생명주기(생성/쿠키저장/검증/삭제) 전체 흐름 명시. 시퀀스 다이어그램에 state 생성/검증/쿠키 삭제 단계 추가. 섹션 9.4 OAuth2 콜백 페이지에 state 검증 단계 추가. 에러 코드에 `INVALID_OAUTH_STATE` 추가. 섹션 11.3에 `CSRF (OAuth2)` 행 추가 |
| C-2 | Refresh Token Rotation 구현 명세 | 섹션 5.3 `refresh_tokens` DDL에 `is_revoked BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 추가. `5.3.1 Refresh Token Rotation 흐름` 서브섹션 신설하여 정상 rotation + 탈취 감지 시나리오 명세. 섹션 4.4 토큰 갱신 API 응답에 `refreshToken` 필드 추가 + 에러 응답 테이블 추가. 섹션 7.1 ERD에 `is_revoked` 컬럼 추가. 섹션 7.2 DDL에 `is_revoked` 컬럼 추가. 에러 코드에 `TOKEN_EXPIRED`, `TOKEN_REUSE_DETECTED` 추가. 섹션 11.3 인증 보안 테이블의 rotation 설명 구체화 |

### Warning 반영 (5건)

| 검토 ID | 수정 내용 | 수정 위치 |
|---------|----------|-----------|
| W-1 | 편집용 조회 API의 대용량 JSON body 트레이드오프 명시 | 섹션 4.3 `GET /api/v1/documents/{slug}` 설명에 트레이드오프 주석 추가. MVP에서는 현재 설계 유지, Phase 2에서 콘텐츠 분리 API 검토 명시. `spring.servlet.multipart.max-request-size=10MB` 설정 추가 |
| W-2 | sandbox iframe 외부 네트워크 요청 차단 CSP 헤더 추가 | 섹션 6.2 HTML 서빙 래퍼에 CSP 헤더(`default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:`) 명세 추가. 섹션 11.2 보안 테이블에 `외부 데이터 유출` 대응책 행 추가 |
| W-3 | 만료 체크 시 중복 DB UPDATE 제거 | 섹션 6.2 서빙 플로우와 섹션 10.3 `getDocumentForView` 로직을 읽기 전용으로 변경. status=EXPIRED 업데이트는 스케줄러에만 위임. 만료 판단은 항상 `expiresAt` 값 기준 |
| W-4 | storageUsage.documentCount 불일치 대응 | 섹션 3.2 StorageUsage 엔티티에 documentCount 정확성 주의사항 추가. 대시보드 표시는 실시간 COUNT 쿼리 사용, 스케줄러 만료 처리 시 document_count 감소 명시 |
| W-5 | Java용 nanoid 구현 방식 명시 | 섹션 3.3에 SecureRandom + 커스텀 알파벳 직접 구현 방침 명시 (외부 라이브러리 의존 없음). 섹션 10.1 SlugGenerator 설명 업데이트. 섹션 14.1 Gradle 의존성 목록에서 nanoid 제거 |

### 서비스명 변경 (3건)

| 변경 전 | 변경 후 | 위치 |
|---------|---------|------|
| `docshare-files` | `drafturl-files` | 섹션 8.1 R2 버킷명, 섹션 10.2 application.yml |
| `com.docshare.api` | `com.drafturl.api` | 섹션 10.1 패키지 경로 |
| `DocshareApiApplication` | `DrafturlApiApplication` | 섹션 10.1 메인 클래스명 |

---

## 수정 후 검토 결과

> 검토일: 2026-03-21
> 검토 대상: 수정 이력 섹션에 기재된 변경사항 (Critical 2건 + Warning 5건 + 서비스명 전체 변경)
> 검토 방법: 수정 이력 표의 반영 위치를 기준으로 본문 실제 반영 여부를 교차 확인

### 반영 확인 결과 요약

| 항목 | 반영 여부 | 비고 |
|------|----------|------|
| C-1: OAuth2 state 검증 | 완전 반영 | |
| C-2: Refresh Token Rotation | 완전 반영 | |
| W-1: 대용량 JSON body 트레이드오프 | 완전 반영 | |
| W-2: CSP 헤더 명세 | 완전 반영 (단, 경미한 불일치 존재) | 하단 상세 참조 |
| W-3: 만료 처리 읽기 전용 변경 | 완전 반영 | |
| W-4: documentCount 정확성 주의사항 | 완전 반영 | |
| W-5: nanoid Java 구현 방침 | 완전 반영 | |
| 서비스명 DocShare/docshare -> DraftURL/drafturl | 완전 반영 | 본문에 잔여 없음 |

---

### C-1: OAuth2 State 검증 반영 확인

반영 확인 완료. 아래 4개 위치 모두 정확히 반영되었다.

- 섹션 4.4 콜백 API 요청 body: `state` 필드(String, 필수) 추가, 필드 설명 포함
- 섹션 4.4 콜백 API: `state` 검증 실패 시 `INVALID_OAUTH_STATE` 에러 응답 예시 추가
- 섹션 4.5 에러 코드 총괄: `400 INVALID_OAUTH_STATE` 행 추가
- 섹션 5.1.1: state 생명주기 7단계 및 시퀀스 다이어그램 신설
- 섹션 9.4 OAuth2 콜백 페이지: state 검증 단계(2~3번) 및 쿠키 삭제 단계(6번) 명시
- 섹션 11.3: `CSRF (OAuth2)` 대응책 행 추가

생명주기의 단계 4에서 "Next.js에서 비교 검증"하고 단계 6에서 "Spring Boot가 state 존재 여부를 확인(null/빈값 거부)"하는 역할 분담이 명확히 기술되어 있다. Spring Boot가 state의 쿠키 일치 여부를 재검증하지 않고 존재 여부만 확인하는 설계 결정도 다이어그램에 명시적으로 드러나 있어 의도가 분명하다.

---

### C-2: Refresh Token Rotation 반영 확인

반영 확인 완료. 아래 위치 모두 정확히 반영되었다.

- 섹션 5.3 DDL(`CREATE TABLE refresh_tokens`): `is_revoked BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 포함
- 섹션 5.3.1: 정상 Rotation 6단계 및 탈취 감지 4단계 명세 신설. `@Transactional` 원자 처리 명시
- 섹션 4.4 토큰 갱신 API: 성공 응답에 `refreshToken` 필드 추가, 에러 응답 표 추가
- 섹션 4.5 에러 코드 총괄: `TOKEN_EXPIRED`, `TOKEN_REUSE_DETECTED` 행 추가
- 섹션 7.1 ERD: `is_revoked BOOLEAN` 컬럼 표시
- 섹션 7.2 DDL(`V1__init.sql`): `is_revoked BOOLEAN NOT NULL DEFAULT FALSE` 포함
- 섹션 11.3: rotation 설명이 "사용 시 교체(rotation)"에서 구체적인 탈취 감지 동작으로 업데이트

수정 이력 표(C-2)에는 "섹션 4.4 토큰 갱신 API 응답에 refreshToken 필드 추가"가 명시되어 있으며, 실제로 섹션 4.4의 성공 응답 예시(line 629-637)에 `refreshToken` 필드가 포함되어 있다.

---

### W-1: 대용량 JSON body 트레이드오프 반영 확인

반영 확인 완료.

- 섹션 4.3 `GET /api/v1/documents/{slug}` 본문에 트레이드오프 주석 블록이 추가되었다 (line 447).
- MVP에서는 현재 설계 유지, Phase 2에서 콘텐츠 분리 API 검토 방침을 명시하였다.
- `spring.servlet.multipart.max-request-size=10MB` 설정 언급이 포함되어 있다.

---

### W-2: CSP 헤더 명세 반영 확인

반영 확인 완료. 단, 경미한 불일치가 존재한다.

반영된 내용:
- 섹션 6.2 HTML 서빙 래퍼에 CSP 헤더 블록 신설 (line 973-983)
- 섹션 11.2 보안 테이블에 `외부 데이터 유출` 대응책 행 추가 (line 1650)

**불일치 사항**: 기존 검토 결과(W-2)에서 권고한 CSP 헤더와 실제 반영된 CSP 헤더가 `img-src` 지시어에서 다르다.

| 위치 | CSP 헤더 값 |
|------|-------------|
| 기존 검토(W-2) 권고안 | `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'` |
| 실제 반영(섹션 6.2) | `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:` |
| 섹션 11.2 보안 테이블 | `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:` |

권고안보다 `img-src data:`가 추가되어 data URI 이미지를 허용하는 방향으로 확장된 것은 LLM 생성 HTML에서 base64 인라인 이미지가 흔하다는 점을 감안하면 합리적인 결정이다. 섹션 6.2와 섹션 11.2의 CSP 값이 일치하므로 내부 일관성은 유지되고 있다. 의도적 변경이라면 수정 이력 표에 "img-src data: 추가 이유" 한 줄을 덧붙이면 완결성이 높아진다.

---

### W-3: 만료 처리 읽기 전용 변경 반영 확인

반영 확인 완료.

- 섹션 6.2 서빙 플로우 시퀀스 다이어그램(line 925-929): 만료 시간 확인 시 "DB UPDATE 없음, status 변경은 스케줄러에 위임" 명시
- 섹션 10.3 `getDocumentForView` 로직(line 1605-1607): "DB UPDATE 없음 (읽기 전용). status=EXPIRED 변경은 스케줄러에 위임. 만료 판단은 항상 expiresAt 값 기준으로 수행" 명시

---

### W-4: documentCount 정확성 주의사항 반영 확인

반영 확인 완료.

- 섹션 3.2 StorageUsage 엔티티 아래에 상세한 주의사항 블록 추가 (line 217): 대시보드의 `documentCount` 표시를 실시간 COUNT 쿼리로 조회한다는 방침, `storage_usage.document_count`는 근사값 용도에 한정한다는 결정, 스케줄러 만료 처리 시 document_count 감소 명시를 모두 포함하고 있다.

---

### W-5: nanoid Java 구현 방침 반영 확인

반영 확인 완료.

- 섹션 3.3 값 객체: `SecureRandom + 커스텀 알파벳으로 직접 구현 (외부 라이브러리 의존 없음)` 및 `com.aventrix.jnanoid:jnanoid 대신 직접 구현을 선택`하는 이유 명시 (line 234-237)
- 섹션 10.1 `SlugGenerator.java` 설명: `(SecureRandom 직접 구현)` 명시 (line 1486)
- 섹션 14.1 구현 순서 Step 2: `nanoid는 직접 구현` 명시 (line 1790)

---

### 서비스명 변경 반영 확인

반영 확인 완료. 본문 전체에서 `DocShare`, `docshare` 잔여 사용은 없다.

- 섹션 8.1 R2 버킷명: `drafturl-files` (line 1223)
- 섹션 10.1 패키지 경로: `com.drafturl.api` (line 1465)
- 섹션 10.1 메인 클래스명: `DrafturlApiApplication` (line 1466)
- 섹션 10.2 application.yml `r2.bucket-name`: `drafturl-files` (line 1258)

수정 이력 표에 기재된 3개소 이외에 `DocShare`/`docshare`가 등장하는 위치는 수정 이력 표 자체(이전 값 표기용)뿐이며, 이는 변경 전 값을 기록한 것이므로 잔여 오류가 아니다.

---

### 신규 발견 사항

수정 반영 자체는 모두 올바르게 이루어졌다. 검토 과정에서 추가로 발견된 사항을 아래에 기록한다.

#### Minor-1: 섹션 15 "기존 검토 결과(Critical 3건) 반영 확인" 표와 실제 검토 결과의 ID 체계 불일치

**위치**: 섹션 15 (line 1881-1888)

**설명**: 섹션 15의 표는 이전 최초 검토 결과(C-1: 고아 R2 파일, C-2: sandbox allow-same-origin, C-3: CRON 인증)를 기준으로 작성되어 있다. 그런데 "검토 결과" 섹션의 Critical 항목(C-1: OAuth2 state, C-2: Refresh Token Rotation)은 이후 검토에서 새로 부여된 ID이므로 번호가 겹친다. 문서를 처음 읽는 사람이 두 C-1, 두 C-2를 혼동할 수 있다.

**개선 제안**: 섹션 15의 표 제목을 "이전 1차 검토 결과(Critical 3건) 반영 확인"으로 변경하거나, 검토 결과 섹션의 Critical ID를 C-4, C-5로 순번을 이어서 부여하는 방식을 검토한다.

#### Minor-2: W-2 권고 CSP와 반영 CSP의 차이를 수정 이력에 미기재

**위치**: 수정 이력 섹션 W-2 행 (line 2227)

**설명**: W-2 항목에서 확인한 대로, 기존 검토 권고안에는 없던 `img-src data:`가 추가되었으나 수정 이력 표에는 이 추가 사항이 설명되어 있지 않다. 의도적인 확장임을 수정 이력에 한 줄로 기록하면 문서의 추적 가능성이 높아진다.

#### Minor-3: 섹션 6.5 스케줄러에서 만료 처리 시 document_count 감소 로직이 의사코드에 미반영

**위치**: 섹션 6.5 (line 1029-1052)

**설명**: W-4 반영으로 섹션 3.2의 주의사항 블록에 "스케줄러의 만료 처리 시에도 로그인 사용자의 문서에 대해 document_count 감소를 명시적으로 처리한다"고 명시되었다. 그러나 섹션 6.5의 스케줄러 의사코드(`2. 만료 문서 처리` 블록)에는 `document_count` 감소 처리가 표시되어 있지 않다. 두 위치가 일치해야 한다.

**개선 제안**: 섹션 6.5의 `2. 만료 문서 처리` 블록에 `-> 로그인 사용자 문서인 경우: storage_usage.document_count 감소` 한 줄을 추가한다.

---

### 전체 평가

이번 수정은 Critical 2건과 Warning 5건 모두 올바르게 반영되었다. 특히 다음 사항이 잘 처리되었다.

- C-1의 시퀀스 다이어그램이 Next.js/Spring Boot 역할 분담(Next.js는 쿠키 일치 검증, Spring Boot는 존재 여부 확인)을 명확히 표현하고 있다.
- C-2의 탈취 감지 시나리오가 별도 블록으로 분리되어 정상 Rotation과 비정상 케이스가 구분된다.
- W-3의 읽기 전용 변경이 시퀀스 다이어그램과 서비스 로직 양쪽에 일관되게 반영되었다.
- 서비스명 변경은 본문 전체에서 누락 없이 완료되었다.

신규 발견 사항 3건은 모두 Minor 수준이며, 문서 기능성에 영향을 주지 않는다. 우선순위상 Minor-3(섹션 6.5 의사코드 불일치)은 구현자가 오해할 여지가 있으므로 가장 먼저 처리하는 것을 권장한다.

| 심각도 | 건수 |
|--------|------|
| Critical | 0건 |
| Major | 0건 |
| Minor | 3건 |
