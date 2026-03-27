# 프론트엔드 태스크

> 최종 수정일: 2026-03-23
> 설계 문서: [frontend/](../design/frontend/)

---

## 진행 상태

| # | 태스크 | 상태 | 의존성 |
|---|--------|------|--------|
| 1 | 프로젝트 기반 설정 (Next.js 스캐폴딩) | ✅ | - |
| 2 | 공통 인프라 구현 (API 클라이언트, 상수, 유틸) | ✅ | #1 |
| 3 | Zustand 상태관리 구현 | ✅ | #1 |
| 4 | 공통 UI 컴포넌트 (shadcn/ui) | ✅ | #1 |
| 5 | 공통 레이아웃 컴포넌트 (Header) | ✅ | #3, #4 |
| 6 | 에디터 컴포넌트 (Monaco + 미리보기 + 파일 드롭) | ✅ | #2, #3 |
| 7 | 메인 페이지 (랜딩 + 에디터 + 공유하기) | ✅ | #5, #6 |
| 8 | 인증 구현 (로그인, 콜백, 토큰 관리, 인증 가드) | ✅ | #2, #3 |
| 9 | 문서 뷰어 컴포넌트 (HTML + Markdown 렌더링) | ✅ | #2 |
| 10 | 문서 서빙 페이지 (공유 URL) | ✅ | #9 |
| 11 | 대시보드 페이지 (문서 목록 + 사용량) | ✅ | #5, #8 |
| 12 | 문서 편집 페이지 | ✅ | #6, #8 |
| 13 | 에러/상태 페이지 (404, 에러, 만료, 인증 에러) | ✅ | #4 |
| 14 | 백엔드 연동 테스트 및 통합 검증 | | #7, #8, #10, #11, #12 |
| 15 | 반응형 UI 및 다크모드 검증 | | #7, #11, #12 |
| 16 | 접근성(a11y) 및 SEO 최적화 | | #7, #10 |

---

## 1. 프로젝트 기반 설정 (Next.js 스캐폴딩)

- [x] Next.js 15 (App Router) + TypeScript 프로젝트 초기화
- [x] Tailwind CSS 설정
- [x] 루트 레이아웃 (`app/layout.tsx`) - 글로벌 스타일, 폰트(Geist), 메타데이터
- [x] `tsconfig.json` 경로 별칭 (`@/`) 설정
- [x] `package.json` 의존성 구성 (next, react, zustand, monaco-editor, unified 등)
- [x] `globals.css` Tailwind 기본 스타일

## 2. 공통 인프라 구현 (API 클라이언트, 상수, 유틸)

- [x] `lib/api/client.ts` - fetch 래퍼 (baseURL, 인증 헤더, 401 자동 갱신, 공통 응답 파싱)
- [x] `lib/api/types.ts` - API 응답/요청 타입 정의 (ApiResponse, ApiError, DocumentSummary, DocumentDetail, DocumentView, User, AuthTokens 등)
- [x] `lib/api/auth.ts` - 인증 API 함수 (oauthCallback, getMe, logout, generateOAuthState)
- [x] `lib/api/documents.ts` - 문서 API 함수 (createDocument, getMyDocuments, getDocument, getDocumentView, updateDocument, deleteDocument)
- [x] `lib/constants.ts` - 상수 정의 (MAX_CONTENT_SIZE, DOC_TYPES, FILE_EXTENSION_MAP, 쿠키명, OAuth 설정, DEFAULT_PAGE_SIZE)
- [x] `lib/auth/token.ts` - JWT 토큰 쿠키 관리 (getAccessToken, setTokens, clearTokens)
- [x] `lib/markdown.ts` - unified (remark + rehype) MD->HTML 변환
- [x] `lib/utils.ts` - 유틸리티 함수

## 3. Zustand 상태관리 구현

- [x] `lib/store/useAuthStore.ts` - 인증 상태 (user, isAuthenticated, login, logout, setUser)
- [x] `lib/store/useEditorStore.ts` - 에디터 상태 (content, docType, title, setContent, setDocType, setTitle, reset, loadDocument)

## 4. 공통 UI 컴포넌트 (shadcn/ui)

- [x] `components/ui/button.tsx`
- [x] `components/ui/card.tsx`
- [x] `components/ui/dialog.tsx`
- [x] `components/ui/input.tsx`
- [x] `components/ui/select.tsx`

## 5. 공통 레이아웃 컴포넌트 (Header)

- [x] `components/layout/Header.tsx` - 로고, 로그인/로그아웃 버튼, 내 문서 링크, 사용자 이름 표시
- [x] 인증 상태에 따른 네비게이션 분기
- [x] 로그아웃 처리 (API 호출 + 토큰 정리 + 상태 초기화)

## 6. 에디터 컴포넌트 (Monaco + 미리보기 + 파일 드롭)

- [x] `components/editor/EditorPanel.tsx` - Monaco Editor 래퍼 (SSR:false 동적 import, 언어 자동 전환)
- [x] `components/editor/PreviewPanel.tsx` - 실시간 미리보기 (HTML: iframe srcdoc, MD: unified 변환, 300ms debounce)
- [x] `components/editor/FileDropZone.tsx` - 파일 드래그 앤 드롭 + 파일 선택 (확장자 검증, 5MB 크기 제한, UTF-8 읽기)

## 7. 메인 페이지 (랜딩 + 에디터 + 공유하기)

- [x] `app/page.tsx` - 에디터 + 미리보기 2분할 레이아웃
- [x] 문서 타입 선택 (HTML/Markdown)
- [x] 제목 입력 (선택)
- [x] 파일 드래그 앤 드롭 영역
- [x] `components/common/PublishButton.tsx` - 공유하기 버튼 (비로그인/로그인 분기, 빈 내용 검증)
- [x] `components/common/PublishResultModal.tsx` - 공유 결과 모달 (URL 복사, 비로그인 만료 안내, 로그인 유도)
- [x] 비로그인 사용자 안내 문구 ("비로그인 문서는 대시보드에서 관리되지 않습니다")

## 8. 인증 구현 (로그인, 콜백, 토큰 관리, 인증 가드)

- [x] `app/auth/login/page.tsx` - Google/GitHub OAuth2 로그인 버튼, state 생성 + 쿠키 저장, provider 인가 URL 리다이렉트
- [x] `app/auth/callback/page.tsx` - 인가 코드 수신, state 검증 (CSRF 방지), Spring Boot 콜백 API 호출, 토큰 쿠키 저장, 대시보드 리다이렉트
- [x] `proxy.ts` - /dashboard/* 경로 인증 가드 (JWT 쿠키 확인, 미인증 시 /auth/login 리다이렉트)
- [x] provider 자동 판별 (Google: code가 "4/"로 시작)

## 9. 문서 뷰어 컴포넌트 (HTML + Markdown 렌더링)

- [x] `components/viewer/HtmlViewer.tsx` - sandbox iframe 렌더링 (allow-same-origin, allow-scripts)
- [x] `components/viewer/MarkdownViewer.tsx` - unified MD->HTML 변환 후 스타일링된 iframe 렌더링
- [x] 제목 헤더 표시 (title 존재 시)

## 10. 문서 서빙 페이지 (공유 URL)

- [x] `app/[slug]/page.tsx` - 서버 컴포넌트에서 Spring Boot API 호출
- [x] 200 정상 응답 시 DocumentViewPage 렌더링
- [x] 404 응답 시 notFound() 호출
- [x] 410 응답 시 만료/삭제 안내 UI 인라인 렌더링 (DOCUMENT_EXPIRED vs DOCUMENT_GONE 분기)
- [x] `app/[slug]/DocumentViewPage.tsx` - 상단 CTA 바 ("나도 만들어보기") + docType 분기 렌더링

## 11. 대시보드 페이지 (문서 목록 + 사용량)

- [x] `app/dashboard/layout.tsx` - 대시보드 레이아웃 (Header 포함)
- [x] `app/dashboard/page.tsx` - 문서 목록 조회, 사용자 정보/사용량 조회, 로딩 상태
- [x] `components/document/DocumentList.tsx` - 테이블 형태 문서 목록, 페이지네이션 (이전/다음), 빈 상태 UI
- [x] `components/document/DocumentCard.tsx` - 문서 행 (제목, 유형 배지, 크기, 상대 시간, 링크 복사/편집/삭제 버튼)
- [x] `components/document/DeleteConfirmDialog.tsx` - 삭제 확인 다이얼로그
- [x] `components/common/StorageUsageBar.tsx` - 사용량 프로그레스 바 (문서 수, 용량/최대용량)

## 12. 문서 편집 페이지

- [x] `app/dashboard/[slug]/edit/page.tsx` - 기존 문서 로드 (GET /documents/{slug}), 에디터 컴포넌트 재사용
- [x] 편집 헤더 (뒤로가기, 제목 입력, docType 배지, 저장 버튼)
- [x] 저장 처리 (PUT /documents/{slug}), 성공 토스트 (3초 자동 소멸)
- [x] 에러 상태 처리 (로드 실패 시 대시보드 이동 안내)

## 13. 에러/상태 페이지 (404, 에러, 만료, 인증 에러)

- [x] `app/not-found.tsx` - 404 페이지 (홈으로 돌아가기 링크)
- [x] `app/error.tsx` - 글로벌 에러 바운더리 (다시 시도, 홈으로 돌아가기)
- [x] `app/expired/page.tsx` - 만료 문서 안내 페이지 (?slug= 쿼리 지원, 로그인 유도)
- [x] `app/auth/error/page.tsx` - 인증 에러 페이지 (다시 로그인하기 링크)

## 14. 백엔드 연동 테스트 및 통합 검증

- [ ] 로컬 환경에서 백엔드(Spring Boot)와 프론트엔드 동시 실행 후 E2E 플로우 검증
- [ ] 비로그인 문서 생성 플로우: 붙여넣기 -> 미리보기 -> 공유하기 -> URL 생성 -> 공유 URL 접근
- [ ] 로그인 플로우: Google/GitHub OAuth2 로그인 -> 토큰 발급 -> 대시보드 리다이렉트
- [ ] 토큰 갱신 플로우: Access Token 만료 -> 자동 갱신 -> API 재시도
- [ ] 대시보드 인증 가드: 미인증 시 /auth/login 리다이렉트 + callbackUrl 유지
- [ ] 문서 CRUD 플로우: 생성 -> 목록 조회 -> 상세 조회 -> 수정 -> 삭제
- [ ] 문서 서빙 에러 분기: 404 (존재하지 않음), 410 DOCUMENT_EXPIRED (만료), 410 DOCUMENT_GONE (삭제)
- [ ] Rate Limiting 동작 확인: 429 응답 시 안내 UI 표시
- [ ] 파일 드래그 앤 드롭: .html, .htm, .md, .markdown 파일 읽기 + 5MB 초과 거부
- [ ] CORS 설정 확인: 프론트엔드 도메인에서 백엔드 API 호출 가능

## 15. 반응형 UI 및 다크모드 검증

- [ ] 모바일 뷰: 에디터/미리보기 단일 컬럼 (미리보기 숨김 처리 확인)
- [ ] 태블릿/데스크탑 뷰: 에디터/미리보기 2분할 레이아웃
- [ ] 대시보드 테이블 가로 스크롤 (overflow-x-auto)
- [ ] 다크모드 전체 페이지 시각 검증 (dark: 클래스 적용 확인)
- [ ] 파일 드롭존 드래그 오버 상태 시각 피드백

## 16. 접근성(a11y) 및 SEO 최적화

- [ ] 메타데이터 설정 확인 (title, description)
- [ ] 문서 서빙 페이지 SSR 동작 확인 (서버 컴포넌트에서 API 호출 -> SEO 크롤링 가능)
- [ ] 키보드 내비게이션: 모달 포커스 트랩, 버튼 Tab 순서
- [ ] 이미지/아이콘 대체 텍스트 (OAuth 로고 SVG에 aria-label 추가)
- [ ] lang="ko" 설정 확인
- [ ] Open Graph 태그 추가 (공유 URL 미리보기용)
