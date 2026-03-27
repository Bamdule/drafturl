# DraftURL -- 프론트엔드 설계 (Next.js)

> 상태: active
> 작성일: 2026-03-21

---

## 요약

- Next.js 15(App Router) + TypeScript + Tailwind CSS/shadcn/ui 기반이며, UI 렌더링과 Spring Boot API 호출만 담당
- Server Components에서 직접 DB에 접근하지 않으며, 메타데이터는 Spring Boot API 경유, 문서 콘텐츠는 CDN(files.drafturl.com)에서 직접 로드
- Zustand로 상태 관리하고, unified(remark + rehype)로 Markdown 클라이언트 렌더링 처리
- Monaco Editor를 코드 편집에 사용하며, 컴포넌트 기반 디렉토리 구조로 관심사를 분리

## 하위 문서

| 문서 | 내용 |
|------|------|
| [pages.md](pages.md) | 페이지/라우트 구조, 주요 페이지 상세 (메인, 서빙, 콜백, 대시보드, 편집, 로그인) |
| [client.md](client.md) | API 클라이언트 레이어, 에러 처리 전략, 토큰 관리, Zustand 상태관리 |

---

## 1. 기술 스택

| 구성요소 | 선택 | 이유 |
|---------|------|------|
| 프레임워크 | **Next.js 15 (App Router)** | SSR + 정적 페이지, SEO |
| 언어 | **TypeScript** | 타입 안전성 |
| 스타일링 | **Tailwind CSS + shadcn/ui** | 빠른 UI 개발, 일관된 디자인 시스템 |
| 에디터 | **Monaco Editor** | HTML/MD 코드 편집 |
| 상태 관리 | **Zustand** | 경량, 단순 (store 구조는 [client.md](client.md) 참조) |
| MD 렌더링 | **unified (remark + rehype)** | 클라이언트 미리보기용 MD->HTML 변환 |
| HTTP 클라이언트 | **ky** 또는 **fetch wrapper** | Spring Boot API 호출 |

---

## 2. 역할 정의

Next.js는 UI 렌더링과 Spring Boot API 호출을 담당한다. **Server Components에서 직접 DB에 접근하지 않는다.** 문서 콘텐츠는 백엔드가 반환한 CDN URL(`contentUrl`)을 통해 `files.drafturl.com`에서 직접 로드한다.

| 역할 | O/X | 설명 |
|------|-----|------|
| UI 렌더링 | O | React Server/Client Components |
| 정적 페이지 | O | 랜딩, 만료 안내 등 |
| Spring Boot API 호출 | O | 서버/클라이언트 모두에서 |
| 직접 DB 접근 | X | 모든 데이터는 Spring Boot API 경유 |
| CDN 파일 로드 | O | 백엔드가 반환한 `contentUrl`(CDN URL)을 통해 `files.drafturl.com`에서 파일 직접 로드 |
| OAuth2 리다이렉트 처리 | O | 인가 코드를 받아 Spring Boot에 전달 |
| JWT 토큰 관리 | O | 쿠키 저장, 자동 갱신 |

---

## 3. 디렉토리 구조

```
src/
+-- app/                          -- Next.js App Router 페이지/라우트
|   +-- (pages.md 참조)
+-- components/
|   +-- ui/                       -- shadcn/ui 컴포넌트
|   +-- editor/
|   |   +-- EditorPanel.tsx       -- Monaco Editor 래퍼
|   |   +-- PreviewPanel.tsx      -- 미리보기 패널
|   |   +-- FileDropZone.tsx      -- 파일 드래그 앤 드롭
|   +-- document/
|   |   +-- DocumentList.tsx      -- 문서 목록
|   |   +-- DocumentCard.tsx      -- 문서 카드 (URL 복사, 편집, 삭제)
|   |   +-- DeleteConfirmDialog.tsx
|   +-- viewer/
|   |   +-- HtmlViewer.tsx        -- HTML sandbox iframe
|   |   +-- MarkdownViewer.tsx    -- MD 렌더링 뷰어
|   +-- layout/
|   |   +-- Header.tsx            -- 공통 헤더
|   |   +-- DashboardLayout.tsx   -- 대시보드 레이아웃
|   +-- common/
|       +-- PublishButton.tsx
|       +-- PublishResultModal.tsx
|       +-- StorageUsageBar.tsx
+-- lib/
|   +-- api/
|   |   +-- client.ts             -- API 클라이언트 (fetch 래퍼)
|   |   +-- auth.ts               -- 인증 API
|   |   +-- documents.ts          -- 문서 API
|   |   +-- types.ts              -- API 타입 정의
|   +-- auth/
|   |   +-- token.ts              -- JWT 토큰 관리 (쿠키 읽기/쓰기/갱신)
|   +-- markdown.ts               -- unified MD->HTML 변환 (미리보기용)
|   +-- constants.ts              -- 상수 (제한값 등)
+-- middleware.ts                  -- 인증 가드 (대시보드 보호), 토큰 갱신
```

---

## 4. 구현 주의사항

| 항목 | 주의사항 |
|------|----------|
| Monaco Editor | Next.js에서 `next/dynamic`으로 SSR: false 동적 import 필수 |
| 미리보기 debounce | 300ms debounce 적용. 클라이언트에서 처리하므로 API 호출 불필요 |
| 파일 읽기 인코딩 | 프론트엔드에서 `FileReader.readAsText(file, 'utf-8')`로 통일 |

---

## 관련 문서

- [백엔드 설계](../backend/README.md) -- API 명세, 인증 플로우
- [인프라 & 배포](../infrastructure/README.md) -- Vercel 배포 설정
- UI 목업 -- HTML 목업 페이지:
  - [메인 (에디터)](../../mockups/index.html)
  - [문서 뷰어](../../mockups/viewer.html)
  - [공유 페이지](../../mockups/shared.html)
  - [대시보드](../../mockups/dashboard.html)
  - [로그인](../../mockups/login.html)
