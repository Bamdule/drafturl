# DraftURL -- 페이지/라우트 구조

> 상태: active
> 최종 수정일: 2026-04-01

---

## 요약

- App Router 기반 라우트 구조. 다국어(i18n) 지원: `/en`, `/ko` 정적 라우트
- 메인 페이지는 2컬럼 랜딩(히어로+스텝 | 드롭존+공유) 구조
- 문서 서빙은 sandbox iframe으로 렌더링
- API Routes: Next.js Route Handlers를 BFF 프록시로 사용 (인증 쿠키 처리)

---

## 1. 페이지/라우트 구조

```
app/
+-- layout.tsx                    -- 루트 레이아웃 (글로벌 스타일, 폰트, 메타데이터)
+-- en/page.tsx                   -- 영문 랜딩 (SEO 메타데이터 + JSON-LD)
+-- ko/page.tsx                   -- 한글 랜딩 (SEO 메타데이터 + JSON-LD)
+-- [slug]/
|   +-- page.tsx                  -- 문서 서빙 페이지 (공유 URL)
|   +-- DocumentViewPage.tsx      -- 문서 뷰어 (클라이언트 컴포넌트)
+-- dashboard/
|   +-- layout.tsx                -- 대시보드 레이아웃 (Header 포함)
|   +-- page.tsx                  -- 내 문서 목록
|   +-- [slug]/edit/page.tsx      -- 문서 편집 페이지
+-- account/
|   +-- layout.tsx                -- 내 정보 레이아웃
|   +-- page.tsx                  -- 내 정보 (계정 정보 + 회원탈퇴)
+-- contact/
|   +-- layout.tsx                -- 문의 레이아웃
|   +-- page.tsx                  -- 1:1 문의 폼
+-- guide/
|   +-- page.tsx                  -- 가이드 (Features, Steps, FAQ, MCP 연결)
|   +-- mcp/page.tsx              -- MCP 연결 가이드
+-- auth/
|   +-- login/page.tsx            -- 로그인
|   +-- signup/page.tsx           -- 회원가입
|   +-- callback/page.tsx         -- OAuth2 콜백 처리
|   +-- error/page.tsx            -- 인증 에러
+-- terms/page.tsx                -- 이용약관
+-- privacy/page.tsx              -- 개인정보처리방침
+-- aup/page.tsx                  -- 이용제한정책
+-- expired/page.tsx              -- 만료 문서 안내
+-- not-found.tsx                 -- 404
+-- error.tsx                     -- 글로벌 에러
+-- api/
    +-- auth/                     -- 인증 Route Handlers (BFF 프록시)
    |   +-- login/route.ts
    |   +-- signup/route.ts
    |   +-- logout/route.ts
    |   +-- refresh/route.ts
    |   +-- me/route.ts
    |   +-- oauth-callback/route.ts
    |   +-- delete-account/route.ts
    +-- proxy/[...path]/route.ts  -- API 프록시 (httpOnly 쿠키 → Authorization 헤더)
```

---

## 2. 주요 페이지 상세

### 메인 페이지 (`components/home/HomePage.tsx`)

역할: 랜딩 페이지 + 문서 공유. 서비스의 핵심 진입점.

```
+-----------------------------------------------+
|  헤더: 로고 | [로그인]                           |
+-----------------------------------------------+
|                                               |
|  AI가 만든 문서,     |  ┌──────────────────┐  |
|  링크 하나로 공유     |  │                  │  |
|                      |  │    드래그앤드롭     │  |
|  ① AI에게 요청   →   |  │    또는 Ctrl+V    │  |
|  ② 붙여넣기     →   |  │                  │  |
|  ③ 공유 URL 생성     |  └──────────────────┘  |
|                      |                        |
|  비로그인 24시간 만료  |  [미리보기] [공유하기]  |
+-----------------------------------------------+
|  © 2026 DraftURL | 이용약관 | 개인정보 | 문의   |
+-----------------------------------------------+
```

- 2컬럼 그리드 (좌 5fr : 우 7fr), 모바일은 1컬럼
- 드롭존: 파일 드래그앤드롭, Ctrl+V 붙여넣기, 클릭으로 파일 선택
- 파일 등록 후: 드롭존 내부에 파일 정보 + 미리보기/공유 버튼 표시
- 미리보기: window.open으로 새 창에서 렌더링
- 공유하기: `POST /api/v1/documents` → PublishResultModal

### 문서 서빙 (`app/[slug]/page.tsx`)

서버 컴포넌트에서 `GET /api/v1/documents/{slug}/view` 호출:
- 200: DocumentViewPage (HTML: sandbox iframe, MD: marked 변환)
- 404: notFound()
- 410: 만료/삭제 안내 UI

하단에 콘텐츠 신고 버튼 (ReportDialog).

### 대시보드 (`app/dashboard/page.tsx`)

인증 가드: middleware.ts에서 JWT 쿠키 확인 → 미인증 시 로그인 리다이렉트.

- 문서 목록 (카드형, 페이지네이션)
- 저장 용량 / 문서 수 / 플랜 표시
- 문서 삭제 (DeleteConfirmDialog)

### 내 정보 (`app/account/page.tsx`)

- 계정 정보 (이름, 이메일, 플랜)
- 회원탈퇴 (Danger Zone → DeleteAccountDialog)

### 문의 (`app/contact/page.tsx`)

- 유형 선택 (일반 문의 / 버그 리포트)
- 이메일, 이름, 제목, 내용 폼
- 로그인 시 이메일/이름 자동 채움
- `POST /api/v1/inquiries` → 성공 메시지

### 가이드 (`app/guide/page.tsx`)

- 가이드 목록 (MCP 연결)
- 시작하기 (3 Steps)
- 주요 기능 (4 Features)
- FAQ (6개)

---

## 관련 문서

- [프론트엔드 설계 개요](README.md) -- 기술 스택, 디렉토리 구조
- [API 클라이언트 설계](client.md) -- API 호출, 에러 처리
- [백엔드 API 명세](../backend/api.md) -- 엔드포인트 상세
- [백엔드 인증 설계](../backend/auth.md) -- OAuth2 플로우
