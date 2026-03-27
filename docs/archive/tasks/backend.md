# 백엔드 태스크

> 최종 수정일: 2026-03-22
> 설계 문서: [backend/](../design/backend/)

---

## 진행 상태

| # | 태스크 | 상태 | 의존성 |
|---|--------|------|--------|
| 1 | 프로젝트 기반 설정 (Spring Boot 스캐폴딩) | ✅ | - |
| 2 | Global 계층 구현 (공통 클래스, 설정) | ✅ | #1 |
| 3 | 인증/인가 구현 (JWT + OAuth2) | ✅ | #2 |
| 4 | 인증 API 구현 | ✅ | #3 |
| 5 | R2 스토리지 구현 (Port-Adapter) | ✅ | #1 |
| 6 | 문서 생성 API 구현 (2단계 커밋) | ✅ | #2, #5 |
| 7 | 문서 서빙 API 구현 | ✅ | #6 |
| 8 | 문서 관리 API 구현 (목록/상세/수정/삭제) | ✅ | #6 |
| 9 | 만료 문서 정리 스케줄러 구현 | ✅ | #6 |
| 10 | Rate Limiting 구현 | ✅ | #2 |
| 11 | 로깅 & 모니터링 설정 | ✅ | #1 |

---

## 1. 프로젝트 기반 설정 (Spring Boot 스캐폴딩)

- [ ] Spring Boot 3.4.x 프로젝트 초기 구조 확인 (Gradle Kotlin DSL)
- [ ] 패키지 구조를 domain/infra/global 3계층으로 정리
- [ ] Gradle 의존성 설정 (Spring Web, Security, JPA, Validation, Flyway, jjwt, jsoup, AWS SDK, SpringDoc OpenAPI)
- [ ] application.yml / application-local.yml / application-prod.yml 프로파일 분리
- [ ] Flyway 마이그레이션 V1__init.sql 작성 (users, documents, storage_usage, refresh_tokens)
- [ ] 로컬 Docker Compose 환경 구성 (PostgreSQL, MinIO, Spring Boot, Next.js)

## 2. Global 계층 구현 (공통 클래스, 설정)

- [ ] BaseEntity.java (createdAt, updatedAt JPA Auditing)
- [ ] JpaConfig.java (@EnableJpaAuditing)
- [ ] ApiResponse.java (공통 응답 래퍼 {success, data/error})
- [ ] ErrorResponse.java (에러 응답 DTO)
- [ ] BusinessException.java (비즈니스 예외 공통 부모)
- [ ] GlobalExceptionHandler.java (@RestControllerAdvice, 에러 코드 18종 매핑)
- [ ] RateLimitExceededException.java
- [ ] DocType, DocumentStatus, PlanType enum
- [ ] WebConfig.java (기타 웹 설정)

## 3. 인증/인가 구현 (JWT + OAuth2)

- [ ] JwtProvider.java (JWT 생성/검증, HS256, Access 1h, Refresh 7d)
- [ ] JwtAuthenticationFilter.java (OncePerRequestFilter, SecurityContext 설정)
- [ ] UserPrincipal.java (Authentication에 담길 사용자 정보)
- [ ] SecurityConfig.java (필터 체인, CORS, 엔드포인트별 인가 규칙)
- [ ] OAuth2Provider.java (domain/user/port - 인터페이스)
- [ ] GoogleOAuth2Provider.java (infra/oauth2 - 토큰 교환 + 사용자 정보 조회)
- [ ] GitHubOAuth2Provider.java (infra/oauth2)
- [ ] OAuth2 State 검증 흐름 (state 존재 확인, INVALID_OAUTH_STATE 에러)
- [ ] Refresh Token Rotation (is_revoked + 탈취 감지 → 전체 토큰 무효화)

## 4. 인증 API 구현

- [ ] POST /api/v1/auth/oauth2/callback/{provider} (OAuth2 콜백, JWT 발급)
- [ ] POST /api/v1/auth/refresh (토큰 갱신, Rotation 적용)
- [ ] POST /api/v1/auth/logout (Refresh Token revoke)
- [ ] GET /api/v1/auth/me (현재 사용자 정보 + storageUsage)
- [ ] AuthService.java (domain/user/service)
- [ ] User, RefreshToken 엔티티 + Repository
- [ ] OAuthCallbackRequest, RefreshTokenRequest DTO
- [ ] AuthResponse, UserResponse DTO

## 5. R2 스토리지 구현 (Port-Adapter)

- [ ] FileStorage.java (domain/document/port - 인터페이스: upload, download, delete)
- [ ] R2FileStorage.java (infra/storage - FileStorage 구현, AWS SDK S3Client)
- [ ] R2Config.java (S3Client 빈 설정, endpoint/accessKey/secretKey)
- [ ] 키 네이밍: documents/{documentId}/content.{ext}
- [ ] 로컬 환경에서 MinIO로 동작 확인

## 6. 문서 생성 API 구현 (2단계 커밋)

- [ ] POST /api/v1/documents (인증 선택적)
- [ ] DocumentService.createDocument (2단계 커밋: DB INSERT PENDING → R2 업로드 → DB UPDATE ACTIVE)
- [ ] SlugGenerator.java (SecureRandom + 커스텀 알파벳 8자리, 충돌 시 3회 재시도)
- [ ] ContentSanitizer.java (domain/document/port - 인터페이스)
- [ ] JsoupContentSanitizer.java (infra/sanitizer - jsoup Safelist)
- [ ] 비로그인 시 expiresAt = createdAt + 24h 자동 설정 (DR-1)
- [ ] 커스텀 validator로 바이트 크기 검증 (max 5MB)
- [ ] StorageUsage UPDATE (트랜잭션)
- [ ] Document, StorageUsage 엔티티 + Repository
- [ ] CreateDocumentRequest, DocumentResponse DTO
- [ ] DocumentNotFoundException, DocumentExpiredException, ContentTooLargeException

## 7. 문서 서빙 API 구현

- [ ] GET /api/v1/documents/{slug}/view (인증 불필요, 공개)
- [ ] DocumentService.getDocumentForView (ACTIVE만 서빙, 읽기 전용)
- [ ] 만료 시간 확인 (expiresAt < now → 410 DOCUMENT_EXPIRED, DB UPDATE 없음)
- [ ] 삭제된 문서 → 410 DOCUMENT_GONE
- [ ] R2에서 파일 로드 + content + metadata 반환
- [ ] Cache-Control 헤더 + ETag 헤더
- [ ] DocumentViewResponse DTO

## 8. 문서 관리 API 구현 (목록/상세/수정/삭제)

- [ ] GET /api/v1/documents (내 문서 목록, 페이지네이션, 정렬)
- [ ] GET /api/v1/documents/{slug} (편집용 상세, 소유자만)
- [ ] PUT /api/v1/documents/{slug} (문서 수정, 타입 변경 불허)
- [ ] DELETE /api/v1/documents/{slug} (soft delete, 200 OK + body)
- [ ] 소유권 확인 로직 (403 FORBIDDEN)
- [ ] ForbiddenException (domain/user/exception)
- [ ] StorageUsage 동시성 제어 (atomic increment)
- [ ] R2→DB 실패 시 허용 가능한 불일치 로깅
- [ ] DocumentListResponse, UpdateDocumentRequest DTO

## 9. 만료 문서 정리 스케줄러 구현

- [ ] DocumentCleanupScheduler.java (@Scheduled cron 매시 정각)
- [ ] PendingCleanupScheduler.java (PENDING 5분 초과 정리)
- [ ] 만료 문서: ACTIVE + expires_at < NOW → R2 삭제 + status=EXPIRED
- [ ] 완전 삭제: EXPIRED/DELETED + 30일 경과 → DB 물리 삭제
- [ ] 고아 문서 감지: user_id IS NULL AND expires_at IS NULL → 만료 처리
- [ ] 도메인 서비스 호출만 (DocumentService.expireDocuments, cleanupPending)
- [ ] 실패 시 Sentry 알림
- [ ] LIMIT 100 배치 처리

## 10. Rate Limiting 구현

- [ ] RateLimitFilter.java (global/filter)
- [ ] 토큰 버킷 알고리즘 (bucket4j 또는 직접 구현)
- [ ] 비로그인 IP: 분당 10회 / 로그인 사용자: 분당 30회
- [ ] 인메모리 ConcurrentHashMap (MVP 단일 인스턴스)
- [ ] 429 Too Many Requests + Retry-After 헤더

## 11. 로깅 & 모니터링 설정

- [ ] Logback 설정 (JSON 구조화 로깅)
- [ ] 로그 레벨: ERROR/WARN/INFO/DEBUG 정책
- [ ] Sentry SDK 연동 (GlobalExceptionHandler에서 ERROR 전송)
- [ ] MDC에 requestId, userId 추가
- [ ] SpringDoc OpenAPI 3 설정 (Swagger UI)
