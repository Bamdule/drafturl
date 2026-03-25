# DraftURL -- 페이지/라우트 구조

> 상태: active
> 작성일: 2026-03-21

---

## 요약

- App Router 기반 라우트 구조로 메인(에디터), 문서 서빙, 대시보드, OAuth2 콜백, 로그인 등 페이지를 정의
- 메인 페이지는 랜딩 + Monaco Editor + 실시간 미리보기를 결합하며, 문서 서빙은 sandbox iframe/unified로 렌더링
- 대시보드는 middleware.ts에서 JWT 쿠키 존재 여부로 인증 가드를 적용
- API Routes는 없으며 모든 백엔드 로직은 Spring Boot에서 처리

---

## 1. 페이지/라우트 구조

```
app/
+-- layout.tsx                    -- 루트 레이아웃 (글로벌 스타일, 폰트, 메타데이터)
+-- page.tsx                      -- 랜딩 + 에디터 페이지 (메인)
+-- [slug]/
|   +-- page.tsx                  -- 문서 서빙 페이지 (공유 URL)
+-- dashboard/
|   +-- layout.tsx                -- 대시보드 레이아웃 (인증 가드)
|   +-- page.tsx                  -- 내 문서 목록
|   +-- [slug]/
|       +-- edit/
|           +-- page.tsx          -- 문서 편집 페이지
+-- auth/
|   +-- login/
|   |   +-- page.tsx              -- 로그인 페이지
|   +-- callback/
|   |   +-- page.tsx              -- OAuth2 콜백 처리 (인가코드 -> Spring Boot)
|   +-- error/
|       +-- page.tsx              -- 인증 에러 페이지
+-- expired/
|   +-- page.tsx                  -- 만료 문서 안내 페이지 (?slug= 쿼리 지원)
+-- not-found.tsx                 -- 404 페이지
+-- error.tsx                     -- 글로벌 에러 페이지
```

API Routes는 없다. 모든 백엔드 로직은 Spring Boot에서 처리한다.

---

## 2. 주요 페이지 상세

### 메인 페이지 (`app/page.tsx`)

역할: 랜딩 페이지 + 문서 생성 에디터. 서비스의 핵심 진입점.

```
+-----------------------------------------------+
|  헤더: 로고 | [로그인] [내 문서]                  |
+-----------------------------------------------+
|                                               |
|  +-----------------+  +--------------------+  |
|  |                 |  |                    |  |
|  |   코드 에디터     |  |   실시간 미리보기    |  |
|  |   (Monaco)      |  |   (iframe/렌더링)   |  |
|  |                 |  |                    |  |
|  |                 |  |                    |  |
|  +-----------------+  +--------------------+  |
|                                               |
|  +-----------------------------------------+  |
|  |  [HTML v] [MD]  |  파일 드래그 앤 드롭    |  |
|  +-----------------------------------------+  |
|                                               |
|           [ 공유하기 ]                          |
|                                               |
|  비로그인: "24시간 후 만료됩니다. 로그인하면     |
|            영구 보존!"                          |
|  "비로그인 문서는 대시보드에서 관리되지 않습니다" |
+-----------------------------------------------+
```

미리보기:
- HTML: 클라이언트 iframe `srcdoc`로 즉시 렌더링 (Spring Boot 호출 불필요)
- MD: 클라이언트에서 `unified (remark + rehype)`로 변환 후 렌더링

"공유하기" 클릭 시:
1. Spring Boot `POST /api/v1/documents` 호출
2. 응답의 `url`을 결과 모달에 표시

### 문서 서빙 페이지 (`app/[slug]/page.tsx`)

역할: 공유 URL로 접근 시 문서를 렌더링한다.

서버 컴포넌트에서 Spring Boot `GET /api/v1/documents/{slug}/view` 호출:
- 200: 문서 렌더링 (HTML이면 sandbox iframe, MD이면 unified 변환)
- 404: `notFound()` 호출
- 410: 만료 안내 UI 인라인 렌더링 (리다이렉트 대신 같은 페이지에서 처리하여 slug 정보 유지)

### OAuth2 콜백 페이지 (`app/auth/callback/page.tsx`)

역할: OAuth2 인가 코드를 받아 state를 검증한 후 Spring Boot에 전달하고 JWT를 수신한다. 인증 플로우 상세는 [백엔드 인증 설계](../backend/auth.md)를 참조한다.

1. URL 쿼리 파라미터에서 `code`, `state` 추출
2. **`oauth_state` 쿠키에서 저장된 state 값을 읽어 URL의 state와 비교 검증**
3. **불일치 시 `/auth/error`로 리다이렉트 (CSRF 의심)**
4. Spring Boot `POST /api/v1/auth/oauth2/callback/{provider}` 호출 (code, redirectUri, state 전달)
5. 응답의 `accessToken`, `refreshToken`을 쿠키에 저장
6. `oauth_state` 쿠키 삭제 (재사용 방지)
7. 대시보드 또는 이전 페이지로 리다이렉트

### 대시보드 (`app/dashboard/page.tsx`)

인증 가드: `middleware.ts`에서 `/dashboard/*` 경로에 대해 JWT 쿠키 존재 여부를 확인하여 미인증 시 `/auth/login`으로 리다이렉트한다. 레이아웃에서 중복 확인하지 않는다.

```
+-----------------------------------------------+
|  헤더: 로고 | 사용자 프로필 드롭다운              |
+-----------------------------------------------+
|                                               |
|  내 문서 (N개)                   [+ 새 문서]    |
|                                               |
|  +-----------------------------------------+  |
|  | 제목       | 유형 | 크기 | 수정일         |  |
|  +-----------+------+------+---------------+  |
|  | 주간 보고서 | HTML | 12KB | 3시간전        |  |
|  |           |      |      | [링크] [편집]  |  |
|  |           |      |      | [삭제]         |  |
|  +-----------+------+------+---------------+  |
|  | API 문서   | MD   | 8KB  | 1일전          |  |
|  |           |      |      | [링크] [편집]  |  |
|  |           |      |      | [삭제]         |  |
|  +-----------------------------------------+  |
|                                               |
|  사용량: 3개 문서 | 20KB / 5MB                   |
|  < 1 2 3 ... >                                 |
+-----------------------------------------------+
```

[링크] 버튼으로 공유 URL 복사 기능 제공.

사용량 표시: `GET /api/v1/auth/me` 응답의 `storageUsage` 필드를 사용한다.

### 문서 편집 페이지 (`app/dashboard/[slug]/edit/page.tsx`)

역할: 기존 문서를 수정한다. 메인 페이지의 에디터 컴포넌트를 재사용한다.

1. `GET /api/v1/documents/{slug}`로 기존 내용 로드 -> 에디터에 표시
2. 수정 후 `PUT /api/v1/documents/{slug}` 호출
3. 수정 성공 시 토스트 알림으로 결과 표시

### 로그인 페이지 (`app/auth/login/page.tsx`)

역할: OAuth2 소셜 로그인 진입점.

1. Google/GitHub OAuth2 버튼 클릭
2. `state` 값 생성 (CSRF 방지용) + `oauth_state` 쿠키에 저장
3. Provider의 OAuth2 인가 URL로 리다이렉트 (client_id, redirect_uri, state, scope 포함)

---

## 관련 문서

- [프론트엔드 설계 개요](README.md) -- 기술 스택, 디렉토리 구조
- [API 클라이언트 설계](client.md) -- API 호출, 에러 처리
- [백엔드 API 명세](../backend/api.md) -- 엔드포인트 상세
- [백엔드 인증 설계](../backend/auth.md) -- OAuth2 플로우
