# DraftURL — 보안 설계

> 최종 수정일: 2026-03-28
> v0.3.0 보안 점검 결과 반영

---

## 보안 감사 요약

### 안전한 항목 (수정 불필요)

| 항목 | 구현 방식 |
|------|-----------|
| JWT 토큰 | HS256, 액세스 1시간/리프레시 7일 만료, jjwt 0.12.6 |
| OAuth2 CSRF | HMAC-SHA256 state 파라미터 + 타이밍 공격 방어 (constant-time 비교) |
| HTML 새니타이징 | Jsoup 1.18.3, 백엔드에서 저장 전 정화 |
| 패스워드 | BCrypt 해싱, 평문 저장 없음 |
| Rate Limiting | 토큰 버킷 알고리즘, 익명 10req/min, 인증 30req/min |
| 입력 검증 | 모든 DTO에 @Valid, @NotBlank, @Size, @Email, 커스텀 @ContentSize |
| SQL Injection | JPA 파라미터 바인딩만 사용, 문자열 결합 없음 |
| 쿠키 보안 | httpOnly, secure(프로덕션), SameSite=Lax |
| 에러 핸들링 | 스택 트레이스 미노출, Sentry로 서버 사이드 로깅 |
| 의존성 | Spring Boot 3.4.5, 알려진 취약점 없음 |

---

## 수정된 보안 항목

### 1. iframe sandbox 강화 (CRITICAL)

**파일:** `HtmlViewer.tsx`, `PreviewPanel.tsx`

**문제:** `allow-same-origin` + `allow-scripts`가 함께 있으면 iframe 안의 스크립트가 부모 페이지와 동일한 출처(origin)로 인식된다. 악성 HTML 문서가 업로드되면 부모 페이지의 쿠키, localStorage, DOM에 접근할 수 있었다.

**수정:** `allow-same-origin`, `allow-forms`, `allow-popups-to-escape-sandbox` 제거.

**이점:** iframe 내 스크립트가 완전히 격리되어, 사용자가 올린 HTML에 악성 코드가 있어도 DraftURL 본체에 영향을 줄 수 없다.

---

### 2. 보안 응답 헤더 추가 (MEDIUM)

**파일:** `SecurityConfig.java`

**문제:** 브라우저 내장 보안 기능들이 활성화되지 않은 상태였다.

**수정 및 이점:**

| 헤더 | 역할 |
|------|------|
| `X-Content-Type-Options: nosniff` | 브라우저가 응답의 Content-Type을 추측하지 않아, JSON을 HTML로 해석하는 등의 MIME 스니핑 공격 차단 |
| `X-Frame-Options: SAMEORIGIN` | 외부 사이트에서 DraftURL을 iframe으로 삽입하는 클릭재킹 공격 차단 |
| `Strict-Transport-Security` | 브라우저가 항상 HTTPS로 접속하게 강제하여, HTTP 다운그레이드 공격(중간자 공격) 방지 |

---

### 3. API 프록시 경로 검증 (MEDIUM)

**파일:** `frontend/src/app/api/proxy/[...path]/route.ts`

**문제:** 프론트엔드 프록시가 어떤 경로든 백엔드로 전달할 수 있었다. 공격자가 `/api/proxy/internal-admin/...` 같은 경로를 호출하면 내부 API에 접근할 수 있는 SSRF(서버 사이드 요청 위조) 위험이 있었다.

**수정:** 허용 경로 화이트리스트(`/documents`, `/auth`, `/health`) + 요청 본문 5MB 크기 제한.

**이점:** 의도치 않은 내부 엔드포인트 노출이 불가능하다. 대용량 요청을 통한 서비스 장애도 방지한다.

---

### 4. Markdown 새니타이징 복원 (MEDIUM)

**파일:** `frontend/src/lib/markdown.ts`

**문제:** Markdown에서 HTML 태그를 직접 사용할 수 있는데(`<img onerror="alert(1)">`), 새니타이저가 꺼져 있어서 이런 태그가 그대로 렌더링되었다. iframe sandbox에만 의존하는 단일 방어선이었다.

**수정:** `rehype-sanitize` 플러그인 재활성화. 위험한 태그/속성 제거 + 안전한 태그만 허용.

**이점:** `rehype-sanitize`가 위험한 태그와 속성을 제거하고, iframe sandbox가 2차 방어선 역할을 한다. 하나가 뚫려도 다른 하나가 막아주는 이중 방어(defense-in-depth) 체계.

---

### 5. 인증 미들웨어 (기존 구현 확인)

**파일:** `frontend/src/proxy.ts`

**상태:** `/dashboard` 경로 접근 시 `access_token` 쿠키가 없으면 로그인 페이지로 리다이렉트하는 로직이 이미 구현되어 있음.

**이점:** 미인증 사용자가 대시보드 URL을 직접 입력해도 백엔드 API 호출 전에 차단되어, 불필요한 서버 부하와 에러 노출을 방지한다.

---

## 보안 아키텍처 다이어그램

```
[사용자 입력]
    │
    ▼
[프론트엔드 입력 검증] ← 클라이언트 레벨
    │
    ▼
[proxy.ts 인증 체크] ← 미인증 차단
    │
    ▼
[API 프록시 경로 화이트리스트] ← SSRF 방지
    │
    ▼
[Rate Limiting] ← 과도한 요청 차단
    │
    ▼
[백엔드 @Valid 입력 검증] ← 서버 레벨
    │
    ▼
[Jsoup HTML 새니타이징] ← 저장 전 정화
    │
    ▼
[DB 저장 (JPA 파라미터 바인딩)] ← SQL Injection 방지
    │
    ▼
[렌더링 시 rehype-sanitize] ← Markdown 이중 방어
    │
    ▼
[iframe sandbox 격리] ← XSS 최종 방어선
```

---

## CSRF 방어 전략

현재 CSRF 보호는 Spring Security 레벨에서 비활성화(`csrf.disable()`)되어 있다. 이는 다음 이유로 합리적이다:

1. **Stateless JWT 인증** — 세션 쿠키가 아닌 JWT 토큰 기반
2. **SameSite=Lax 쿠키** — 크로스 사이트 POST 요청에서 쿠키가 전송되지 않음
3. **OAuth2 state 파라미터** — OAuth 흐름에 대한 별도 CSRF 방어
4. **CORS 설정** — 허용된 오리진에서만 API 접근 가능

---

## 향후 개선 사항

| 항목 | 우선순위 | 설명 |
|------|----------|------|
| Content-Security-Policy | MEDIUM | CSP 헤더 추가로 인라인 스크립트 실행 제한 |
| CDN SRI | LOW | 외부 CDN 리소스에 Subresource Integrity 적용 |
