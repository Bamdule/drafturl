# DraftURL MVP 개선점 분석

> 분석일: 2026-03-24
> 대상: 프론트엔드, 백엔드, 인프라, 문서 전체
> 참조: `docs/reviews/` 하위 기존 리뷰 8건, 소스 코드 전체

---

## 분석 요약

DraftURL MVP는 "붙여넣기 -> URL 생성 -> 공유" 핵심 루프가 동작하는 상태이며, 코드 품질과 아키텍처 완성도가 전반적으로 높다. 기존 코드 리뷰에서 식별된 Critical 이슈 4건(OAuth CSRF, 스케줄러 트랜잭션, StorageUsage 음수, Jsoup Safelist) 중 4건 모두 수정 완료되었다. 그러나 프론트엔드 보안(non-httpOnly 토큰), UX 미완성 영역, CI/CD 부재, 모니터링 미설정 등 프로덕션 운영 준비를 위한 개선이 필요하다.

---

## 1. UX/UI 개선

### P0 (즉시)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| U-1 | 모바일 에디터 UX 부재 | 좌우 분할 레이아웃이 모바일에서 세로 스택되지만, 에디터(Monaco)가 모바일에서 사용하기 어려움. 최소 높이 600px 고정 | 모바일에서 탭 전환(에디터/미리보기) 또는 단순 textarea 폴백 제공. 모바일 사용자가 간단한 수정 가능 |
| U-2 | 편집 페이지 미저장 경고 없음 | 문서 수정 중 페이지 이탈 시 변경사항 무경고 유실 | `beforeunload` 이벤트로 미저장 확인 다이얼로그 표시 |
| U-3 | 대시보드 로딩 상태 빈약 | "로딩 중..." 텍스트만 표시. 레이아웃 시프트 발생 | 스켈레톤 UI 적용 (카드 3개 + 문서 목록 플레이스홀더) |

### P1 (단기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| U-4 | 인증 상태 깜빡임 (flash) | 새로고침 시 Zustand 초기화로 비로그인 UI가 잠깐 표시 후 복구 | 쿠키 존재 여부로 초기 상태 설정 또는 persist 미들웨어 적용. 헤더 깜빡임 제거 |
| U-5 | OAuth 에러 시 무반응 | 로그인 페이지에서 OAuth state 발급 실패 시 사용자에게 피드백 없음 (`catch` 블록에서 로딩만 해제) | 에러 메시지 표시: "로그인 연결에 실패했습니다. 다시 시도해주세요." |
| U-6 | 클립보드 실패 시 거짓 성공 메시지 | `DocumentViewPage`에서 clipboard API 실패 시에도 "URL이 복사되었습니다!" 표시 | 실패 시 "URL 복사에 실패했습니다. 주소창에서 직접 복사해주세요." 표시 |
| U-7 | 문서 서빙 페이지에 CTA 없음 | 공유 URL 접속 시 DraftURL 브랜딩/CTA가 전혀 없음. 가입 유도 기회 상실 | product.md 설계대로 상단 바에 "이 문서는 N시간 후 만료됩니다. 나도 만들어보기" CTA 추가 |
| U-8 | 만료 문서 안내 페이지와 expired 라우트 공존 | `app/[slug]/page.tsx`에서 410을 인라인 처리하지만, `app/expired/page.tsx`도 별도 존재. 용도가 불명확 | 인라인 처리로 통일하고 expired 라우트 제거, 또는 역할을 명확히 구분 |
| U-9 | 에디터 글로벌 스토어 상태 오염 | 메인 페이지 -> 편집 페이지 이동 시 이전 내용이 잠깐 보임. 뒤로 가기 시 편집 내용이 메인에 잔류 | 편집 페이지 `useEffect` cleanup에서 `reset()` 호출. 또는 페이지별 로컬 상태 분리 |

### P2 (중기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| U-10 | 접근성(a11y) 미지원 | Dialog에 `aria-labelledby`/`aria-describedby` 없음. 키보드 탐색 일부 미흡 | 스크린 리더 호환성 확보. WCAG 2.1 AA 수준 달성 |
| U-11 | 에러 코드별 사용자 메시지 부재 | 백엔드 12가지 에러 코드에 대한 프론트엔드 매핑 없음. 대부분 기술적 메시지 그대로 노출 | 에러 카테고리별 사용자 친화적 메시지 매핑 (인증 에러 -> 재로그인 유도, Rate Limit -> 잠시 후 재시도 등) |
| U-12 | 다크모드만 지원 | `html` 클래스에 `dark` 고정. 라이트모드 미지원 | 시스템 설정 감지 또는 토글 추가. 라이트모드 사용자 접근성 향상 |

---

## 2. 성능 최적화

### P1 (단기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| F-1 | 매 키 입력마다 Blob 생성 (바이트 카운트) | `EditorPanel`에서 `new Blob([content]).size`를 `useEffect` 내 매 렌더마다 호출 | `TextEncoder().encode(content).byteLength`로 교체 + debounce. 대용량 문서 편집 시 프레임 드롭 방지 |
| F-2 | Monaco Editor 번들 크기 | `@monaco-editor/react` 전체 번들을 클라이언트에 로드. 초기 로딩에 영향 | 동적 import + 로딩 스피너. 또는 에디터 Web Worker 최적화. 초기 번들 크기 30-40% 감소 예상 |
| F-3 | 서버 컴포넌트 캐싱 미적용 | `app/[slug]/page.tsx`의 `getDocumentViewServer()`에 Next.js 캐싱/revalidation 설정 없음 | `fetch`에 `next: { revalidate: 60 }` 추가. 인기 문서의 반복 API 호출 감소 |

### P2 (중기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| F-4 | 백엔드 ETag 이중 DB 조회 | `DocumentController.viewDocument()`에서 문서 조회 + ETag 생성으로 DB 2회 호출 | ETag를 응답 DTO에 포함하여 단일 조회로 통합. API 응답 지연 20-30% 감소 |
| F-5 | ContentSizeValidator 메모리 이중 할당 | `getBytes(UTF-8)` 호출이 Validator와 Service에서 각각 발생. 5MB 문서에서 최대 15MB 힙 사용 | Validator에서 계산한 바이트 크기를 request attribute로 전달하여 재활용 |
| F-6 | Markdown 미리보기 스타일 중복 | `PreviewPanel.tsx`와 `MarkdownViewer.tsx`에 거의 동일한 CSS 하드코딩 | 공통 `wrapMarkdownHtml()` 유틸리티로 추출. 스타일 변경 시 한 곳만 수정 |

---

## 3. 보안

### P0 (즉시)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 | 리뷰 참조 |
|---|------|----------|-----------------|----------|
| S-1 | JWT가 non-httpOnly 쿠키에 저장 | `token.ts`에서 `document.cookie`로 직접 접근 가능. XSS 시 토큰 탈취 가능 | Next.js Server Action 또는 API Route를 프록시로 사용하여 httpOnly 쿠키 전환. 또는 최소한 Phase 2 전환 로드맵 명문화 | 프론트엔드 리뷰 C2 |
| S-2 | GitHub OAuth email null 미처리 | GitHub에서 email을 비공개 설정한 사용자는 `email: null` 반환. `User` 엔티티의 `email`은 `nullable=false`이므로 INSERT 실패 | `/user/emails` API 추가 호출로 primary email 획득, 또는 email nullable 허용 | 백엔드 리뷰 추가 고려사항 4 |

### P1 (단기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 | 리뷰 참조 |
|---|------|----------|-----------------|----------|
| S-3 | OAuth provider 판별을 인가 코드 prefix로 추측 | `code.startsWith("4/") ? "google" : "github"` -- 신뢰할 수 없는 추측 | state 파라미터에 provider 인코딩, 또는 콜백 URL을 provider별로 분리 (`/auth/callback/google`, `/auth/callback/github`) | 프론트엔드 리뷰 M1 |
| S-4 | OAuth2 Provider RestClient 에러 핸들링 부족 | Google/GitHub API의 4xx 응답이 500 INTERNAL_ERROR + Sentry 알림으로 처리됨 | `.onStatus()` 핸들러로 4xx는 `OAuth2Exception`으로 래핑. 불필요한 Sentry 노이즈 제거 | 백엔드 리뷰 M-6 |
| S-5 | Sentry 이중 보고 가능성 | `GlobalExceptionHandler` + `sentry-spring-boot-starter` 자동 보고가 겹칠 수 있음 | 직접 `Sentry.captureException()` 호출 제거하고 starter 자동 통합에 위임, 또는 자동 보고 비활성화 후 수동 제어 | 백엔드 리뷰 추가 고려사항 2 |

### P2 (중기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| S-6 | CSP(Content-Security-Policy) 헤더 미설정 | 프론트엔드에 CSP 헤더 없음. iframe sandbox만으로 XSS 방어 | 서빙 페이지에 CSP 헤더 추가 (`frame-src 'self'`, `script-src 'self'`). 방어 레이어 추가 |
| S-7 | Rate Limit 인메모리 구현의 한계 | `ConcurrentHashMap` 기반이므로 서버 재시작 시 리셋, 멀티 인스턴스 시 공유 불가 | Phase 2에서 Redis 기반으로 전환. 분산 환경에서도 일관된 Rate Limiting |

---

## 4. 코드 품질

### P1 (단기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| Q-1 | 백엔드 테스트 부족 | RestDocs 테스트 2개 + Sanitizer 테스트 1개 + OAuthState 테스트 1개만 존재. UseCase/Service 단위 테스트 없음 | 핵심 UseCase (문서 생성/수정/삭제, 토큰 갱신) 단위 테스트 추가. 회귀 방지 및 리팩토링 안전망 확보 |
| Q-2 | 프론트엔드 테스트 전무 | 테스트 파일 0개. 컴포넌트 테스트, E2E 테스트 모두 없음 | 최소 핵심 플로우 E2E 테스트 (문서 생성, 로그인, 대시보드). Playwright 또는 Cypress 도입 |
| Q-3 | `LocalDateTime.now()` 직접 호출 | 전반적으로 `LocalDateTime.now()`를 직접 호출. 시간 관련 로직 테스트 불가 | `Clock` Bean 주입 패턴 전환. `LocalDateTime.now(clock)` 사용. 만료 관련 테스트 정확도 향상 |
| Q-4 | `ForbiddenException` 클래스 2개 존재 | `document.exception.ForbiddenException`과 `user.exception.ForbiddenException` 중복 | `global/exception/ForbiddenException`으로 통합. import 혼란 제거 |
| Q-5 | `SlugGenerator`가 `IllegalStateException` 사용 | slug 생성 재시도 초과 시 `IllegalStateException` -> 500 INTERNAL_ERROR | `BusinessException`으로 교체. 사용자에게 의미있는 에러 메시지 제공 |

### P2 (중기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| Q-6 | `BaseEntity`에 `LocalDateTime` 사용 | 타임존 정보 없음. 서버 TZ에 의존 | `Instant` 또는 `OffsetDateTime`으로 전환 + JVM `-Duser.timezone=UTC` 설정 |
| Q-7 | `StorageException.initCause()` 비표준 패턴 | `super()`가 아닌 `initCause()`로 cause 전달. cause 이중 설정 시 예외 발생 가능 | `BusinessException`에 cause 파라미터 생성자 추가 후 `super(message, cause)` 사용 |
| Q-8 | `MdcFilter` userId 추출 로직 | `authentication.getName()`이 `UserPrincipal.toString()` 반환. MDC에 전체 record 문자열이 기록됨 | `instanceof UserPrincipal` 체크 후 `principal.userId().toString()` 직접 추출 |
| Q-9 | 프론트엔드 서버/클라이언트 API 클라이언트 혼용 | `apiFetch`는 클라이언트 전용(`document.cookie` 접근)이나, 서버 컴포넌트에서도 사용될 수 있는 구조 | 서버 컴포넌트용 별도 API 클라이언트(`serverFetch`) 명확히 분리. Next.js `cookies()` API 활용 |

---

## 5. 인프라/배포

### P0 (즉시)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| I-1 | CI/CD 파이프라인 부재 | GitHub Actions 워크플로우 미구성. 수동 Docker 빌드 + Cloudflare Tunnel로 서빙 | 최소 CI: PR 체크 (lint + build + test). 코드 품질 게이트 확보. 배포 자동화 기반 마련 |
| I-2 | 프로덕션 배포 구조 미확립 | Cloudflare Tunnel을 통해 로컬 머신에서 직접 서빙. 서버 다운 시 서비스 중단 | 클라우드 배포 (Railway + Vercel 또는 단일 VPS). 고가용성 확보 |
| I-3 | 헬스체크/모니터링 미설정 | Sentry DSN 미설정 상태 추정 (환경변수 미확인). Actuator health만 노출 | Sentry DSN 설정 + Vercel Analytics 연동. 에러 알림 + 성능 모니터링 활성화 |

### P1 (단기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| I-4 | 환경변수 관리 비체계적 | docker-compose 파일마다 환경변수 하드코딩. `.env` 파일 미사용 | `.env.example` 제공 + 실제 값은 `.env.local`(gitignore). 새 개발자 온보딩 용이 |
| I-5 | 백엔드 Dockerfile 멀티스테이지 미적용 | `Dockerfile.dev`에서 호스트 빌드 JAR을 복사. CI 환경과 JDK 버전 불일치 가능 | 멀티스테이지 빌드로 전환. 빌드 환경 일관성 보장 |
| I-6 | 프론트엔드 `NEXT_PUBLIC_API_URL` 하드코딩 의존 | dev 환경에서 `NEXT_PUBLIC_API_URL`이 설정되지 않으면 `localhost:8080` 폴백 | 환경별 명확한 설정 분리. 서버 사이드 프록시(`proxy.ts`) 활용 확대 |
| I-7 | MinIO 버킷 자동 생성 미구현 | 최초 실행 시 수동으로 MinIO 버킷을 생성해야 함 | docker-compose init 컨테이너 또는 Spring Boot `ApplicationRunner`로 자동 생성 |

### P2 (중기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| I-8 | 로그 수집/분석 부재 | 구조화된 로깅(logstash-logback-encoder) 설정되었으나, 로그 수집 대상 없음 | Grafana Cloud Free 티어 등으로 로그 수집. 운영 이슈 사후 분석 가능 |
| I-9 | DB 백업 전략 없음 | PostgreSQL 데이터가 Docker 볼륨에만 존재. 볼륨 삭제 시 전체 데이터 유실 | 프로덕션 DB에 일일 자동 백업 + 복구 테스트. 데이터 보호 |
| I-10 | Spring Boot 콜드 스타트 | JVM 기반이므로 첫 요청 시 지연 발생 (인프라 문서에서도 언급) | 워밍업 엔드포인트 또는 keep-alive 핑. 사용자 체감 첫 응답 개선 |

---

## 6. 비즈니스/제품

### P0 (즉시)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| B-1 | 사용자 행동 분석 없음 | Google Analytics, Vercel Analytics 등 미연동. KPI(문서 생성 완료율, 가입 전환율) 측정 불가 | 최소 GA4 연동. MVP 성공 기준(생성 완료율 70%, 가입 전환율 5%) 측정 가능 |
| B-2 | 비로그인 -> 로그인 전환 유도 약함 | 문서 생성 후 결과 모달에 "로그인하면 영구 보관" 메시지만 표시. 서빙 페이지에 CTA 없음 | 서빙 페이지 상단 바 CTA + 결과 모달 개선 (로그인 버튼 강조, 카운트다운 표시). 전환율 향상 |

### P1 (단기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| B-3 | SEO 최적화 미흡 | 루트 레이아웃에 기본 OG 태그 있으나, 공유 문서(`/[slug]`)의 OG 이미지 미지원 | 동적 OG 이미지 생성 (Next.js `ImageResponse`). SNS 공유 시 미리보기 품질 향상 -> 바이럴 효과 |
| B-4 | 랜딩 페이지 차별화 부족 | "LLM이 만든 문서를 3초 만에 공유하세요"가 유일한 가치 제안. 경쟁사(tiiny.host) 대비 차별점 미강조 | "AI 수정 Coming Soon" 대신, 실제 사용 사례/데모 영상 추가. 사용자가 가치를 즉시 이해 |
| B-5 | 이메일 수집/리텐션 미구현 | 가입 후 리텐션 수단 없음. 사용자가 서비스를 잊을 가능성 높음 | 문서 만료 사전 알림 이메일 (24h/7d 만료 N시간 전). 재방문 유도 + 업그레이드 기회 |

### P2 (중기)

| # | 항목 | 현재 상태 | 개선 후 기대 효과 |
|---|------|----------|-----------------|
| B-6 | 결제/플랜 시스템 미구현 | product.md에 Free/Starter/Pro/Team 가격 정의 있으나, 구현 없음. Free 플랜의 7일 만료도 미적용 | Stripe 연동 + 플랜별 제한 적용. 수익화 경로 확보 |
| B-7 | 국제화(i18n) 미준비 | UI 텍스트가 한국어 하드코딩. 영어 사용자 접근 불가 | 최소 영어 지원 추가. 글로벌 사용자 확보 가능성 |
| B-8 | 공유 URL의 QR 코드 미지원 | URL 복사만 가능. 오프라인 공유 불편 | QR 코드 생성 버튼 추가. 발표/미팅 현장에서 즉시 공유 |

---

## 해결 완료된 이슈 (기존 리뷰 기준)

다음 이슈들은 기존 코드 리뷰에서 식별되었으며, 이미 수정 완료되었다.

| 리뷰 ID | 제목 | 수정 확인 문서 |
|---------|------|--------------|
| 백엔드 C-1 | OAuth2 state CSRF 방어 | `docs/reviews/20260323-oauth-state-hmac-self-review.md` |
| 백엔드 C-2 | 스케줄러 @Transactional 부재 | `docs/reviews/20260323-c2-scheduler-transactional-review.md` |
| 백엔드 C-3 | StorageUsage 음수 방지 | `docs/reviews/20260323-C3-storage-usage-negative-prevention-review.md` |
| 백엔드 C-4 | Jsoup Safelist 미사용 | `docs/reviews/20260323-C4-jsoup-safelist-fix-review.md` |
| 백엔드 M-2/M-8 | DocumentService 비대화 + N+1 배치 | `docs/reviews/20260323-M2-M8-batch-optimization-review.md` |
| 프론트엔드 C1 | iframe sandbox allow-same-origin 제거 | `HtmlViewer.tsx` -- `sandbox="allow-scripts"`로 수정 확인 |
| 프론트엔드 m3 | getCookie/deleteCookie 중복 | `lib/utils/cookie.ts`로 통합 확인 |
| 프론트엔드 S3 | 토큰 쿠키 Secure 플래그 | `cookie.ts`에서 HTTPS 감지 후 `Secure` 자동 설정 확인 |
| 프론트엔드 M6 | dangerouslySetInnerHTML 제거 | `page.tsx`에서 유니코드 문자 직접 사용으로 변경 확인 |

---

## 우선순위별 액션 플랜

### Phase 즉시 (P0) -- 이번 주

1. **I-1**: GitHub Actions CI 파이프라인 구성 (lint + build + test)
2. **B-1**: Google Analytics 4 연동
3. **S-1**: non-httpOnly 토큰의 Phase 2 전환 로드맵 명문화 (당장 변환 어려우면 최소한 위험 인지 문서화)
4. **S-2**: GitHub OAuth email null 처리
5. **U-1**: 모바일 에디터 최소 사용성 확보
6. **I-2**: 프로덕션 배포 구조 확립 (현재 로컬 Cloudflare Tunnel 의존 탈피)

### Phase 단기 (P1) -- 2주 내

1. **Q-1, Q-2**: 핵심 테스트 추가 (백엔드 UseCase + 프론트엔드 E2E)
2. **S-3**: OAuth provider 판별 안정화
3. **U-4 ~ U-9**: UX 개선 일괄 (인증 깜빡임, 에러 처리, CTA 등)
4. **F-1 ~ F-3**: 프론트엔드 성능 최적화
5. **B-3, B-4**: SEO + 랜딩 페이지 개선

### Phase 중기 (P2) -- 1개월 내

1. **B-6**: 결제 시스템 + 플랜 제한 적용
2. **S-6**: CSP 헤더 설정
3. **Q-6 ~ Q-9**: 코드 품질 리팩토링
4. **I-8, I-9**: 로그 수집 + DB 백업
5. **B-5**: 만료 알림 이메일

---

## 문서 관리

| 날짜 | 변경 내용 |
|------|----------|
| 2026-03-24 | 초안 작성 -- 프론트엔드/백엔드/인프라/비즈니스 전 영역 분석 |
