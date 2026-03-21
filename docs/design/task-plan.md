# DraftURL MVP 태스크 계획

> 작성일: 2026-03-21
> 참조 문서:
> - docs/plans/file-storage/file-upload-storage-plan.md (MVP 계획서)
> - docs/design/mvp-service-design.md (MVP 설계 문서)
> - docs/plans/domain-analysis.md (도메인 분석)
> - docs/plans/service-plan.md (전체 서비스 계획서)

---

## 진행 현황

- 전체: 0/45 완료
- 인프라: 0/9
- 백엔드: 0/17
- 프론트엔드: 0/14
- 테스트: 0/5

---

## Phase 1: 인프라 및 프로젝트 설정

### INFRA-001: 모노레포 구조 설정
> 우선순위: P0 | 예상 소요: 1시간 | 선행 태스크: 없음

모노레포 저장소를 먼저 생성한 뒤 그 안에 각 프로젝트를 배치한다. (검토 M-1 반영: 순서 역전 수정)

- [x] 루트 프로젝트 디렉토리 생성 (GitHub 저장소 초기화)
- [x] `frontend/` 디렉토리 구조 준비
- [x] `backend/` 디렉토리 구조 준비
- [x] 공통 설정 파일 (`.gitignore`, `.editorconfig`)
- [ ] 초기 커밋

### INFRA-002: Spring Boot 프로젝트 생성 및 기본 설정
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: INFRA-001

Spring Initializr로 `backend/` 디렉토리에 프로젝트 생성.

- [ ] Spring Boot 3.4.x 프로젝트 생성 (Java 21 LTS, Gradle Kotlin DSL)
- [ ] 의존성 포함: Spring Web, Spring Security, Spring Data JPA, Validation, Flyway
- [ ] 추가 의존성: PostgreSQL 드라이버, AWS SDK for Java v2 (`s3`), `jjwt`, SpringDoc OpenAPI 3
- [ ] `nanoid` 외부 라이브러리는 미포함 (직접 구현 — 설계 문서 3.3절 결정)
- [ ] `spring.servlet.multipart.max-request-size=10MB` 설정 (설계 문서 4.3절)
- [ ] 기본 빌드 및 실행 확인

### INFRA-003: Next.js 15 프로젝트 생성 및 기본 설정
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: INFRA-001

`create-next-app`으로 `frontend/` 디렉토리에 프로젝트 생성.

- [ ] Next.js 15 App Router + TypeScript 프로젝트 생성
- [ ] Tailwind CSS 설정
- [ ] shadcn/ui 초기화 및 기본 컴포넌트 설치 (Button, Dialog, Input, Card, Table, DropdownMenu, Toast)
- [ ] Zustand 설치
- [ ] `@monaco-editor/react` 설치
- [ ] `unified` + `remark-parse` + `remark-rehype` + `rehype-stringify` + `rehype-sanitize` 설치
- [ ] 기본 빌드 및 실행 확인

### INFRA-004: 로컬 개발 환경 구성 (Docker Compose)
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: INFRA-001

모든 인프라를 로컬에서 실행한다. 클라우드 서비스(Supabase, R2) 대신 로컬 대체를 사용한다.

- [ ] `docker-compose.yml` 작성 (PostgreSQL + MinIO)
- [ ] PostgreSQL 15 로컬 컨테이너 설정
  - [ ] 포트: 5432
  - [ ] 초기 데이터베이스 `drafturl` 생성
  - [ ] 볼륨 마운트로 데이터 영속성 확보
- [ ] MinIO 컨테이너 설정 (S3 호환 로컬 스토리지 — R2 대체)
  - [ ] API 포트: 9000
  - [ ] 콘솔 포트: 9001
  - [ ] 초기 버킷 `drafturl-files` 자동 생성
  - [ ] 볼륨 마운트로 데이터 영속성 확보
- [ ] `.env.local` 환경변수 템플릿 파일 작성
  - [ ] Spring Boot 환경변수: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
  - [ ] MinIO 환경변수: `S3_ENDPOINT`, `S3_BUCKET_NAME`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`
  - [ ] JWT 환경변수: `JWT_SECRET`
  - [ ] OAuth2 환경변수: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
  - [ ] Next.js 환경변수: `NEXT_PUBLIC_API_URL=http://localhost:8080`
  - [ ] 프론트엔드 URL: `FRONTEND_URL=http://localhost:3000`
- [ ] `docker compose up` 으로 전체 인프라 기동 확인

### INFRA-005: Flyway 마이그레이션 설정
> 우선순위: P0 | 예상 소요: 2시간 | 선행 태스크: INFRA-002, INFRA-004

- [ ] `application.yml`에 데이터소스 설정 (환경변수 참조)
- [ ] `application-local.yml` 작성 (로컬 Docker PostgreSQL 연결)
- [ ] Flyway `V1__init.sql` 작성: `users`, `documents`, `storage_usage`, `refresh_tokens` 4개 테이블 + 인덱스 7개 (설계 문서 7.2 DDL 기준)
- [ ] `spring.jpa.hibernate.ddl-auto: validate` 설정
- [ ] 로컬 PostgreSQL에서 마이그레이션 실행 및 검증

### INFRA-006: MinIO 스토리지 연동 설정
> 우선순위: P0 | 예상 소요: 2시간 | 선행 태스크: INFRA-002, INFRA-004

MinIO는 S3 호환 API를 제공하므로 AWS SDK로 동일하게 접근한다. 추후 프로덕션에서 Cloudflare R2로 엔드포인트만 변경하면 된다.

- [ ] Spring Boot에서 `StorageConfig.java` 작성: AWS SDK for Java v2 `S3Client` 빈 설정
  - [ ] 엔드포인트를 환경변수(`S3_ENDPOINT`)로 설정 (로컬: MinIO, 프로덕션: R2)
  - [ ] 버킷 이름, Access Key, Secret Key 환경변수화
- [ ] MinIO에 PutObject, GetObject, DeleteObject 동작 검증
- [ ] CORS 설정 (Spring Boot): `http://localhost:3000` 허용

### INFRA-007: Spring Boot 공통 응답 래퍼 및 글로벌 예외 핸들러
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: INFRA-002

- [ ] `ApiResponse<T>` 공통 응답 래퍼 클래스 (`{success: true, data: T}` / `{success: false, error: {code, message}}`)
- [ ] `ErrorResponse` DTO
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`) 구현
- [ ] 비즈니스 예외 클래스 계층:
  - [ ] `BusinessException` 기본 클래스
  - [ ] `DocumentNotFoundException` (404)
  - [ ] `DocumentExpiredException` (410)
  - [ ] `ForbiddenException` (403)
  - [ ] `ContentTooLargeException` (413)
  - [ ] `RateLimitExceededException` (429)
- [ ] HTTP 상태 코드 매핑 (설계 문서 4.5 에러 코드 총괄 기준)

### INFRA-008: Cloudflare Tunnel 설정
> 우선순위: P1 | 예상 소요: 2시간 | 선행 태스크: INFRA-002, INFRA-003

로컬 서버를 외부에서 접근 가능하게 설정한다.

- [ ] `cloudflared` 설치 (`brew install cloudflared`)
- [ ] Cloudflare 계정 인증 (`cloudflared tunnel login`)
- [ ] 터널 생성 (`cloudflared tunnel create drafturl-dev`)
- [ ] 설정 파일 작성 (`config.yml`)
  - [ ] 로컬 Spring Boot (`localhost:8080`) → `api.{터널도메인}` 매핑
  - [ ] 로컬 Next.js (`localhost:3000`) → `{터널도메인}` 매핑
- [ ] DNS 레코드 등록 및 터널 실행 확인
- [ ] 외부에서 접근 가능 여부 검증

### INFRA-009: GitHub Actions CI/CD 파이프라인 설정
> 우선순위: P1 | 예상 소요: 3시간 | 선행 태스크: INFRA-001

검토 M-6 반영: CI/CD 파이프라인을 별도 태스크로 정의.

- [ ] `.github/workflows/backend-ci.yml` 작성
  - [ ] PR 시 Gradle 빌드 + 테스트 자동 실행
  - [ ] Java 21 설정
- [ ] `.github/workflows/frontend-ci.yml` 작성
  - [ ] PR 시 Next.js 빌드 + lint 자동 실행
  - [ ] Node.js 설정
- [ ] (선택) 배포 워크플로우는 클라우드 배포 결정 시 추가

---

## Phase 2: 백엔드 핵심 (P0 — 비로그인 문서 생성 + 서빙)

### BE-001: JPA 엔티티 + Repository 클래스
> 우선순위: P0 | 예상 소요: 4시간 | 선행 태스크: INFRA-005

- [ ] 4개 JPA 엔티티 구현:
  - [ ] `UserEntity` (id, email, name, profileImage, provider, providerId, plan, createdAt, updatedAt)
  - [ ] `DocumentEntity` (id, slug, userId, title, docType, r2Key, contentSize, status, expiresAt, createdAt, updatedAt)
  - [ ] `StorageUsageEntity` (userId, totalBytes, documentCount, updatedAt)
  - [ ] `RefreshTokenEntity`
- [ ] 3개 Enum 클래스:
  - [ ] `DocType` (HTML, MARKDOWN)
  - [ ] `DocumentStatus` (PENDING, ACTIVE, EXPIRED, DELETED)
  - [ ] `PlanType` (FREE, STARTER, PRO)
- [ ] 4개 Repository 인터페이스 (Spring Data JPA):
  - [ ] `UserRepository` (findByProviderAndProviderId)
  - [ ] `DocumentRepository` (스케줄러용 커스텀 쿼리 메서드: 만료/PENDING 문서 조회)
  - [ ] `StorageUsageRepository`
  - [ ] `RefreshTokenRepository`

### BE-002: SlugGenerator (nanoid 생성기)
> 우선순위: P0 | 예상 소요: 1시간 | 선행 태스크: BE-001

- [ ] `SecureRandom` + 커스텀 알파벳 `[A-Za-z0-9]` 기반 8자리 ID 직접 구현 (외부 의존성 없음)
- [ ] 충돌 시 최대 3회 재시도 (DB 조회로 유일성 확인)
- [ ] `SlugGenerator` Spring Bean 등록

### BE-003: StorageService (파일 저장/조회/삭제)
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: INFRA-006

로컬 MinIO를 통해 S3 호환 API로 구현. 프로덕션에서 R2로 엔드포인트만 교체.

- [ ] `upload(documentId, content, docType)`: `documents/{documentId}/content.{ext}` 키로 PutObject
- [ ] `download(r2Key)`: GetObject로 파일 내용을 String으로 반환
- [ ] `delete(r2Key)`: DeleteObject
- [ ] 에러 시 `StorageException` throw
- [ ] 키 네이밍은 설계 문서 8.2 규칙 준수

### BE-004: HtmlSanitizer (HTML 새니타이징)
> 우선순위: P0 | 예상 소요: 2시간 | 선행 태스크: INFRA-002

최소한의 HTML 새니타이징. LLM 생성 HTML 호환성 우선, sandbox iframe으로 안전성 확보.

- [ ] 제거 대상: `<iframe>`, `<object>`, `<embed>` 태그
- [ ] 제거 대상: `javascript:` URL 스킴 (`href="javascript:..."` 패턴)
- [ ] 나머지 그대로 유지 (`<script>`, `<style>`, 인라인 이벤트 허용)
- [ ] Jsoup 또는 정규식 기반 구현
- [ ] 단위 테스트 (다양한 LLM HTML 출력 샘플)

### BE-005: 문서 생성 API (POST /api/v1/documents)
> 우선순위: P0 | 예상 소요: 6시간 | 선행 태스크: BE-001, BE-002, BE-003, BE-004, INFRA-007

핵심 P0 기능. 2단계 커밋 패턴(PENDING -> ACTIVE).

- [ ] `CreateDocumentRequest` DTO: `content` (@NotBlank, @Size(max=5MB)), `type` (@Pattern "html|markdown"), `title` (nullable)
- [ ] `DocumentService.createDocument()` 구현:
  - [ ] Bean Validation
  - [ ] JWT에서 userId 추출 (nullable — 비로그인 허용)
  - [ ] nanoid slug 생성 (BE-002)
  - [ ] HTML이면 새니타이징 (BE-004)
  - [ ] DB INSERT (status=PENDING)
  - [ ] MinIO 업로드 (BE-003)
  - [ ] DB UPDATE (status=ACTIVE) + StorageUsage UPDATE (@Transactional)
  - [ ] 비로그인 시 `expiresAt = now + 24h`
- [ ] `DocumentController.createDocument()` 엔드포인트
- [ ] `DocumentResponse` DTO로 201 Created 응답

### BE-006: 문서 서빙 API (GET /api/v1/documents/{slug}/view)
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: BE-005

인증 불필요. 공유 URL 접근 시 프론트엔드가 호출.

- [ ] `DocumentService.getDocumentForView()` 구현:
  - [ ] slug로 문서 조회
  - [ ] 상태 확인 (ACTIVE만 서빙)
  - [ ] 만료 시간 확인 (expiresAt < now → EXPIRED로 업데이트 후 410)
  - [ ] MinIO에서 파일 로드
- [ ] `DocumentViewResponse` DTO (id, title, docType, content, createdAt)
- [ ] 응답 헤더: `Cache-Control: public, max-age=300, s-maxage=3600, stale-while-revalidate=86400`
- [ ] 에러: 404 `DOCUMENT_NOT_FOUND`, 410 `DOCUMENT_EXPIRED`/`DOCUMENT_GONE`

### BE-007: Spring Security 설정 (JWT 인증 필터 + CORS)
> 우선순위: P0 | 예상 소요: 6시간 | 선행 태스크: INFRA-002, INFRA-007

- [ ] `JwtProvider.java`:
  - [ ] JWT 생성 (HS256, Access Token 1시간, Refresh Token 7일)
  - [ ] JWT 검증 (서명 + 만료)
  - [ ] Claims 추출 (sub=userId, email, plan)
- [ ] `JwtAuthenticationFilter.java` (`OncePerRequestFilter`):
  - [ ] Authorization 헤더에서 Bearer 토큰 추출
  - [ ] 검증 후 `SecurityContext`에 `UserPrincipal` 설정
  - [ ] 유효하지 않으면 필터 체인 계속 (익명 요청)
- [ ] `SecurityConfig.java` (`SecurityFilterChain`):
  - [ ] CORS 설정 (개발: `http://localhost:3000`, 프로덕션: `https://{서비스도메인}`)
  - [ ] 경로별 인증 규칙 (설계 문서 4.2절 기준)
- [ ] `UserPrincipal` 클래스

---

## Phase 3: 백엔드 인증 + 문서 관리 (P1)

### BE-008: OAuth2 인증 API (콜백 + JWT 발급)
> 우선순위: P1 | 예상 소요: 6시간 | 선행 태스크: BE-007, BE-001

- [ ] `AuthService.java` 구현:
  - [ ] OAuth2 인가 코드로 Provider(Google/GitHub)에 토큰 교환 요청
  - [ ] 사용자 정보 조회
  - [ ] DB에 사용자 생성/조회 (provider + providerId 기준)
  - [ ] JWT Access Token + Refresh Token 발급
- [ ] `OAuthCallbackRequest` DTO: `code`, `redirectUri`, `state` (state null/빈값 거부)
- [ ] `AuthController.java`: `POST /api/v1/auth/oauth2/callback/{provider}`
- [ ] `AuthResponse` DTO (accessToken, refreshToken, expiresIn, user)
- [ ] Google OAuth2 설정
- [ ] GitHub OAuth2 설정

### BE-009: JWT 리프레시 토큰 관리 (갱신 + 로그아웃)
> 우선순위: P1 | 예상 소요: 4시간 | 선행 태스크: BE-008

- [ ] `POST /api/v1/auth/refresh`: Refresh Token Rotation
  - [ ] DB에서 토큰 조회
  - [ ] is_revoked이면 탈취 감지 (해당 user_id 전체 토큰 무효화 + 401)
  - [ ] 만료 확인
  - [ ] 기존 토큰 is_revoked=true + 새 토큰 생성 (@Transactional)
  - [ ] 새 Access Token + Refresh Token 응답
- [ ] `POST /api/v1/auth/logout`: Refresh Token 무효화
- [ ] `GET /api/v1/auth/me`: userId로 사용자 정보 + 사용량(실시간 COUNT 쿼리) 조회

### BE-010: 내 문서 목록 API (GET /api/v1/documents)
> 우선순위: P1 | 예상 소요: 3시간 | 선행 태스크: BE-005, BE-007

- [ ] `DocumentService.getMyDocuments()` 구현 (인증 필수)
- [ ] 쿼리 파라미터: `page` (0-based), `size` (기본 20, 최대 50), `sort` (기본 `updatedAt,desc`)
- [ ] Spring Data JPA `Pageable` 활용, userId + status=ACTIVE 기준 조회
- [ ] `DocumentListResponse` DTO (documents 배열 + pagination)
- [ ] `idx_documents_user_status_updated` 복합 인덱스 활용

### BE-011: 문서 상세 조회 API (GET /api/v1/documents/{slug})
> 우선순위: P1 | 예상 소요: 2시간 | 선행 태스크: BE-005, BE-007

- [ ] `DocumentService.getDocumentDetail()` 구현 (인증 필수, 소유자만 403)
- [ ] MinIO에서 원본 콘텐츠 로드 포함
- [ ] `DocumentResponse` DTO에 `content` 필드 포함

### BE-012: 문서 수정 API (PUT /api/v1/documents/{slug}) + 캐시 무효화
> 우선순위: P1 | 예상 소요: 4시간 | 선행 태스크: BE-005, BE-007

검토 C-2 반영: CDN 캐시 무효화 단계 추가.

- [ ] `UpdateDocumentRequest` DTO: `content` (nullable), `title` (nullable). 최소 하나 필수.
- [ ] 타입 변경 불허
- [ ] content 변경 시: HTML 새니타이징 → MinIO 파일 덮어쓰기 → DB UPDATE (contentSize, updatedAt) + StorageUsage UPDATE (@Transactional)
- [ ] title만 변경 시: DB UPDATE만
- [ ] 캐시 무효화 로직 (프로덕션 대비 구현, 로컬에서는 skip)
  - [ ] Cloudflare Cache Purge API 호출 유틸 클래스 작성
  - [ ] 환경변수: `CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_API_TOKEN` (로컬에서는 빈 값)
  - [ ] 캐시 무효화 실패 시 로그만 남기고 수정 트랜잭션은 성공 처리

### BE-013: 문서 삭제 API (DELETE /api/v1/documents/{slug})
> 우선순위: P1 | 예상 소요: 2시간 | 선행 태스크: BE-005, BE-007

- [ ] `DocumentService.deleteDocument()` 구현 (인증 필수, 소유자만)
- [ ] Soft delete:
  - [ ] MinIO 파일 삭제
  - [ ] DB status='DELETED' + updatedAt=now
  - [ ] StorageUsage: totalBytes 감소, documentCount 감소 (@Transactional)
- [ ] 응답: `{id, slug, deletedAt}`

### BE-014: StorageUsageService (사용량 추적)
> 우선순위: P1 | 예상 소요: 2시간 | 선행 태스크: BE-001, BE-005

검토 C-1 반영: 선행 태스크에 BE-005 추가. BE-005 구현 시점에는 StorageUsage 업데이트를 stub으로 두고, BE-014 완료 후 연동.

- [ ] `StorageUsageService.java` 구현
- [ ] 문서 생성/수정/삭제 시 `storage_usage` 테이블의 `total_bytes`, `document_count` 동기 업데이트
- [ ] 사용자 최초 문서 생성 시 `storage_usage` 레코드 자동 생성 (UPSERT)
- [ ] `GET /api/v1/auth/me` 응답에 사용량 정보 포함 (실시간 COUNT 쿼리 사용)
- [ ] BE-005, BE-012, BE-013의 StorageUsage 연동 완료

### BE-015: 만료 문서 정리 스케줄러
> 우선순위: P1 | 예상 소요: 3시간 | 선행 태스크: BE-001, BE-003

- [ ] `DocumentCleanupScheduler.java` (`@Scheduled(fixedRate=3600000)`, 매 1시간)
- [ ] 3가지 정리 작업 (각 독립 실행):
  - [ ] PENDING 5분 초과 문서: MinIO 파일 삭제 시도 + DB DELETE (LIMIT 100)
  - [ ] 만료된 ACTIVE 문서 (expires_at < now): MinIO 파일 삭제 + status=EXPIRED (LIMIT 100)
    - [ ] 로그인 사용자(`user_id IS NOT NULL`) 문서의 경우 `storage_usage.document_count` 감소 처리
  - [ ] 30일 이상 EXPIRED/DELETED 문서: DB 물리 삭제 (LIMIT 100)
- [ ] MinIO 삭제 실패 시 로그 남기고 다음 주기에 재시도

### BE-016: Rate Limiting 필터
> 우선순위: P1 | 예상 소요: 3시간 | 선행 태스크: BE-007

검토 M-3 반영: P2에서 P1로 격상. 비로그인 배포 기능의 악용 방지를 위해 MVP에 반드시 포함.

- [ ] `RateLimitFilter.java` (`OncePerRequestFilter`)
- [ ] 토큰 버킷 알고리즘, 인메모리 `ConcurrentHashMap<String, TokenBucket>` 저장
- [ ] 비로그인: IP 기반 분당 10회
- [ ] 로그인 사용자: userId 기반 분당 30회
- [ ] 비로그인 일일 문서 생성 한도: IP당 20개 (별도 카운터, DB 또는 인메모리)
- [ ] 초과 시 429 응답 + `Retry-After` 헤더
- [ ] 오래된 버킷 주기적 정리 (1시간마다)
- [ ] Spring Security 필터 체인에서 `JwtAuthenticationFilter` 이후 배치

### ~~BE-017: 소유권 이전~~ (MVP 제외 — Phase 2로 이동)

> 사용자 결정: 문서 공유가 최우선. 소유권 이전은 Phase 2에서 구현.
> 설계 문서(mvp-service-design.md 6.6절) 결정과 일치.

### BE-017: 캐시 무효화 유틸리티 서비스
> 우선순위: P1 | 예상 소요: 1시간 | 선행 태스크: INFRA-002

검토 C-2 반영: Cloudflare Cache Purge API 연동 준비. 로컬에서는 no-op.

- [ ] `CacheInvalidationService.java` 인터페이스 정의
- [ ] `CloudflareCacheInvalidationService` 구현 (프로덕션용)
  - [ ] Cloudflare Zone API로 특정 URL 캐시 purge
  - [ ] 환경변수: `CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_API_TOKEN`
- [ ] `NoOpCacheInvalidationService` 구현 (로컬 개발용)
- [ ] `@Profile` 어노테이션으로 환경별 빈 선택

---

## Phase 4: 프론트엔드

### FE-001: API 클라이언트 레이어
> 우선순위: P0 | 예상 소요: 6시간 | 선행 태스크: INFRA-003

- [ ] `lib/api/client.ts`: fetch 래퍼 구현
  - [ ] `NEXT_PUBLIC_API_URL` 환경변수에서 baseURL 설정
  - [ ] 공통 헤더 (`Content-Type: application/json`)
  - [ ] 인증 요청 시 쿠키에서 Access Token 읽어 `Authorization: Bearer` 헤더 자동 추가
  - [ ] 401 응답 시 Refresh Token 자동 갱신, 실패 시 `/auth/login`으로 리다이렉트
  - [ ] 공통 응답 파싱 (`{success, data/error}`), 에러 시 구조화된 에러 throw
- [ ] `lib/api/types.ts`: API 응답 타입 정의
- [ ] `lib/api/documents.ts`: `createDocument()`, `getMyDocuments()`, `getDocumentDetail()`, `updateDocument()`, `deleteDocument()`, `getDocumentForView()`
- [ ] `lib/api/auth.ts`: `oauthCallback()`, `refreshToken()`, `getMe()`, `logout()`

### FE-002: 공통 레이아웃 및 헤더
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: INFRA-003

- [ ] `app/layout.tsx`: 루트 레이아웃 (글로벌 스타일, 폰트, 메타데이터)
- [ ] `components/layout/Header.tsx`:
  - [ ] 서비스 로고
  - [ ] 비로그인: [로그인] 버튼
  - [ ] 로그인: 사용자 프로필 드롭다운 [내 문서] [로그아웃]
- [ ] 반응형 모바일 메뉴

### FE-003: Monaco Editor 패널
> 우선순위: P0 | 예상 소요: 4시간 | 선행 태스크: INFRA-003

- [ ] `components/editor/EditorPanel.tsx`: Monaco Editor 래퍼 컴포넌트
- [ ] 동적 import (`next/dynamic`, SSR 비활성화)
- [ ] HTML/Markdown 탭 전환 UI
- [ ] 에디터 내용 변경 시 `onChange` 콜백
- [ ] 에디터 테마 (기본 vs-dark 또는 light)
- [ ] 반응형 레이아웃

### FE-004: 실시간 미리보기 패널
> 우선순위: P0 | 예상 소요: 4시간 | 선행 태스크: INFRA-003

- [ ] `components/editor/PreviewPanel.tsx`
- [ ] HTML 미리보기: `<iframe srcdoc={content}>` (클라이언트 미리보기)
- [ ] Markdown 미리보기: `unified` + `remark-parse` + `remark-rehype` + `rehype-stringify`로 HTML 변환
- [ ] debounce 300ms 적용
- [ ] 에디터-미리보기 좌우 분할 레이아웃 (50:50)

### FE-005: 파일 드래그 앤 드롭
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: FE-003

- [ ] `components/editor/FileDropZone.tsx`: 드래그 앤 드롭 영역 + 파일 선택 버튼
- [ ] 클라이언트 사전 검증: 확장자 (.html, .htm, .md, .markdown), 파일 크기 5MB 이하
- [ ] `FileReader.readAsText()`로 파일 내용 읽기
- [ ] 읽은 내용을 에디터에 반영 + docType 자동 감지 (확장자 기반)
- [ ] 드래그 오버 시 시각적 피드백 (하이라이트)

### FE-006: 메인 페이지 (랜딩 + 에디터)
> 우선순위: P0 | 예상 소요: 5시간 | 선행 태스크: FE-001, FE-003, FE-004, FE-005

- [ ] `app/page.tsx`: 서비스 핵심 진입점
- [ ] EditorPanel + PreviewPanel 좌우 배치
- [ ] FileDropZone 통합
- [ ] HTML/MD 타입 선택 UI
- [ ] "공유하기" 버튼 (Cmd/Ctrl+Enter 단축키)
- [ ] 비로그인 안내 문구 ("24시간 후 만료, 로그인하면 영구 보존")
- [ ] 공유하기 클릭 → `POST /api/v1/documents` 호출
- [ ] 결과 모달 (`PublishResultModal`: URL 표시 + 복사 버튼 + "상세 보기" 링크)
- [ ] 비로그인 안내: "로그인하면 영구 보관" 문구 표시

### FE-007: 문서 뷰어 페이지 (공유 URL)
> 우선순위: P0 | 예상 소요: 5시간 | 선행 태스크: FE-001, FE-002

- [ ] `app/[slug]/page.tsx`: Server Component에서 `GET /api/v1/documents/{slug}/view` 호출
- [ ] 200 + HTML: `HtmlViewer` (sandbox iframe `sandbox="allow-scripts"`, `srcdoc`)
- [ ] 200 + MD: `MarkdownViewer` (unified 변환 + 테마 적용)
- [ ] 상단 바: 서비스 로고 + "나도 만들어보기" 링크 + "URL 복사" 버튼
- [ ] 404: `notFound()` 호출
- [ ] 410: 만료 안내 UI 인라인 렌더링 (slug 정보 유지, FE-012의 별도 만료 페이지와 역할 구분)
- [ ] `components/viewer/HtmlViewer.tsx`, `components/viewer/MarkdownViewer.tsx`

### FE-008: OAuth2 로그인 페이지 + 콜백 처리
> 우선순위: P1 | 예상 소요: 5시간 | 선행 태스크: FE-001, FE-002

- [ ] `app/auth/login/page.tsx`:
  - [ ] Google 로그인 버튼, GitHub 로그인 버튼
  - [ ] 클릭 시 `crypto.randomUUID()`로 state 생성
  - [ ] `oauth_state` httpOnly 쿠키 저장 (SameSite=Lax, Secure, Max-Age=300)
  - [ ] OAuth2 Provider로 리다이렉트
- [ ] `app/auth/callback/page.tsx`:
  - [ ] URL에서 code, state 추출
  - [ ] 쿠키의 oauth_state와 비교 검증 (불일치 시 `/auth/error` 리다이렉트)
  - [ ] `POST /api/v1/auth/oauth2/callback/{provider}` 호출
  - [ ] accessToken, refreshToken을 httpOnly 쿠키에 저장
  - [ ] 대시보드로 리다이렉트
- [ ] `app/auth/error/page.tsx`: 인증 에러 안내

### FE-009: Next.js 미들웨어 (인증 가드 + 토큰 관리)
> 우선순위: P1 | 예상 소요: 3시간 | 선행 태스크: FE-001, FE-008

검토 M-2 반영: 선행 태스크에 FE-008 추가. JWT 쿠키 저장 로직이 FE-008에서 구현되므로 테스트를 위해 선행 필요.

- [ ] `middleware.ts`: `/dashboard/*` 경로에 대해 JWT 쿠키 존재 여부 확인, 미인증 시 `/auth/login` 리다이렉트
- [ ] `lib/auth/token.ts`: JWT 토큰 쿠키 읽기/쓰기/삭제 유틸
  - [ ] Access Token 쿠키: httpOnly, SameSite=Lax, Secure
  - [ ] Refresh Token 쿠키: httpOnly, SameSite=Lax, Secure, Path=/api/v1/auth/refresh

### FE-010: 대시보드 페이지 (내 문서 목록)
> 우선순위: P1 | 예상 소요: 6시간 | 선행 태스크: FE-001, FE-002, FE-009

- [ ] `app/dashboard/layout.tsx`: 인증 가드 (middleware에서 처리)
- [ ] `app/dashboard/page.tsx`: `GET /api/v1/documents` 호출
- [ ] `DocumentList` 컴포넌트: 테이블 (제목, 유형, 크기, 수정일)
  - [ ] 각 문서 행에 [링크 복사], [보기], [편집], [삭제] 액션
  - [ ] 삭제 시 확인 다이얼로그 (`DeleteConfirmDialog`)
- [ ] 페이지네이션 UI
- [ ] [+ 새 문서] 버튼 (메인 페이지로 이동)
- [ ] 사용량 표시 (`StorageUsageBar`: N개 문서, XKB / 5MB)
  - [ ] `GET /api/v1/auth/me`로 사용량 정보 조회

### FE-011: 문서 편집 페이지
> 우선순위: P1 | 예상 소요: 4시간 | 선행 태스크: FE-003, FE-004, FE-006, FE-010

- [ ] `app/dashboard/[slug]/edit/page.tsx`
- [ ] `GET /api/v1/documents/{slug}`로 기존 문서 내용 로드
- [ ] EditorPanel에 기존 content 표시
- [ ] PreviewPanel로 실시간 미리보기
- [ ] "업데이트" 버튼 → `PUT /api/v1/documents/{slug}` 호출
- [ ] 성공 시 토스트 + 대시보드로 이동
- [ ] 타입(docType) 읽기 전용 표시 (변경 불가)

### FE-012: 에러/만료/404 페이지
> 우선순위: P1 | 예상 소요: 2시간 | 선행 태스크: FE-002

- [ ] `app/not-found.tsx`: 404 페이지
- [ ] `app/error.tsx`: 글로벌 에러 페이지
- [ ] `app/expired/page.tsx`: 만료 문서 안내 페이지 (직접 URL 접근용, slug 쿼리 파라미터 지원)
- [ ] 각 페이지에 메인 페이지로 돌아가기 링크
- [ ] 서비스 브랜딩 유지

### FE-013: 공유 완료 페이지
> 우선순위: P1 | 예상 소요: 4시간 | 선행 태스크: FE-001, FE-002

검토 M-5 반영: domain-analysis.md의 shared.html에 해당하는 독립 페이지. 바이럴 성장 요소 포함.

- [ ] `app/shared/[slug]/page.tsx`
- [ ] 공유 URL 복사 버튼
- [ ] 소셜 공유 버튼:
  - [ ] Twitter/X 공유
  - [ ] 이메일 공유 (mailto:)
  - [ ] Slack 공유
- [ ] 비로그인 문서인 경우 만료 카운트다운 타이머 (expiresAt 기반)
- [ ] "문서 보기" 버튼 (뷰어 페이지로 이동)
- [ ] "새 문서 만들기" 버튼 (메인 페이지로 이동)
- [ ] 비로그인 시 "무료 가입하기" CTA 배너

### FE-014: 공유 완료 페이지를 위한 문서 메타 API 연동
> 우선순위: P1 | 예상 소요: 1시간 | 선행 태스크: FE-013, BE-006

공유 완료 페이지에서 문서 메타 정보(제목, 타입, 크기, 만료 시간)를 표시하기 위해 기존 view API의 응답을 활용.

- [ ] `GET /api/v1/documents/{slug}/view` 호출로 문서 정보 조회
- [ ] 만료 카운트다운 계산 로직 (클라이언트 타이머)
- [ ] FE-006의 `PublishResultModal`에서 "상세 보기" 링크를 `/shared/{slug}`로 연결

---

## Phase 5: 통합 테스트 및 QA

### TEST-001: 프론트엔드-백엔드 통합 테스트
> 우선순위: P0 | 예상 소요: 6시간 | 선행 태스크: BE-016, FE-013 (전체 기능 완료 후)

- [ ] 핵심 플로우 E2E 검증:
  - [ ] 비로그인 문서 생성 → URL 접근 → 렌더링 확인
  - [ ] 공유 완료 페이지 표시 확인
  - [ ] 로그인 → 문서 생성 → 대시보드 확인 → 수정 → 삭제
  - [ ] 만료 문서 접근 시 만료 안내 표시
  - [ ] Rate Limiting 동작 확인
- [ ] CORS 설정 검증 (Origin 허용/거부)
- [ ] 인증 플로우 전체 검증 (OAuth2 → JWT → API 호출)

### TEST-002: 로컬 전체 환경 구동 테스트
> 우선순위: P0 | 예상 소요: 3시간 | 선행 태스크: TEST-001

- [ ] `docker compose up`으로 PostgreSQL + MinIO 기동
- [ ] Spring Boot 로컬 실행 (`./gradlew bootRun --args='--spring.profiles.active=local'`)
- [ ] Next.js 로컬 실행 (`npm run dev`)
- [ ] 전체 서비스 플로우 수동 검증
- [ ] Cloudflare Tunnel로 외부 접근 테스트 (INFRA-008 연동)

### TEST-003: 모바일 반응형 점검 및 최종 QA
> 우선순위: P1 | 예상 소요: 4시간 | 선행 태스크: TEST-002

- [ ] 에디터 페이지 모바일 레이아웃 (에디터/미리보기 탭 전환 또는 상하 배치)
- [ ] 대시보드 모바일 레이아웃
- [ ] 문서 뷰어 모바일 렌더링
- [ ] 공유 완료 페이지 모바일 레이아웃
- [ ] 다양한 LLM 출력 HTML 샘플로 서빙 테스트
- [ ] 성능 측정 (서빙 응답 시간 < 800ms 목표)
- [ ] 에러 페이지 동작 확인

---

## Phase 6 (후순위): 클라우드 배포

> 아래 태스크는 로컬 개발 환경에서 기능이 검증된 후, 프로덕션 배포가 필요할 때 진행한다.

### DEPLOY-001: 백엔드 클라우드 배포 (Railway)
> 우선순위: P2 | 예상 소요: 4시간 | 선행 태스크: TEST-002

- [ ] `backend/Dockerfile` 작성 (eclipse-temurin:21-jre-alpine 기반)
- [ ] Railway Hobby 플랜 설정
- [ ] 환경변수 설정 (DATABASE_URL, S3_*, JWT_SECRET, OAuth2 자격증명, FRONTEND_URL)
- [ ] 헬스체크 엔드포인트 확인
- [ ] GitHub Actions `backend-deploy.yml` 연동

### DEPLOY-002: 프론트엔드 클라우드 배포 (Vercel)
> 우선순위: P2 | 예상 소요: 2시간 | 선행 태스크: TEST-002

- [ ] Vercel에 GitHub 저장소 연동 (루트 디렉토리: `frontend/`)
- [ ] 환경변수 설정 (`NEXT_PUBLIC_API_URL` = 백엔드 배포 URL)
- [ ] 빌드 설정 확인

### DEPLOY-003: 도메인 설정 및 HTTPS
> 우선순위: P2 | 예상 소요: 3시간 | 선행 태스크: DEPLOY-001, DEPLOY-002

- [ ] Cloudflare DNS에서 도메인 설정
- [ ] 프론트엔드 도메인 → Vercel CNAME
- [ ] 백엔드 API 도메인 (`api.{서비스도메인}`) → Railway CNAME
- [ ] SSL/HTTPS 자동 설정 확인
- [ ] CORS 설정을 프로덕션 도메인으로 업데이트
- [ ] OAuth2 Provider의 리다이렉트 URI를 프로덕션 도메인으로 업데이트

---

## 의존관계 다이어그램

```
INFRA-001 (모노레포 구조)
  |
  +---> INFRA-002 (Spring Boot) --+---> INFRA-005 (Flyway) --> BE-001 (엔티티/레포)
  |                               |                               |
  |                               +---> INFRA-006 (MinIO) -----> BE-003 (StorageService)
  |                               |                               |
  |                               +---> INFRA-007 (공통 응답/예외) |
  |                               |                               |
  |                               +---> BE-004 (HtmlSanitizer)   |
  |                               |                               |
  |                               +---> BE-007 (Security/JWT) ---+
  |                                      |                       |
  |                                      +-> BE-008 (OAuth2)     |
  |                                      |    |                  |
  |                                      |    +-> BE-009 (Refresh)|
  |                                      |    |                  |
  |                                      |    +-> BE-017 (소유권이전)
  |                                      |                       |
  |                                      +-> BE-016 (RateLimit)  |
  |                                                              |
  |     BE-002 (Slug) <--- BE-001                                |
  |          |                                                   |
  |          +---+---+---+-------+-------------------------------+
  |              |
  |              v
  |         BE-005 (문서 생성 API) --- P0 핵심
  |              |
  |              +---> BE-006 (문서 서빙 API) --- P0 핵심
  |              +---> BE-010 (문서 목록 API)
  |              +---> BE-011 (문서 상세 API)
  |              +---> BE-012 (문서 수정 + 캐시 무효화)
  |              +---> BE-013 (문서 삭제 API)
  |              +---> BE-014 (StorageUsage)
  |
  |     BE-001 + BE-003 --> BE-015 (스케줄러)
  |     INFRA-002 ---------> BE-018 (캐시 무효화 유틸)
  |
  +---> INFRA-003 (Next.js)
  |       |
  |       +---> FE-001 (API 클라이언트)
  |       |       |
  |       |       +---> FE-006 (메인 페이지) --- P0
  |       |       +---> FE-007 (뷰어 페이지) --- P0
  |       |       +---> FE-008 (OAuth2 로그인)
  |       |       |       |
  |       |       |       +-> FE-009 (미들웨어)
  |       |       |              |
  |       |       |              +-> FE-010 (대시보드)
  |       |       |                    |
  |       |       |                    +-> FE-011 (편집)
  |       |       |
  |       |       +---> FE-013 (공유 완료 페이지)
  |       |               |
  |       |               +-> FE-014 (메타 API 연동)
  |       |
  |       +---> FE-002 (공통 레이아웃)
  |       +---> FE-003 (Monaco Editor)
  |       +---> FE-004 (미리보기 패널)
  |       +---> FE-005 (파일 D&D)
  |       +---> FE-012 (에러/만료 페이지)
  |
  +---> INFRA-008 (Cloudflare Tunnel)
  +---> INFRA-009 (GitHub Actions CI/CD)

통합/테스트:
  전체 --> TEST-001 (통합 테스트)
  TEST-001 --> TEST-002 (로컬 전체 환경)
  TEST-002 --> TEST-003 (반응형 + 최종 QA)
```

---

## 병렬 작업 가능 식별

### 완전 병렬 가능 (의존관계 없음)

| 그룹 | 태스크 | 비고 |
|------|--------|------|
| A | INFRA-002 + INFRA-003 | Spring Boot / Next.js 프로젝트 동시 생성 (INFRA-001 이후) |
| B | INFRA-005 + INFRA-006 | DB 설정과 MinIO 설정 병렬 (INFRA-002 + INFRA-004 이후) |
| C | BE-004 + BE-007 | HtmlSanitizer와 Security 설정 병렬 (INFRA-002 이후) |
| D | FE-002 + FE-003 + FE-004 + FE-005 + FE-012 | 독립 UI 컴포넌트 병렬 (INFRA-003 이후) |
| E | BE-010 + BE-011 + BE-012 + BE-013 | 문서 CRUD API 병렬 (BE-005 + BE-007 이후) |

### 1인 개발 기준 추천 순서

| 시기 | 작업 |
|------|------|
| Day 1 | INFRA-001, INFRA-002, INFRA-003 |
| Day 2 | INFRA-004, INFRA-005, INFRA-006 |
| Day 3 | INFRA-007, BE-001 |
| Day 4 | BE-002, BE-004, BE-007 |
| Day 5 | BE-003, BE-005 (시작) |
| Day 6 | BE-005 (완료), BE-006 |
| Day 7 | FE-001 |
| Day 8 | FE-002, FE-003, FE-004 |
| Day 9 | FE-005, FE-006 |
| Day 10 | FE-007, FE-012 |
| Day 11 | BE-008 |
| Day 12 | BE-009, BE-014 |
| Day 13 | FE-008, FE-013 |
| Day 14 | FE-009, BE-010, BE-011 |
| Day 15 | FE-010 |
| Day 16 | BE-012, BE-013, BE-015 |
| Day 17 | FE-011, BE-016 |
| Day 18 | BE-017, BE-018, FE-014 |
| Day 19 | TEST-001 |
| Day 20 | TEST-002 |
| Day 21 | TEST-003, INFRA-008 |
| Day 22 | INFRA-009, 버퍼/버그 수정 |

---

## 우선순위별 태스크 요약

### P0 (핵심 기능 — 반드시 MVP에 포함)

| ID | 이름 | 분류 | 소요 |
|----|------|------|------|
| INFRA-001 | 모노레포 구조 설정 | Infra | 1h |
| INFRA-002 | Spring Boot 프로젝트 생성 | Infra | 3h |
| INFRA-003 | Next.js 프로젝트 생성 | Infra | 3h |
| INFRA-004 | 로컬 개발 환경 (Docker Compose) | Infra | 3h |
| INFRA-005 | Flyway 마이그레이션 | Infra | 2h |
| INFRA-006 | MinIO 스토리지 연동 | Infra | 2h |
| INFRA-007 | 공통 응답/예외 핸들러 | Infra | 3h |
| BE-001 | JPA 엔티티 + Repository | BE | 4h |
| BE-002 | SlugGenerator | BE | 1h |
| BE-003 | StorageService | BE | 3h |
| BE-004 | HtmlSanitizer | BE | 2h |
| BE-005 | 문서 생성 API | BE | 6h |
| BE-006 | 문서 서빙 API | BE | 3h |
| BE-007 | Security + JWT + CORS | BE | 6h |
| FE-001 | API 클라이언트 레이어 | FE | 6h |
| FE-002 | 공통 레이아웃/헤더 | FE | 3h |
| FE-003 | Monaco Editor 패널 | FE | 4h |
| FE-004 | 미리보기 패널 | FE | 4h |
| FE-005 | 파일 드래그 앤 드롭 | FE | 3h |
| FE-006 | 메인 페이지 | FE | 5h |
| FE-007 | 문서 뷰어 페이지 | FE | 5h |
| TEST-001 | 통합 테스트 | Test | 6h |
| TEST-002 | 로컬 전체 환경 구동 | Test | 3h |

### P1 (중요 기능 — MVP에 포함하되 P0 이후)

| ID | 이름 | 분류 | 소요 |
|----|------|------|------|
| INFRA-008 | Cloudflare Tunnel 설정 | Infra | 2h |
| INFRA-009 | GitHub Actions CI/CD | Infra | 3h |
| BE-008 | OAuth2 인증 API | BE | 6h |
| BE-009 | JWT Refresh Token | BE | 4h |
| BE-010 | 문서 목록 API | BE | 3h |
| BE-011 | 문서 상세 조회 API | BE | 2h |
| BE-012 | 문서 수정 API + 캐시 무효화 | BE | 4h |
| BE-013 | 문서 삭제 API | BE | 2h |
| BE-014 | StorageUsageService | BE | 2h |
| BE-015 | 만료 문서 스케줄러 | BE | 3h |
| BE-016 | Rate Limiting 필터 | BE | 3h |
| BE-017 | 소유권 이전 API | BE | 2h |
| BE-018 | 캐시 무효화 유틸리티 | BE | 1h |
| FE-008 | OAuth2 로그인/콜백 | FE | 5h |
| FE-009 | 미들웨어 (인증 가드) | FE | 3h |
| FE-010 | 대시보드 | FE | 6h |
| FE-011 | 문서 편집 페이지 | FE | 4h |
| FE-012 | 에러/만료/404 페이지 | FE | 2h |
| FE-013 | 공유 완료 페이지 | FE | 4h |
| FE-014 | 공유 완료 메타 연동 | FE | 1h |
| TEST-003 | 반응형 + 최종 QA | Test | 4h |

### P2 (클라우드 배포 — 로컬 검증 완료 후)

| ID | 이름 | 분류 | 소요 |
|----|------|------|------|
| DEPLOY-001 | Railway 배포 (백엔드) | Infra | 4h |
| DEPLOY-002 | Vercel 배포 (프론트엔드) | Infra | 2h |
| DEPLOY-003 | 도메인 설정 + HTTPS | Infra | 3h |

---

## 크리티컬 패스

MVP 출시까지의 최소 경로 (가장 긴 의존 체인):

```
INFRA-001 → INFRA-002 → INFRA-005 → BE-001 → BE-002 → BE-005 → BE-006
                                                            |
INFRA-004 → INFRA-006 → BE-003 ----------------------------+
INFRA-007 -------------------------------------------------+
BE-004 ----------------------------------------------------+
BE-007 --------→ BE-008 → BE-009 → BE-017
                   |
INFRA-003 → FE-001 → FE-006 → FE-007
                 |
                 +→ FE-008 → FE-009 → FE-010 → FE-011
                 |
                 +→ FE-013 → FE-014

전체 → TEST-001 → TEST-002 → TEST-003
```

**크리티컬 패스 소요 시간**: 약 20~22일 (1인 기준, 버퍼 제외)

---

## 검토 결과 반영 체크리스트

| 검토 ID | 유형 | 내용 | 반영 위치 |
|---------|------|------|-----------|
| C-1 | Critical | BE-014 선행 태스크에 BE-005 추가 | BE-014 선행 태스크 수정 |
| C-2 | Critical | CDN 캐시 무효화 태스크 추가 | BE-012에 캐시 무효화 추가 + BE-018 신규 |
| M-1 | Major | 모노레포 순서 수정 | INFRA-001을 최초 태스크로, INFRA-002/003이 의존 |
| M-2 | Major | FE-009 선행 태스크에 FE-008 추가 | FE-009 선행 태스크 수정 |
| M-3 | Major | Rate Limiting P2→P1 격상 | BE-016 P1로 변경 |
| M-4 | Major | 소유권 이전 태스크 추가 | BE-017 신규 태스크 |
| M-5 | Major | 공유 완료 페이지 태스크 추가 | FE-013, FE-014 신규 태스크 |
| M-6 | Major | CI/CD 태스크 추가 | INFRA-009 신규 태스크 |

---

## 검토 결과

> 검토일: 2026-03-21
> 검토자: 문서 검토 에이전트
> 검토 범위: 이전 검토(C-1, C-2, M-1~M-6) 반영 여부 + 설계 문서(mvp-service-design.md) 및 MVP 계획서(file-upload-storage-plan.md)와의 정합성

---

### 이전 검토 반영 결과

이전 검토에서 지적된 Critical 2건, Major 6건의 반영 여부를 검증했다.

| 검토 ID | 반영 여부 | 확인 내용 |
|---------|----------|-----------|
| C-1 (StorageUsage 의존관계) | 반영됨 | BE-014 선행 태스크에 BE-005 추가 확인. stub 처리 후 BE-014 완료 시 연동 완료 체크리스트 명시 |
| C-2 (CDN 캐시 무효화) | 반영됨 | BE-012에 캐시 무효화 서브태스크 추가, BE-018 신규 태스크로 분리. 로컬에서는 NoOpCacheInvalidationService 사용하는 @Profile 패턴 적용 |
| M-1 (모노레포 순서) | 반영됨 | INFRA-001이 최초 태스크, INFRA-002/003/004가 INFRA-001에 의존 |
| M-2 (FE-009 선행 태스크) | 반영됨 | FE-009 선행 태스크에 FE-008 추가 확인 |
| M-3 (Rate Limiting 격상) | 반영됨 | BE-016이 P1으로 변경됨 |
| M-4 (소유권 이전 태스크) | 반영됨 (단, 설계 문서와 충돌 — 아래 Critical 참조) | BE-017 신규 태스크 추가됨 |
| M-5 (공유 완료 페이지) | 반영됨 | FE-013, FE-014 신규 태스크 추가됨 |
| M-6 (CI/CD 태스크) | 반영됨 | INFRA-009 신규 태스크 추가됨 |

---

### Critical (치명적 문제)

#### C-1: BE-017(소유권 이전)이 설계 문서(mvp-service-design.md 6.6절)와 직접 충돌

**문제**: `mvp-service-design.md` 섹션 6.6은 **"MVP에서의 전략: 소유권 이전을 지원하지 않는다"** 라고 명시한다. 근거로 구현 복잡도와 24시간 만료 특성상 보존 필요성이 낮음을 든다. 그러나 이번 태스크 계획에서 이전 검토의 M-4를 반영하면서 BE-017(소유권 이전 API)을 P1 태스크로 추가했다.

이는 두 문서가 동일 기능에 대해 "구현하지 않음"과 "구현함"으로 상반되는 의사결정을 담고 있는 상태다. 구현자가 어느 문서를 따라야 하는지 명확하지 않으며, 추가로 FE-008 콜백 처리 내부에도 `POST /api/v1/documents/claim` 호출 로직이 포함되어 있어 프론트엔드에도 소유권 이전 코드가 산재한다.

**영향 범위**: BE-017 (2h), FE-008 내 claim 호출 로직 (추정 1h 이상). 설계 문서를 따르면 이 공수가 절감되고 코드 복잡도가 감소한다.

**요구 조치**: 둘 중 하나를 선택하고 나머지 문서를 동기화해야 한다.
- 옵션 A (설계 문서 유지): BE-017 태스크를 제거하고 FE-008의 claim 호출 로직을 삭제. mvp-service-design.md 6.6절의 결정을 유지하며, FE-006 메인 페이지의 "24시간 후 만료, 로그인하면 영구 보존" 안내 문구로 사용자 유도.
- 옵션 B (태스크 계획 유지): mvp-service-design.md 6.6절을 "소유권 이전을 지원한다"로 변경하고 근거와 구현 명세 추가.

---

### Major (주요 개선사항)

#### M-1: 진행 현황 카운터 불일치

**문제**: 상단 진행 현황에 "테스트: 0/6"으로 표기되어 있으나, 실제 TEST 태스크는 TEST-001, TEST-002, TEST-003의 3개뿐이다. DEPLOY 태스크(DEPLOY-001~003, 3개)가 테스트로 집계된 것으로 보인다.

실제 태스크 수: INFRA 9, BE 18, FE 14, TEST 3, DEPLOY 3 = 합계 47개 (전체 수는 일치).

**요구 조치**: 진행 현황을 아래와 같이 수정할 것.
```
- 테스트: 0/3
- 배포: 0/3
```

#### M-2: BE-012의 BE-018 의존관계 누락

**문제**: BE-012(문서 수정 API + 캐시 무효화)는 `CacheInvalidationService`를 주입받아야 한다. 그런데 BE-012의 선행 태스크가 `BE-005, BE-007`이고, BE-018은 선행 태스크 목록에 없다. BE-018이 완료되기 전에 BE-012를 구현하면 CacheInvalidationService 빈이 없어 컴파일/실행 오류가 발생한다.

의존관계 다이어그램에도 `INFRA-002 → BE-018`만 표시되어 있고 `BE-018 → BE-012` 방향 연결이 누락되어 있다.

**요구 조치**: BE-012 선행 태스크에 BE-018을 추가한다.
```
선행 태스크: BE-005, BE-007, BE-018
```

#### M-3: BE-015(스케줄러)의 BE-015 → BE-014 의존관계 누락

**문제**: BE-015(만료 문서 정리 스케줄러)는 로그인 사용자 문서 만료 처리 시 `storage_usage.document_count` 감소를 처리해야 한다. 이 로직은 BE-014(StorageUsageService)에 정의된 서비스 메서드를 호출해야 하므로 BE-015가 BE-014에 의존한다. 그러나 BE-015의 선행 태스크는 `BE-001, BE-003`이고 BE-014가 없다.

**요구 조치**: BE-015 선행 태스크에 BE-014를 추가한다.
```
선행 태스크: BE-001, BE-003, BE-014
```

#### M-4: BE-005의 StorageUsage stub 처리 방식이 BE-014 완료 전 구현 시 트랜잭션 문제 야기 가능

**문제**: BE-005 태스크 내에 `DB UPDATE (status=ACTIVE) + StorageUsage UPDATE (@Transactional)`가 단일 서브태스크로 묶여 있다. 그러나 BE-014 설명에는 "BE-005 구현 시점에는 StorageUsage 업데이트를 stub으로 두고, BE-014 완료 후 연동"이라고 명시한다. stub 처리 시 `@Transactional` 범위가 어떻게 관리되는지(stub이 no-op인지, service 호출인지, 주석 처리인지)가 BE-005 태스크에 명시되지 않아 구현자가 임의로 결정해야 한다.

**요구 조치**: BE-005의 StorageUsage 관련 서브태스크에 stub 처리 방식을 명시한다. 예: "로그인 사용자 문서 생성 시 StorageUsageService.updateOnCreate() 호출 (BE-014 완료 전까지는 no-op stub 메서드 호출로 대체)"

---

### Minor (사소한 개선사항)

#### m-1: 추천 순서 표에서 INFRA-008 및 INFRA-009 위치 재검토

Day 21에 INFRA-008(Cloudflare Tunnel)이 배치되어 있는데, INFRA-008은 로컬 외부 접근 테스트에 필요하므로 TEST-002(Day 20)보다 먼저 수행되어야 한다. TEST-002 태스크에 "Cloudflare Tunnel로 외부 접근 테스트(INFRA-008 연동)"가 포함되어 있으나 Day 21에 INFRA-008이 배치되어 있어 순서가 역전된다.

**요구 조치**: INFRA-008을 Day 19~20 사이에 배치하거나, TEST-002의 터널 연동 항목을 TEST-003으로 이동할 것.

#### m-2: FE-007 뷰어 페이지의 sandbox iframe 속성 검토 필요

FE-007에서 sandbox 속성이 `sandbox="allow-scripts"`로 명시되어 있다. 이 설정만으로는 LLM 생성 HTML에서 폼 제출, 팝업, 외부 리소스 로드 등이 차단된다. mvp-service-design.md는 "나머지 그대로 유지 (script, style, 인라인 이벤트 허용)"를 요구하므로, `allow-same-origin` 없이 `allow-scripts`만으로는 인라인 스크립트가 DOM에 접근하는 기본 API 일부가 제한될 수 있다. 실제 LLM 출력 샘플로 동작을 검증할 것.

#### m-3: 태스크 총 소요 시간 명시 부재

병렬 작업 섹션과 크리티컬 패스 섹션에 "약 20~22일"이 명시되어 있으나, P0/P1 태스크의 총 예상 공수(시간 합계)가 별도로 계산되어 있지 않다. 우선순위별 요약 표에 소요 시간이 있으므로 합계를 추가하면 일정 계획에 유용하다.

---

### 추가 고려사항

1. **BE-017(소유권 이전) 제거 시 FE-006 메인 페이지 변경 필요**: FE-006에 "비로그인 시 생성된 slug를 localStorage에 저장 (BE-017 소유권 이전용)"이 서브태스크로 있다. BE-017을 제거하기로 결정하면 이 항목도 함께 삭제되어야 한다.

2. **DEPLOY 태스크의 캐시 무효화 환경변수 설정**: BE-018의 `CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_API_TOKEN`은 로컬에서 빈 값이지만 DEPLOY-001(Railway 배포) 태스크에 해당 환경변수 설정 항목이 누락되어 있다. 프로덕션 배포 시 캐시 무효화가 동작하려면 Railway에 해당 환경변수가 설정되어야 한다.

3. **비로그인 일일 문서 생성 한도(IP당 20개)의 인메모리 저장 리셋**: BE-016 Rate Limiting 필터가 비로그인 일일 문서 생성 카운터를 인메모리에 보관하면, Spring Boot 재시작 시 카운터가 초기화된다. 로컬 개발 환경에서는 문제없지만, 프로덕션에서 악의적 사용자가 서버 재시작을 유발하거나 재시작 직후 집중 요청할 경우 한도가 우회될 수 있다. MVP 규모에서는 허용 가능한 트레이드오프이나 Phase 2에서 Redis 전환이 권장된다. 태스크 설명에 이 한계를 명시하면 Phase 2 계획 시 혼선이 줄어든다.

---

## 인프라 환경 요약

| 구성요소 | 로컬 개발 | 프로덕션 (후순위) |
|---------|----------|-----------------|
| 데이터베이스 | Docker PostgreSQL 15 | Supabase PostgreSQL |
| 파일 저장소 | Docker MinIO (S3 호환) | Cloudflare R2 |
| 백엔드 | `./gradlew bootRun` (localhost:8080) | Railway Docker |
| 프론트엔드 | `npm run dev` (localhost:3000) | Vercel |
| 외부 접근 | Cloudflare Tunnel (cloudflared) | 도메인 + CDN |
| CI/CD | GitHub Actions (빌드/테스트) | GitHub Actions (배포 포함) |
