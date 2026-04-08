# Changelog

이 문서는 DraftURL의 모든 주요 변경사항을 기록합니다.
형식은 [Keep a Changelog](https://keepachangelog.com)를 따릅니다.

---

## [v0.10.1] - 2026-04-08

한글 IME Enter 이중 발화 버그 수정, QA 시나리오 보강.

### Fixed
- 한글 IME 조합 중 Enter 키로 태그가 중복 생성되던 버그 (`isComposing` 체크 누락)
  - 영향 범위: HomePage 공유 전 태그 입력, TagPicker(대시보드 문서 카드)

### Docs
- QA 시나리오 전면 갱신: TC-CREATE-11~19, TC-MANAGE-12~20 추가

---

## [v0.10.0] - 2026-04-07

공유 전 태그/비밀번호 설정, 태그 생명주기 개선, 내 문서 대시보드 전면 개선.

### Added
- 홈 화면 공유 전 태그/비밀번호 설정 섹션 (로그인 사용자)
- 문서 미리보기(preview) 필드: 내 문서 카드에 내용 첫 120자 표시
- 내 문서 정렬 기능: 최신순 / 제목순 / 크기순
- 커스텀 정렬 드롭다운 컴포넌트
- 검색창 초기화(`×`) 버튼

### Improved
- 태그 자동 삭제: 마지막 문서 연결 해제 또는 문서 삭제 시 빈 태그 자동 제거
- 태그 생성 지연: 공유 버튼 클릭 전까지 API 미호출 (고아 태그 방지)
- 내 문서 카드: compact 모드, hover left-border, 자동 제목 italic, 0-count 태그 필터 숨김
- 검색 빈 상태 메시지 분리 (문서 없음 vs 검색 결과 없음)
- 전체 글씨 밝기 상향 조정
- PublishResultModal 단순화 (URL 표시 전용)

### Fixed
- 내 문서 제목 검색 실패: macOS NFD 파일명 vs 검색 NFC 불일치 → NFC 정규화
- null 제목 문서가 검색에서 제외되던 문제 → COALESCE 처리

### DB Migrations
- V7: 태그 기능 (tags, document_tags 테이블)
- V8: null 제목 날짜 기반 일괄 업데이트
- V9: 기존 NFD 제목 NFC 정규화
- V10: documents.preview 컬럼 추가

---

## [v0.9.5] - 2026-04-03

게스트 문서 이관, PublishResultModal UX 개선.

### Added
- 게스트 문서 이관 기능: 비로그인 문서 생성 후 로그인 시 자동으로 내 계정에 영구 보관
- 백엔드: `POST /api/v1/documents/{slug}/claim` 엔드포인트
- 프론트엔드: 로그인 후 콜백에서 pendingClaimSlug 처리

### Improved
- PublishResultModal: 비로그인 사용자에게 로그인 유도 배너 (24시간 만료 경고 강조)
- PublishResultModal: 로그인 사용자에게 "편집하기" 버튼 추가
- 에디터 placeholder 개선 (Claude/ChatGPT/Cursor 언급)

---

## [v0.9.4] - 2026-04-03

랜딩 페이지 UX 개선 — Ctrl+V 붙여넣기 기능 구현, 레이아웃 수정.

### Added
- Ctrl+V 전체 페이지 붙여넣기 기능 구현 (HTML/Markdown 자동 감지)

### Fixed
- 드롭존 button-in-button HTML 구조 오류 수정 (role="button" div 내부 button 중첩)
- 수직 레이아웃 중앙 정렬 개선 (flex-1 items-center 래퍼 추가)
- use case 태그 및 만료 안내 문구 모바일에서도 노출 (`hidden md:*` → 항상 표시)

---

## [v0.9.3] - 2026-04-03

SEO canonical 수정, 랜딩 페이지 UI 개선.

### Fixed
- SEO: canonical 태그가 `localhost:3000`을 가리키던 문제 수정 (`NEXT_PUBLIC_SITE_URL` Dockerfile 누락)
- SEO: 루트(`/`) 리다이렉트 307 → 301 변경 (Google에 영구 리다이렉트 신호 전달)

### Improved
- 랜딩 페이지 히어로 타이틀 fade-up 진입 애니메이션
- gradient-text 색상 이동 애니메이션 (gradient-shift)
- use case 태그 섹션 추가 ("Claude 결과물 공유", "ChatGPT 보고서 전달" 등)
- 파일 드롭존 문구 개선 (Ctrl+V 힌트, 파일 선택하기 버튼 추가)

### Docs
- 구버전 태스크 파일 `docs/archive/tasks/`로 정리

---

## [v0.9.2] - 2026-04-02

MCP 인증 플로우 개선, 보안 수정.

### Added
- MCP 로그인에 "Continue with Google" 지원 (Google OAuth 사용자도 MCP 인증 가능)
- 프론트엔드 로그인 returnTo 파라미터 (MCP → Google 로그인 → MCP 복귀)
- 1:1 문의에 "콘텐츠 신고" 유형 추가

### Fixed
- MCP consent user_id 보안 수정 (hidden field → 세션 쿠키에서만 추출)
- MCP 에러 응답 안정화 (수동 문자열 조합 → ObjectMapper)

### Removed
- 문서 뷰어 신고 버튼 제거 (1:1 문의로 통합)

---

## [v0.9.1] - 2026-04-02

전체 UI/UX 개선, 모바일 최적화, 버그 수정.

### Added
- 가이드/MCP 페이지에 Header + Footer 추가 (네비게이션 일관성)
- MCP 가이드 코드 블록에 Copy 버튼 추가
- Google 사이트 인증 메타태그 (drafturl.team 계정)

### Changed
- 모바일 모달 UI 개선: 버튼 간격, 패딩, 세로 배치
- 공유 결과 모달: URL/액션 버튼 모바일 반응형
- 비밀번호 설정 UI를 공유 결과 모달로 이동 (로그인 사용자만)
- 공유 후 에디터/드롭존 상태 초기화 (중복 공유 방지)
- 로그인 페이지: 미사용 소셜 버튼 제거 (Google만 유지)
- 로그인 비밀번호 placeholder 중복 제거

### Fixed
- OAuth 이메일 중복 시 사용자 친화적 다국어 메시지 (500 에러 → 409 + 안내)
- 비밀번호 입력 시 브라우저 저장 팝업 방지 (type=text + CSS 마스킹)
- NEXT_PUBLIC_GOOGLE_CLIENT_ID 빌드 타임 주입 (Dockerfile.dev ARG)
- 홈 드롭존 Ctrl+V 힌트 제거

---

## [v0.9.0] - 2026-04-01

랜딩페이지 전면 리디자인, 탈퇴 로그, Resend 이메일 알림, 스케줄러 최적화.

### Added
- 랜딩페이지 리디자인: 2컬럼 레이아웃 (히어로+3스텝 | 드롭존+공유), 모바일 반응형
- 탈퇴 로그: withdrawal_logs 테이블 (V6 마이그레이션), 이메일 SHA-256 해시 기록
- Resend 이메일 알림: 문의 접수 시 관리자 Gmail로 알림 발송
- 회원탈퇴 시 문서에 1시간 만료 부여 → 기존 배치가 R2 파일과 함께 정리
- 사용자 획득 전략 문서 (docs/plans/product/growth-strategy.md)

### Changed
- 랜딩페이지: Monaco Editor/미리보기 패널 제거 → 드롭존 기반 UI
- HomeStaticSections: Features/Steps/FAQ → 가이드 페이지로 이동, Footer만 잔류
- 가이드 페이지: 가이드 목록 최상단 배치, 시작하기/주요기능/FAQ 구성
- PendingCleanupScheduler 주기 5분 → 1시간
- 배포 설계 문서: Railway/Vercel → Docker Compose + Tunnel 현행화
- 프론트엔드 페이지 설계 문서 현행화

---

## [v0.8.0] - 2026-04-01

가이드 페이지 추가 — MCP 연결 방법 안내.

### Added
- 가이드 허브 페이지 (/guide) — 가이드 목록을 카드 형태로 표시
- MCP 연결 가이드 (/guide/mcp) — Claude Desktop, Claude Code, Cursor 연결 방법, 도구 목록, 인증 설명
- 헤더에 "가이드" 링크 추가 (데스크톱/모바일 모두)
- 가이드 콘텐츠를 HTML 파일로 관리 (docs/guide/, 다국어 지원)

---

## [v0.7.1] - 2026-03-31

랜딩페이지 UI/UX 개선 — 시각적 완성도 및 전환율 향상.

### Added
- Hero 섹션에 CTA 버튼 ("바로 시작하기") 추가 — 에디터 영역으로 스크롤 유도
- 에디터 위 드래그앤드롭 배너 — .html/.md 파일 드롭 기능을 시각적으로 안내
- Features 카드에 아이콘 추가 (Zap, Eye, Link, Sparkles) — 시각적 스캔 가능성 향상
- 페이지 하단 마무리 CTA 섹션 — FAQ 아래 "지금 바로 공유해보세요" 행동 유도

---

## [v0.7.0] - 2026-03-31

1:1 문의, 버그 리포트, 콘텐츠 신고 기능 추가. 서비스 이메일 구축 완료.

### Added
- 문의 페이지 (/contact) — 일반 문의, 버그 리포트 폼 (로그인 시 이메일/이름 자동 채움)
- 콘텐츠 신고 기능 — 문서 뷰어에서 신고 버튼 + Dialog
- Backend: Inquiry 도메인 (POST /api/v1/inquiries, DB 저장)
- DB 마이그레이션 V5: inquiries 테이블 추가
- 서비스 이메일: Cloudflare Email Routing (support@, abuse@drafturl.com)
- 내 정보 페이지 (/account) — 계정 정보 확인 및 회원탈퇴
- 배포 워크플로우에 백로그 자동 갱신 단계 추가

### Changed
- 푸터 문의 링크: mailto → /contact 페이지로 변경

---

## [v0.6.1] - 2026-03-31

SEO 개선. 정적 콘텐츠 Server Component 분리, OG 이미지 추가, FAQPage/HowTo 구조화 데이터 스키마 추가, favicon 수정.

### Added
- `HomeStaticSections` Server Component — Feature Cards, How-to-Use Steps, FAQ 섹션을 서버 렌더링으로 분리하여 크롤러 텍스트 인식 개선
- FAQ 섹션 — 홈페이지 하단에 자주 묻는 질문 6개 추가 (en/ko)
- OG 이미지 — `/en`, `/ko` 경로에 `opengraph-image.tsx` 추가 (소셜 공유 미리보기)
- FAQPage JSON-LD 스키마 — Google 리치 스니펫(FAQ 펼치기) 대응
- HowTo JSON-LD 스키마 — 3단계 사용법 구조화 데이터

### Fixed
- `favicon.ico` (Next.js 기본 삼각형 아이콘) 삭제 → `favicon.svg` (D 로고) 적용

---

## [v0.6.0] - 2026-03-30

HTML sanitizer 보안 강화. 다층 방어 체계를 통해 피싱, 악성 리다이렉트, 데이터 유출을 방지한다.

### Added
- CSP `connect-src 'none'` meta 태그 자동 주입 (fetch/XHR/WebSocket 차단)
- ContentValidator 포트 — 업로드 시점에 악성 콘텐츠 탐지 및 거부
- 피싱 탐지: 유명 브랜드(네이버, 구글, 카카오 등) 사칭 + password input 조합 차단
- 악성 리다이렉트 탐지: meta refresh 외부 URL, JS location 변경 차단
- MaliciousContentException — 악성 콘텐츠 업로드 시 400 응답

### Changed
- 폼 태그(form, input, select, button 등) 허용으로 정책 변경 (sandbox iframe이 폼 제출 차단)
- `type="password"` input을 `type="text"`로 자동 변환 (피싱 UI 무력화)

### Security
- 4중 방어 체계: ContentValidator(업로드 차단) → ContentSanitizer(CSP 주입, password 변환) → CSP(네트워크 차단) → Sandbox iframe(폼 제출, 내비게이션 차단)

---

## [v0.5.0] - 2026-03-30

법적 약관 페이지 및 노출 위치 구현. 국내외 서비스를 위한 이용약관, 개인정보처리방침, 이용제한정책 한국어/영어 페이지 추가.

### Added
- 이용약관 페이지 (/terms) — docs/legal/ Markdown 파일 빌드 타임 렌더링
- 개인정보처리방침 페이지 (/privacy)
- 이용제한정책 페이지 (/aup)
- 한국어/영어 다국어 약관 지원 (locale별 md 파일)
- 비회원 문서 생성 시 약관 동의 텍스트 (PublishButton)

### Changed
- 홈페이지 Footer 약관 링크 연결 (href="#" → /terms, /privacy)
- 회원가입/로그인 페이지 약관 링크 연결

---

## [v0.4.0] - 2026-03-30

회원탈퇴(Hard Delete) 기능과 문서 생성 제한 서버 사이드 강화, 내 정보 페이지 신설.

### Added
- 회원탈퇴 기능 (DELETE /api/v1/auth/me, Hard Delete + FK cascade)
- 문서 생성 제한 백엔드 강제 (Free 플랜 30개, DOCUMENT_LIMIT_EXCEEDED 에러)
- 내 정보 페이지 (/account) — 계정 정보 확인 및 회원탈퇴

### Changed
- Header 아바타를 /account 페이지 링크로 변경
- SecurityConfig: DELETE /api/v1/auth/me authenticated 규칙 추가

---

## [v0.3.0] - 2026-03-30

사용자 접근성 확대, 보안 강화, MCP 서버 구현, SEO/모니터링 추가.

### Added
- 네이버/카카오 OAuth2 소셜 로그인 추가
- MCP 서버 + OAuth2 인증 (Claude Desktop 등 AI 클라이언트 연동)
- 공유 문서 비밀번호 보호 기능
- SEO 다국어 라우팅 (i18n: `/ko`, `/en`)
- Google Search Console 연동 및 소유권 확인
- Umami 트래킹 + 커스텀 이벤트 추적
- Uptime Kuma 모니터링 구성
- 백엔드 단위 테스트 보강 (101개)
- Sanitizer 피싱 방지 테스트 5개 추가
- VERSION 파일 기반 버전 관리 체계 도입

### Fixed
- HTML Sanitizer 피싱 방지 강화: form/input/password 등 폼 관련 태그 12종 제거
- 문서 생성(CREATE) 경로에 sanitizer 미적용 문제 수정
- Google Search Console 피싱 경고 원인 해결
- 보안 점검: XSS, 헤더, 프록시 검증, 인증 미들웨어 수정

### Changed
- 전체 UI/UX 개선 (로그인/회원가입/홈/404/대시보드)
- 배포 트리거를 release 브랜치로 변경

---

## [v0.2.0] - 2026-03-28

개발 인프라 자동화, 사용성 개선, 모바일 대응, SEO 기본 설정.

### Added
- GitHub Actions Self-hosted Runner 기반 자동 배포
- 서비스 소개 및 사용 가이드
- SEO 기본 설정 (robots, sitemap, OG/Twitter Card)

### Fixed
- 프론트엔드 lint 에러 전량 수정

### Changed
- 메인 페이지 데모 데이터 미리보기만 허용
- 모바일 레이아웃 지원
- 문서 기본 제목 형식 변경

---

## [v0.1.0] - 2026-03-27

MVP 최초 배포.

### Added
- HTML/Markdown 붙여넣기 즉시 배포 (nanoid 8자리 slug URL)
- Monaco Editor 기반 실시간 미리보기
- Markdown → HTML 자동 렌더링
- Google / GitHub OAuth2 소셜 로그인
- JWT 인증 (httpOnly 쿠키 BFF 프록시 패턴)
- 대시보드 문서 목록 조회, 수정, 삭제
- 비로그인 24시간 만료 / 로그인 영구 보관 정책
- Cloudflare R2 파일 저장소 (CDN 직접 서빙)
- Docker Compose 기반 개발서버 (Galaxy Book 3 Pro)
- Cloudflare Tunnel 외부 노출

### Fixed
- 로그인 성공 후 대시보드 리다이렉트 안 되는 문제 수정

### Changed
- MinIO → Cloudflare R2로 파일 저장소 전환
- 문서 조회를 CDN 직접 서빙 방식으로 변경
