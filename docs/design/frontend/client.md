# DraftURL -- API 클라이언트 & 상태관리

> 상태: active
> 작성일: 2026-03-21

---

## 요약

- fetch 래퍼 기반 API 클라이언트로 baseURL, 인증 헤더, 공통 응답 파싱, 자동 토큰 갱신을 처리
- 401 자동 갱신, 410 만료/삭제 분기, 429 Rate Limit 등 HTTP 상태별 에러 처리 전략 정의
- JWT 토큰은 httpOnly 쿠키에 저장하며, 서버/클라이언트 컴포넌트 모두에서 안전하게 전달
- Zustand store로 인증 상태(useAuthStore)와 에디터 상태(useEditorStore)를 관리

---

## 1. API 클라이언트 레이어

```
lib/
  api/
    client.ts              -- fetch 래퍼 (baseURL, 헤더, 에러 처리)
    auth.ts                -- 인증 API 호출 함수
    documents.ts           -- 문서 API 호출 함수
    types.ts               -- API 응답 타입 정의
```

**client.ts 설계 원칙**:

1. `baseURL`을 환경변수 `NEXT_PUBLIC_API_URL`에서 읽는다.
2. 모든 요청에 `Content-Type: application/json` 헤더를 기본 포함한다.
3. 인증이 필요한 요청에 자동으로 `Authorization: Bearer {token}` 헤더를 추가한다.
4. 401 응답 시 자동으로 리프레시 토큰으로 갱신을 시도하고, 갱신 실패 시 로그인 페이지로 리다이렉트한다.
5. 서버 컴포넌트에서 호출할 때는 쿠키에서 토큰을 읽어 전달한다.
6. 공통 응답 포맷(`{success, data/error}`)을 파싱하여 에러 시 구조화된 에러를 throw한다.

---

## 2. 에러 처리 전략

| 상황 | 처리 |
|------|------|
| 네트워크 에러 | 토스트 알림으로 재시도 유도 |
| 401 Unauthorized | 자동 토큰 갱신 -> 실패 시 로그인 페이지 리다이렉트 |
| 410 `DOCUMENT_EXPIRED` vs `DOCUMENT_GONE` | 만료 안내 UI vs 삭제 안내 UI 분기 렌더링 |
| 429 Rate Limit | Rate limit 초과 안내 + `Retry-After` 기반 재시도 |
| 500 서버 에러 | 일반 에러 UI 표시 |

에러 코드 전체 목록은 [백엔드 API 명세](../backend/api.md)의 에러 코드 총괄 섹션을 참조한다.

---

## 3. 토큰 관리

- **Access Token**: `httpOnly` 쿠키에 저장 (XSS 방지). `SameSite=Lax`, `Secure=true`. JavaScript에서 직접 읽을 수 없으므로, Next.js의 서버 컴포넌트/Route Handler에서 쿠키를 읽어 Spring Boot API 호출 시 `Authorization: Bearer` 헤더로 전달한다.
- **클라이언트 컴포넌트에서의 API 호출**: Next.js의 `/api/` Route Handler를 프록시로 사용하여, 브라우저 -> Next.js 서버(쿠키 자동 전송) -> Spring Boot 경로로 호출한다. 또는 같은 도메인이면 쿠키가 자동 전송되므로 Spring Boot에서 쿠키에서 직접 JWT를 추출한다.
- **Refresh Token**: `httpOnly` 쿠키에 저장. Path는 `/`로 설정하여 Next.js 서버에서 접근 가능하게 한다. (기존 `/api/v1/auth/refresh` 경로 제한은 Next.js에서 접근 불가하므로 변경)
- Access Token 만료 시 자동으로 Refresh Token으로 갱신. Refresh Token도 만료되면 로그인 페이지로 리다이렉트.

인증/인가의 백엔드 설계는 [백엔드 인증 설계](../backend/auth.md)를 참조한다.

---

## 4. Zustand Store 구조

```
lib/
  store/
    useAuthStore.ts              -- { user, isAuthenticated, login(), logout() }
    useEditorStore.ts            -- { content, docType, title, setContent(), setDocType() }
```

- `useAuthStore`: 사용자 인증 상태 관리. 로그인/로그아웃 시 상태 갱신.
- `useEditorStore`: 에디터 콘텐츠, 문서 타입, 제목 등 에디터 상태 관리. 메인 페이지와 편집 페이지에서 공용.

---

## 관련 문서

- [프론트엔드 설계 개요](README.md) -- 기술 스택, 디렉토리 구조
- [페이지/라우트 구조](pages.md) -- 각 페이지에서의 API 호출 흐름
- [백엔드 API 명세](../backend/api.md) -- 엔드포인트 상세, 에러 코드
- [백엔드 인증 설계](../backend/auth.md) -- OAuth2 플로우, JWT, Refresh Token Rotation
