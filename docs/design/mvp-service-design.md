# MVP 서비스 설계 문서 (Spring Boot + Next.js 분리 아키텍처)

> 작성일: 2026-03-21
> 참조 문서: docs/plans/service-plan.md, docs/plans/file-storage/file-upload-storage-plan.md
> 아키텍처 변경: Next.js 풀스택 -> 프론트엔드(Next.js) + 백엔드(Spring Boot) 분리

---

## 1. 개요

### 1.1 목적

LLM이 생성한 HTML/MD 파일을 붙여넣기 한 번으로 즉시 공유 URL을 생성하는 서비스의 MVP 설계 문서이다. AI 수정 기능(Phase 2), 버전 히스토리, ZIP/멀티파일, 비밀번호 보호는 MVP 범위에서 제외한다.

기존 Next.js 풀스택 설계에서 **프론트엔드(Next.js 15) + 백엔드(Spring Boot 3.x) 분리 아키텍처**로 재설계한다.

### 1.2 아키텍처 변경 동기

| 항목 | 기존 (Next.js 풀스택) | 변경 후 (분리 아키텍처) |
|------|----------------------|----------------------|
| 백엔드 런타임 | Next.js API Routes + Server Actions | Spring Boot 3.x REST API |
| 인증 | NextAuth.js (Auth.js v5) | Spring Security + JWT + OAuth2 |
| ORM | Drizzle ORM (TypeScript) | Spring Data JPA (Java/Kotlin) |
| DB 접근 | Server Components에서 직접 접근 | REST API 경유만 허용 |
| 배포 | Vercel 단일 배포 | Vercel (FE) + Railway/Fly.io (BE) |

### 1.3 MVP 범위 요약

| 포함 | 제외 |
|------|------|
| HTML/MD 텍스트 붙여넣기 배포 | AI 수정 기능 (Claude API) |
| 단일 파일(.html/.md) 업로드 배포 | 버전 히스토리 |
| 실시간 미리보기 | ZIP/멀티파일 |
| 고유 URL 생성 + 정적 서빙 | 비밀번호 보호 |
| 비로그인 임시 배포 (24h 만료) | 커스텀 도메인 |
| 소셜 로그인 (Google, GitHub) | 팀 워크스페이스 |
| 내 문서 목록 (대시보드) | CLI/API |
| 문서 수정/재배포 | 커스텀 slug |
| 문서 삭제 | |
| MD 기본 테마 렌더링 | |

---

## 2. 아키텍처 설계

### 2.1 시스템 아키텍처

```
사용자 브라우저
    |
    +-- 문서 작성/관리 --> Next.js App (Vercel)
    |                        |
    |                        +-- REST API 호출 --> Spring Boot API (Railway)
    |                                                |
    |                                                +-- Spring Security (JWT + OAuth2)
    |                                                +-- Spring Data JPA --> PostgreSQL (Supabase)
    |                                                +-- AWS SDK for Java --> Cloudflare R2
    |
    +-- 공유 URL 접근 ----> Next.js App (Vercel)
                               |
                               +-- REST API 호출 --> Spring Boot API
                                                      +-- DB 조회 + R2 파일 로드
                                                      +-- 응답 반환
```

### 2.2 컴포넌트 구성

```
[프론트엔드 - Next.js 15]
  +-- UI 렌더링 (React Server/Client Components)
  +-- 정적 페이지 (랜딩, 만료 안내 등)
  +-- API 클라이언트 레이어 (Spring Boot API 호출)
  +-- OAuth2 로그인 리다이렉트 처리
  +-- JWT 토큰 관리 (쿠키/localStorage)

[백엔드 - Spring Boot 3.x]
  +-- REST Controller (API 엔드포인트)
  +-- Spring Security (JWT 인증 + OAuth2 소셜 로그인)
  +-- Service Layer (비즈니스 로직)
  +-- Repository Layer (Spring Data JPA)
  +-- R2 Storage Service (AWS SDK for Java)
  +-- Scheduler (만료 문서 정리)

[외부 서비스]
  +-- PostgreSQL (Supabase) -- 메타데이터
  +-- Cloudflare R2 -- HTML/MD 파일 저장
  +-- Google/GitHub OAuth2 Provider
```

### 2.3 기술 스택

#### 백엔드

| 구성요소 | 선택 | 이유 |
|---------|------|------|
| 프레임워크 | **Spring Boot 3.4.x** | 안정적인 REST API 서버, Java 생태계 |
| 언어 | **Java 21** | LTS, Virtual Threads 지원, Record 타입 |
| 인증 | **Spring Security 6 + JWT** | OAuth2 Resource Server + 자체 JWT 발급 |
| ORM | **Spring Data JPA + Hibernate** | 성숙한 ORM, 자동 DDL, 쿼리 메서드 |
| DB | **PostgreSQL 15+ (Supabase)** | 메타데이터 저장 |
| 파일 저장소 | **Cloudflare R2 (AWS SDK for Java v2)** | S3 호환 API, 이그레스 무료 |
| 빌드 | **Gradle (Kotlin DSL)** | 빠른 빌드, 의존성 관리 |
| API 문서 | **SpringDoc OpenAPI 3** | Swagger UI 자동 생성 |
| 검증 | **Jakarta Validation (Bean Validation)** | 요청 DTO 검증 |
| 스케줄링 | **Spring @Scheduled** | 만료 문서 정리 CRON |
| 마이그레이션 | **Flyway** | DB 스키마 버전 관리 |

#### 프론트엔드

| 구성요소 | 선택 | 이유 |
|---------|------|------|
| 프레임워크 | **Next.js 15 (App Router)** | SSR + 정적 페이지 |
| 언어 | **TypeScript** | 타입 안전성 |
| 스타일링 | **Tailwind CSS + shadcn/ui** | 빠른 UI 개발 |
| 에디터 | **Monaco Editor** | HTML/MD 코드 편집 |
| 상태 관리 | **Zustand** | 경량, 단순 |
| MD 렌더링 (미리보기용) | **unified (remark + rehype)** | 클라이언트 미리보기 |
| HTTP 클라이언트 | **ky** 또는 **fetch wrapper** | Spring Boot API 호출 |

#### 인프라

| 구성요소 | 선택 | 이유 |
|---------|------|------|
| FE 호스팅 | **Vercel** | Next.js 최적 배포 |
| BE 호스팅 | **Railway** | 저비용 Docker 배포, Hobby 플랜 $5/월 |
| DB | **Supabase PostgreSQL** | Free/Pro 티어 |
| 파일 저장소 | **Cloudflare R2** | 이그레스 무료, 넉넉한 무료 티어 |
| 도메인 | **Cloudflare DNS** | CDN + DNS 통합 |
| CI/CD | **GitHub Actions** | 자동 테스트, 자동 배포 |

### 2.4 백엔드 호스팅 비교

| 항목 | Railway | Fly.io | Render |
|------|---------|--------|--------|
| 최소 비용 | $5/월 (Hobby) | $0 (256MB) ~ $1.94/월 | $0 (무료, 750h/월) |
| Docker 지원 | O | O | O |
| 자동 스케일 | O (Pro) | O | X (Free) |
| DB 내장 | O (PostgreSQL) | O (PostgreSQL) | O (PostgreSQL) |
| 슬립 정책 | 없음 (Hobby+) | 없음 (유료) | 15분 비활성 시 슬립 (Free) |
| Spring Boot 친화성 | 높음 | 높음 | 높음 |

**선택: Railway Hobby ($5/월)**

- 슬립 없이 상시 가동 (Render Free는 15분 비활성 시 슬립으로 콜드 스타트 발생)
- 간편한 Docker/Dockerfile 배포
- 환경변수 관리 UI 우수
- 512MB RAM 기본 제공, Spring Boot에 충분

---

## 3. 도메인 모델

### 3.1 바운디드 컨텍스트

MVP에서는 단일 컨텍스트로 운영한다. Spring Boot 내에서 패키지 단위로 모듈을 분리한다.

```
[Document Publishing Context]
  - 문서 생성, 저장, 서빙, 관리의 전체 라이프사이클
  - 사용자 인증은 Spring Security + OAuth2 위임
```

### 3.2 핵심 엔티티

#### User (사용자)

```
User {
  id:             UUID (PK, auto-generated)
  email:          String (unique, not null)
  name:           String (nullable)
  profileImage:   String (nullable)       -- OAuth2 프로필 이미지 URL
  provider:       String (not null)       -- 'google' | 'github'
  providerId:     String (not null)       -- OAuth2 provider의 사용자 ID
  plan:           PlanType                -- 'free' | 'starter' | 'pro'
  createdAt:      LocalDateTime
  updatedAt:      LocalDateTime
}
```

- OAuth2 로그인 전용이므로 password 필드는 없다.
- `provider` + `providerId` 조합으로 소셜 계정을 식별한다.
- `plan` 필드는 MVP에서는 모두 `'free'`로 고정하되, Phase 2 플랜 체계를 위해 미리 포함한다.

#### Document (문서)

```
Document {
  id:             String (PK)             -- nanoid 8자리, slug와 동일
  slug:           String (unique)         -- URL 경로, id와 동일 값
  userId:         UUID (FK->User, nullable) -- null = 비로그인 임시 문서
  title:          String (nullable)
  docType:        DocType                 -- 'html' | 'markdown'
  r2Key:          String (not null)       -- R2 저장 경로
  contentSize:    Long (not null)         -- 바이트
  status:         DocumentStatus          -- 'pending' | 'active' | 'expired' | 'deleted'
  expiresAt:      LocalDateTime (nullable) -- 비로그인: 생성 후 24h, 로그인: null
  createdAt:      LocalDateTime
  updatedAt:      LocalDateTime
}
```

- `status`에 `'pending'` 추가: R2 업로드 전 DB에 먼저 INSERT하는 2단계 커밋 패턴 적용 (기존 C-1 검토 반영).
- `contentSize`를 `Long`(BIGINT)으로 통일하여 `StorageUsage.totalBytes` 집계 시 형변환 불필요 (기존 W-5 반영).

#### StorageUsage (사용량 추적)

```
StorageUsage {
  userId:         UUID (PK, FK->User)
  totalBytes:     Long                    -- 사용자의 총 저장 용량
  documentCount:  Integer                 -- 사용자의 활성 문서 수
  updatedAt:      LocalDateTime
}
```

> **documentCount 정확성 주의**: `documentCount`는 여러 경로(생성, 삭제, 스케줄러 만료 처리)에서 증감되는 캐싱 카운터이므로 실제 ACTIVE 문서 수와 불일치할 수 있다. 대시보드의 "사용량" 표시(`GET /api/v1/auth/me` 응답의 `storageUsage.documentCount`)는 `storage_usage` 테이블 대신 `SELECT COUNT(*) FROM documents WHERE user_id = ? AND status = 'active'` 실시간 쿼리로 조회한다 (MVP 규모에서 성능 문제 없음). `storage_usage.document_count`는 내부 제한 검사 등 근사값으로 충분한 용도에 사용하고, 스케줄러의 만료 처리 시에도 로그인 사용자(`user_id IS NOT NULL`)의 문서에 대해 `document_count` 감소를 명시적으로 처리한다.

### 3.3 값 객체 (Value Objects / Enums)

```java
enum DocType { HTML, MARKDOWN }

enum DocumentStatus { PENDING, ACTIVE, EXPIRED, DELETED }

enum PlanType { FREE, STARTER, PRO }

// R2 키 패턴
// documents/{documentId}/content.{ext}
// ext: "html" | "md"
// 예: documents/xK9mP2nQ/content.html

// Slug: nanoid 호환 8자리, URL-safe [A-Za-z0-9]
// 구현: SecureRandom + 커스텀 알파벳으로 직접 구현 (외부 라이브러리 의존 없음)
// JavaScript nanoid(github.com/ai/nanoid)는 Java 라이브러리가 아니므로,
// `com.aventrix.jnanoid:jnanoid` 대신 직접 구현을 선택한다.
// 이유: 단순한 로직(SecureRandom + 문자셋 인덱싱)이므로 외부 의존성 추가 불필요

// FileConstraints
MAX_CONTENT_SIZE = 5 * 1024 * 1024  // 5MB
ALLOWED_DOC_TYPES = [HTML, MARKDOWN]
```

### 3.4 엔티티 관계

```
User (1) ---- (0..N) Document
  |
  +-- (0..1) StorageUsage

[비로그인 사용자]
  Document.userId = null
  Document.expiresAt = createdAt + 24h
```

### 3.5 도메인 이벤트

MVP에서는 이벤트 버스를 구축하지 않고, Service 레이어에서 직접 처리한다. Spring의 `ApplicationEventPublisher`를 사용한 내부 이벤트 발행은 Phase 2에서 도입을 검토한다.

| 이벤트 | 트리거 | 후속 처리 |
|--------|--------|-----------|
| `DocumentCreated` | 문서 생성 완료 (status: active) | StorageUsage 업데이트 |
| `DocumentUpdated` | 문서 내용 수정 | R2 파일 덮어쓰기, StorageUsage 업데이트 |
| `DocumentDeleted` | 문서 삭제 | R2 파일 삭제, StorageUsage 업데이트 |
| `DocumentExpired` | 스케줄러에 의한 만료 처리 | R2 파일 삭제, status = 'expired' |

### 3.6 도메인 규칙 (불변식)

| 규칙 | 설명 |
|------|------|
| DR-1 | 비로그인 문서는 반드시 `expiresAt`이 설정되어야 한다 (createdAt + 24h) |
| DR-2 | 로그인 사용자의 문서는 `expiresAt`이 null이다 (영구) |
| DR-3 | 문서 내용(content)의 크기는 5MB를 초과할 수 없다 |
| DR-4 | `docType`은 반드시 `HTML` 또는 `MARKDOWN`이어야 한다 |
| DR-5 | `slug`(= `id`)는 시스템 전체에서 유일해야 한다 |
| DR-6 | `status`가 `EXPIRED`, `DELETED`, `PENDING`인 문서는 서빙하지 않는다 |
| DR-7 | 문서 수정/삭제는 해당 문서의 소유자(userId)만 가능하다 |
| DR-8 | 비로그인 문서는 수정/삭제할 수 없다 |
| DR-9 | `status`가 `PENDING`인 문서는 생성 후 5분 이내에 `ACTIVE`로 전이되지 않으면 스케줄러가 정리한다 |

---

## 4. API 설계

### 4.1 공통 규격

#### Base URL

```
프론트엔드:  https://{서비스도메인}              (Vercel)
백엔드 API: https://api.{서비스도메인}           (Railway)
            또는 https://{app-name}.up.railway.app
```

#### 공통 응답 포맷

```json
// 성공 응답
{
  "success": true,
  "data": { ... }
}

// 에러 응답
{
  "success": false,
  "error": {
    "code": "CONTENT_REQUIRED",
    "message": "문서 내용을 입력해주세요"
  }
}
```

Spring Boot에서 `@RestControllerAdvice`로 글로벌 예외 핸들러를 구현하여 일관된 응답 포맷을 보장한다.

#### 공통 헤더

| 헤더 | 용도 |
|------|------|
| `Content-Type: application/json` | 요청/응답 본문 형식 |
| `Authorization: Bearer {JWT}` | 인증이 필요한 API 호출 시 |
| `X-Forwarded-For` | Rate Limiting용 클라이언트 IP 식별 |

#### CORS 설정

```
Access-Control-Allow-Origin: https://{서비스도메인}
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

Spring Security의 `CorsConfigurationSource` 빈으로 설정한다. 개발 환경에서는 `http://localhost:3000`을 추가 허용한다.

#### Rate Limiting

| 대상 | 제한 |
|------|------|
| 비로그인 IP | 분당 10회 API 호출 |
| 로그인 사용자 | 분당 30회 API 호출 |

Spring Boot에서 `bucket4j-spring-boot-starter` 또는 직접 구현한 필터로 처리한다. 토큰 버킷 알고리즘을 사용하며, 상태는 인메모리 `ConcurrentHashMap`에 저장한다 (단일 인스턴스 MVP 기준). 프로덕션 스케일 시 Redis로 전환한다.

Rate limit 초과 시 `429 Too Many Requests` 응답과 함께 `Retry-After` 헤더를 포함한다.

### 4.2 엔드포인트 목록

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| `POST` | `/api/v1/documents` | 선택 | 문서 생성 |
| `GET` | `/api/v1/documents` | 필수 | 내 문서 목록 조회 |
| `GET` | `/api/v1/documents/{slug}` | 필수 | 문서 상세 조회 (편집용 메타데이터 + 내용). 소유자만 접근 가능 |
| `PUT` | `/api/v1/documents/{slug}` | 필수 | 문서 수정 |
| `DELETE` | `/api/v1/documents/{slug}` | 필수 | 문서 삭제 |
| `GET` | `/api/v1/documents/{slug}/view` | 없음 | 문서 서빙용 (공유 URL 접근 시 프론트엔드가 호출) |
| `POST` | `/api/v1/auth/oauth2/callback/{provider}` | 없음 | OAuth2 인증 콜백 처리, JWT 발급 |
| `POST` | `/api/v1/auth/refresh` | 없음 | JWT 리프레시 토큰으로 액세스 토큰 갱신 |
| `GET` | `/api/v1/auth/me` | 필수 | 현재 로그인 사용자 정보 |
| `POST` | `/api/v1/auth/logout` | 필수 | 로그아웃 (리프레시 토큰 무효화) |

`/api/v1` 버전 접두사를 사용하여 향후 API 변경 시 하위 호환성을 유지한다.

### 4.3 API 상세 명세

#### POST /api/v1/documents -- 문서 생성

**요청**

```json
{
  "content": "<html>...</html>",
  "type": "html",
  "title": "My Document"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `content` | String | O | HTML 또는 MD 텍스트 (최대 5MB) |
| `type` | String | O | `"html"` 또는 `"markdown"` |
| `title` | String | X | 문서 제목 |

**성공 응답 (201 Created)**

```json
{
  "success": true,
  "data": {
    "id": "xK9mP2nQ",
    "slug": "xK9mP2nQ",
    "url": "https://{서비스도메인}/xK9mP2nQ",
    "title": "My Document",
    "docType": "html",
    "contentSize": 2048,
    "status": "active",
    "expiresAt": "2026-03-22T12:00:00Z",
    "createdAt": "2026-03-21T12:00:00Z",
    "updatedAt": "2026-03-21T12:00:00Z"
  }
}
```

#### GET /api/v1/documents -- 내 문서 목록

**요청 쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `page` | int | 0 | 페이지 번호 (0-based, Spring Data 기본 관례) |
| `size` | int | 20 | 페이지당 항목 수 (최대 50) |
| `sort` | String | `updatedAt,desc` | 정렬 (`updatedAt,desc`, `createdAt,asc`, `title,asc`) |

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "documents": [
      {
        "id": "xK9mP2nQ",
        "slug": "xK9mP2nQ",
        "url": "https://{서비스도메인}/xK9mP2nQ",
        "title": "My Document",
        "docType": "html",
        "contentSize": 2048,
        "status": "active",
        "createdAt": "2026-03-21T12:00:00Z",
        "updatedAt": "2026-03-21T12:00:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 42,
      "totalPages": 3
    }
  }
}
```

#### GET /api/v1/documents/{slug} -- 문서 상세 조회 (편집용)

인증 필수. 소유자만 접근 가능 (403 Forbidden 반환). R2에서 원본 콘텐츠를 함께 로드하여 반환한다.

> **대용량 콘텐츠 트레이드오프**: `content` 필드(최대 5MB)가 JSON body에 포함된다. JSON 직렬화 시 이스케이핑으로 페이로드가 증가할 수 있으나, MVP 규모(일 100개 문서, 평균 50KB)에서는 허용 가능하다. 5MB 문서를 빈번하게 편집하는 유스케이스가 확인되면 Phase 2에서 `GET /api/v1/documents/{slug}/content` (Content-Type: text/plain)로 콘텐츠를 분리하는 방안을 적용한다. Spring Boot의 `server.servlet.max-http-form-parameter-size` 및 Jackson 스트리밍 직렬화는 별도 설정하지 않으며, `spring.servlet.multipart.max-request-size=10MB`를 명시적으로 설정한다.

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": "xK9mP2nQ",
    "slug": "xK9mP2nQ",
    "url": "https://{서비스도메인}/xK9mP2nQ",
    "title": "My Document",
    "docType": "html",
    "content": "<html>...</html>",
    "contentSize": 2048,
    "status": "active",
    "expiresAt": null,
    "createdAt": "2026-03-21T12:00:00Z",
    "updatedAt": "2026-03-21T12:00:00Z"
  }
}
```

#### GET /api/v1/documents/{slug}/view -- 문서 서빙용 (공개)

인증 불필요. 공유 URL 접근 시 프론트엔드가 호출한다. 만료/삭제/PENDING 상태의 문서는 에러를 반환한다.

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": "xK9mP2nQ",
    "title": "My Document",
    "docType": "html",
    "content": "<html>...</html>",
    "createdAt": "2026-03-21T12:00:00Z"
  }
}
```

응답 헤더에 캐싱 지시를 포함한다:

```
Cache-Control: public, max-age=300, s-maxage=3600, stale-while-revalidate=86400
```

**에러 응답**

| 상황 | HTTP | 코드 |
|------|------|------|
| 문서 없음 | 404 | `DOCUMENT_NOT_FOUND` |
| 만료된 문서 | 410 | `DOCUMENT_EXPIRED` |
| 삭제된 문서 | 410 | `DOCUMENT_GONE` |

만료 문서에 `410 Gone`을 사용하여 프론트엔드가 만료 안내 UI를 분기 렌더링할 수 있게 한다.

#### PUT /api/v1/documents/{slug} -- 문서 수정

**요청**

```json
{
  "content": "<html>updated...</html>",
  "title": "Updated Title"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `content` | String | X | 변경된 내용 |
| `title` | String | X | 변경된 제목 |

최소 하나의 필드는 포함되어야 한다. **타입 변경(`type` 필드)은 MVP에서 허용하지 않는다.** R2 키 충돌 및 추가 복잡도를 방지한다 (기존 W-1 반영).

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": "xK9mP2nQ",
    "slug": "xK9mP2nQ",
    "url": "https://{서비스도메인}/xK9mP2nQ",
    "title": "Updated Title",
    "docType": "html",
    "contentSize": 3072,
    "status": "active",
    "expiresAt": null,
    "updatedAt": "2026-03-21T15:00:00Z"
  }
}
```

`expiresAt`을 응답에 포함하여 GET과 일관성을 유지한다 (기존 S-2 반영).

#### DELETE /api/v1/documents/{slug} -- 문서 삭제

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": "xK9mP2nQ",
    "slug": "xK9mP2nQ",
    "deletedAt": "2026-03-21T16:00:00Z"
  }
}
```

`deletedAt`은 soft delete 시점의 `updatedAt` 값이다. DB에 별도 `deleted_at` 컬럼은 두지 않는다 (기존 S-3 반영).

### 4.4 인증 API

#### POST /api/v1/auth/oauth2/callback/{provider} -- OAuth2 콜백

OAuth2 인가 코드를 받아 사용자를 생성/조회하고 JWT를 발급한다. **CSRF 방어를 위해 `state` 파라미터를 반드시 검증한다.**

**요청**

```json
{
  "code": "authorization_code_from_oauth_provider",
  "redirectUri": "https://{서비스도메인}/auth/callback",
  "state": "cryptographically_random_state_value"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `code` | String | O | OAuth2 인가 코드 |
| `redirectUri` | String | O | 리다이렉트 URI (provider에 등록된 값과 일치해야 함) |
| `state` | String | O | CSRF 방어용 state 값. 로그인 시작 시 생성하여 쿠키에 저장한 값과 일치해야 함 |

**state 검증 실패 시 에러 응답 (400 Bad Request)**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_OAUTH_STATE",
    "message": "OAuth2 state 값이 유효하지 않습니다"
  }
}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "expiresIn": 3600,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "name": "User Name",
      "profileImage": "https://...",
      "plan": "free"
    }
  }
}
```

#### POST /api/v1/auth/refresh -- 토큰 갱신

Refresh Token Rotation을 적용한다. 갱신 시 기존 Refresh Token은 즉시 무효화되고 새 Refresh Token이 발급된다. 이미 무효화된 토큰으로 갱신 시도 시 해당 사용자의 모든 토큰을 일괄 무효화한다 (탈취 감지).

**요청**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "bmV3IHJlZnJlc2ggdG9rZW4...",
    "expiresIn": 3600
  }
}
```

**에러 응답**

| 상황 | HTTP | 코드 |
|------|------|------|
| 토큰 없음/유효하지 않음 | 401 | `INVALID_TOKEN` |
| 토큰 만료 | 401 | `TOKEN_EXPIRED` |
| 이미 무효화된 토큰 재사용 (탈취 의심) | 401 | `TOKEN_REUSE_DETECTED` |

#### GET /api/v1/auth/me -- 현재 사용자

**성공 응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "name": "User Name",
    "profileImage": "https://...",
    "plan": "free",
    "storageUsage": {
      "totalBytes": 51200,
      "documentCount": 3
    }
  }
}
```

### 4.5 에러 코드 총괄

| HTTP | 코드 | 상황 |
|------|------|------|
| 400 | `CONTENT_REQUIRED` | content 필드 누락 |
| 400 | `CONTENT_TOO_LARGE` | content > 5MB |
| 400 | `INVALID_DOC_TYPE` | type이 html/markdown 아님 |
| 400 | `INVALID_PARAMETERS` | 요청 파라미터 오류 |
| 400 | `EMPTY_UPDATE` | PUT 요청에 변경 필드가 없음 |
| 400 | `INVALID_OAUTH_STATE` | OAuth2 state 값 누락 또는 불일치 (CSRF 의심) |
| 401 | `UNAUTHORIZED` | JWT 누락 또는 만료 |
| 401 | `INVALID_TOKEN` | JWT 서명 불일치 또는 변조 |
| 401 | `TOKEN_EXPIRED` | Refresh Token 만료 |
| 401 | `TOKEN_REUSE_DETECTED` | 이미 무효화된 Refresh Token 재사용 (탈취 의심, 전체 토큰 무효화) |
| 403 | `FORBIDDEN` | 문서 소유자가 아닌 사용자의 접근 |
| 404 | `DOCUMENT_NOT_FOUND` | 존재하지 않는 문서 |
| 410 | `DOCUMENT_EXPIRED` | 만료된 문서 |
| 410 | `DOCUMENT_GONE` | 삭제된 문서 |
| 429 | `RATE_LIMIT_EXCEEDED` | API 호출 제한 초과 |
| 500 | `STORAGE_ERROR` | R2 읽기/쓰기 실패 |
| 500 | `DATABASE_ERROR` | PostgreSQL 쿼리 실패 |
| 500 | `INTERNAL_ERROR` | 예상치 못한 서버 오류 |

---

## 5. 인증/인가 설계

### 5.1 인증 플로우 (OAuth2 + JWT)

#### 5.1.1 OAuth2 State (CSRF 방어) 흐름

OAuth2 인가 코드 주입(CSRF) 공격을 방어하기 위해 `state` 파라미터의 생성/저장/검증을 다음과 같이 처리한다.

```
[State 생명주기]
1. 로그인 시작 시: Next.js가 crypto.randomUUID()로 state 생성
2. 저장: httpOnly 쿠키 `oauth_state`에 저장 (SameSite=Lax, Secure, Max-Age=300)
3. OAuth2 요청: state를 쿼리 파라미터로 포함하여 Provider에 전달
4. 콜백 수신: URL의 state와 쿠키의 state를 Next.js에서 비교 검증
5. Spring Boot 전달: 검증 통과 시 code + state를 Spring Boot에 전달
6. Spring Boot 검증: state 값의 존재 여부를 확인 (빈 문자열/null 거부)
7. 쿠키 삭제: 콜백 처리 완료 후 oauth_state 쿠키 즉시 삭제 (재사용 방지)
```

```
[1. OAuth2 로그인 시작]
Browser                  Next.js                  Spring Boot           OAuth Provider
  |                        |                        |                      |
  | "Google 로그인" 클릭    |                        |                      |
  | ---------------------> |                        |                      |
  |                        |                        |                      |
  |                        | 1. state = crypto.randomUUID()                |
  |                        | 2. Set-Cookie: oauth_state={state}            |
  |                        |    (httpOnly, SameSite=Lax, Secure,           |
  |                        |     Max-Age=300)                              |
  |                        |                        |                      |
  | 302 Redirect           |                        |                      |
  | <--------------------- |                        |                      |
  |  Location: https://accounts.google.com/o/oauth2/auth                   |
  |  ?client_id={CLIENT_ID}                                                |
  |  &redirect_uri=https://{서비스도메인}/auth/callback                      |
  |  &scope=email+profile                                                  |
  |  &response_type=code                                                   |
  |  &state={state}                                                        |
  | ---------------------------------------------------------------------> |
  |                                                                        |
  | 사용자 동의 후 리다이렉트                                                  |
  | <--------------------------------------------------------------------- |
  |  Location: https://{서비스도메인}/auth/callback?code={CODE}&state={STATE} |
  |                        |                        |                      |
  | ----code,state-------> |                        |                      |
  |                        |                        |                      |
  |                        | 3. state 검증:                                |
  |                        |    URL의 state == 쿠키의 oauth_state?         |
  |                        |    불일치 시 -> /auth/error 리다이렉트          |
  |                        |                        |                      |
  |                        | POST /api/v1/auth/oauth2/callback/google      |
  |                        | {code, redirectUri, state}                    |
  |                        | ---------------------> |                      |
  |                        |                        |                      |
  |                        |                        | 4. state 존재 확인    |
  |                        |                        |    (null/빈값 거부)   |
  |                        |                        |                      |
  |                        |                        | 5. code -> token 교환 |
  |                        |                        | -------------------> |
  |                        |                        | <------------------- |
  |                        |                        | {access_token, ...}  |
  |                        |                        |                      |
  |                        |                        | 6. 사용자 정보 조회    |
  |                        |                        | -------------------> |
  |                        |                        | <------------------- |
  |                        |                        | {email, name, ...}   |
  |                        |                        |                      |
  |                        |                        | 7. DB: 사용자 생성/조회|
  |                        |                        |    JWT 발급           |
  |                        |                        |                      |
  |                        | {accessToken,           |                      |
  |                        |  refreshToken, user}   |                      |
  |                        | <--------------------- |                      |
  |                        |                        |                      |
  | Set cookie/storage     |                        |                      |
  | + Delete oauth_state   |                        |                      |
  | + redirect /dashboard  |                        |                      |
  | <--------------------- |                        |                      |
```

### 5.2 JWT 설계

| 항목 | 값 |
|------|-----|
| 알고리즘 | HS256 (대칭키, MVP 단순화) |
| Access Token 만료 | 1시간 |
| Refresh Token 만료 | 7일 |
| Access Token Payload | `{ sub: userId, email, plan, iat, exp }` |
| Refresh Token 저장 | DB `refresh_tokens` 테이블 (로그아웃 시 무효화 가능) |

Phase 2에서 RS256(비대칭키)으로 전환을 검토한다 (마이크로서비스 확장 시 공개키 배포 필요).

### 5.3 Refresh Token 테이블

```sql
CREATE TABLE refresh_tokens (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token       TEXT NOT NULL UNIQUE,
  is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at  TIMESTAMPTZ NOT NULL,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

### 5.3.1 Refresh Token Rotation 흐름

토큰 갱신 시 기존 토큰을 즉시 무효화하고 새 토큰을 발급하는 Rotation을 적용한다.

```
[정상 Rotation 흐름]
1. 클라이언트가 POST /api/v1/auth/refresh 호출 (refreshToken 전달)
2. DB에서 토큰 조회
   - 토큰이 없으면 -> 401 INVALID_TOKEN
   - is_revoked = true이면 -> [탈취 감지 시나리오] 실행
   - expires_at < now이면 -> 401 TOKEN_EXPIRED
3. 기존 Refresh Token: is_revoked = true로 UPDATE
4. 새 Refresh Token 생성 + DB INSERT
5. 새 Access Token 발급
6. 응답: { accessToken, refreshToken, expiresIn }
   (3~4단계는 하나의 @Transactional로 원자 처리)

[탈취 감지 시나리오]
이미 revoked된 토큰으로 갱신 시도가 들어오면, 공격자가 탈취한 토큰을
사용하고 있을 가능성이 높다. 이 경우:
1. 해당 user_id의 모든 Refresh Token을 일괄 무효화 (is_revoked = true)
2. 401 TOKEN_REUSE_DETECTED 에러 반환
3. 클라이언트는 사용자를 로그인 페이지로 리다이렉트
4. 이벤트 로깅 (사용자 ID, 시도 시간, IP 등)

이 방식은 정상 사용자도 재로그인해야 하는 불편함이 있지만,
토큰 탈취 시 피해를 최소화하는 보안 우선 전략이다.
```

### 5.4 프론트엔드 토큰 관리

- **Access Token**: `httpOnly` 쿠키에 저장 (XSS 방지). `SameSite=Lax`, `Secure=true`.
- **Refresh Token**: `httpOnly` 쿠키에 저장. Path를 `/api/v1/auth/refresh`로 제한하여 불필요한 전송을 방지.
- Next.js의 `middleware.ts`에서 요청 시 쿠키에서 토큰을 읽어 Spring Boot API 호출 시 `Authorization: Bearer` 헤더로 전달.
- Access Token 만료 시 자동으로 Refresh Token으로 갱신. Refresh Token도 만료되면 로그인 페이지로 리다이렉트.

### 5.5 Spring Security 필터 체인 구조

```
HTTP Request
  |
  +-> CorsFilter
  +-> RateLimitFilter
  +-> JwtAuthenticationFilter
  |     - Authorization 헤더에서 JWT 추출
  |     - 서명 검증 + 만료 확인
  |     - 유효하면 SecurityContext에 Authentication 설정
  |     - 유효하지 않으면 필터 체인 계속 (익명 요청)
  +-> SecurityFilterChain
        - /api/v1/documents POST: permitAll (비로그인 문서 생성)
        - /api/v1/documents GET, PUT, DELETE: authenticated
        - /api/v1/documents/{slug}/view: permitAll
        - /api/v1/auth/**: permitAll
        - 그 외: denyAll
```

---

## 6. 핵심 로직 설계

### 6.1 문서 생성 (2단계 커밋 패턴)

기존 설계의 C-1(고아 R2 파일) 검토를 반영하여 pending/active 2단계 커밋을 적용한다.

```
Browser            Next.js             Spring Boot              R2              PostgreSQL
  |                  |                    |                      |                  |
  | POST /api/v1/documents               |                      |                  |
  | (content, type, title)               |                      |                  |
  | ----------------->|                  |                      |                  |
  |                   | Forward          |                      |                  |
  |                   | ----------------->|                     |                  |
  |                   |                  |                      |                  |
  |                   |                  | 1. 입력 검증 (Bean Validation)           |
  |                   |                  |    - @NotBlank content                   |
  |                   |                  |    - @Size(max=5MB) content              |
  |                   |                  |    - @ValidDocType type                  |
  |                   |                  |                      |                  |
  |                   |                  | 2. JWT에서 userId 추출 (nullable)        |
  |                   |                  |                      |                  |
  |                   |                  | 3. nanoid 8자리 생성  |                  |
  |                   |                  |                      |                  |
  |                   |                  | 4. HTML이면 새니타이징 |                  |
  |                   |                  |    (iframe/object/embed, javascript:)    |
  |                   |                  |                      |                  |
  |                   |                  | 5. DB INSERT (status=PENDING)            |
  |                   |                  | ---------------------------------------->|
  |                   |                  | <----------------------------------------|
  |                   |                  |                      |                  |
  |                   |                  | 6. R2 업로드          |                  |
  |                   |                  | --------------------->|                 |
  |                   |                  | <---------------------|                 |
  |                   |                  |                      |                  |
  |                   |                  | 7. DB UPDATE (status=ACTIVE)             |
  |                   |                  |    + StorageUsage UPDATE (트랜잭션)       |
  |                   |                  | ---------------------------------------->|
  |                   |                  | <----------------------------------------|
  |                   |                  |                      |                  |
  |                   | { url, slug, ... }                      |                  |
  |                   | <----------------|                     |                  |
  | { url, slug, ... }                  |                      |                  |
  | <-----------------|                  |                      |                  |
```

**실패 처리**:
- 5단계(DB INSERT) 실패: 에러 반환. R2에는 아직 파일이 없으므로 정합성 문제 없음.
- 6단계(R2 업로드) 실패: DB에 `PENDING` 레코드만 존재. 스케줄러가 5분 초과 `PENDING` 문서를 정리.
- 7단계(DB UPDATE) 실패: `PENDING` 상태 유지 + R2에 파일 존재. 스케줄러가 `PENDING` 정리 시 R2 파일도 함께 삭제.

### 6.2 문서 서빙 (프론트엔드 -> 백엔드)

```
Browser            Next.js (서버)          Spring Boot
  |                  |                       |
  | GET /{slug}      |                       |
  | ----------------->|                      |
  |                   |                      |
  |                   | GET /api/v1/documents/{slug}/view
  |                   | ---------------------->|
  |                   |                       |
  |                   |                       | 1. slug로 문서 조회
  |                   |                       | 2. 상태 확인 (ACTIVE만 서빙)
  |                   |                       | 3. 만료 시간 확인 (읽기 전용 판단)
  |                   |                       |    expiresAt != null && expiresAt < now
  |                   |                       |    -> 410 DOCUMENT_EXPIRED 반환
  |                   |                       |    (DB UPDATE 없음, status 변경은
  |                   |                       |     스케줄러에 위임)
  |                   |                       | 4. R2에서 파일 로드
  |                   |                       | 5. 응답 반환 (content + metadata)
  |                   |                       |
  |                   | { docType, content, ...}
  |                   | <----------------------|
  |                   |                       |
  |                   | HTML인 경우:
  |                   |   sandbox iframe 래퍼 페이지 렌더링
  |                   | MD인 경우:
  |                   |   unified로 HTML 변환 + 테마 적용
  |                   |
  | 렌더링된 HTML     |
  | <-----------------|
```

#### HTML 서빙 래퍼 (sandbox iframe)

기존 C-2 검토를 반영하여 `allow-same-origin`을 제거한다.

```
래퍼 페이지 (Next.js 렌더링):
+-------------------------------------------+
|  상단 바: 서비스 로고 + "나도 만들어보기"     |
+-------------------------------------------+
|                                           |
|  <iframe                                  |
|    sandbox="allow-scripts"                |
|    srcdoc="{사용자 HTML 내용}"             |
|    style="width:100%;                     |
|           height:calc(100vh - 상단바높이)" |
|  />                                       |
|                                           |
+-------------------------------------------+
```

- `sandbox="allow-scripts"` : 스크립트 실행은 허용하되 `allow-same-origin`은 제거하여 iframe이 부모 페이지와 동일 origin으로 동작하는 것을 차단한다.
- 트레이드오프: `allow-same-origin` 제거 시 iframe 내 `localStorage`, `sessionStorage` 접근이 불가하고 일부 LLM 생성 HTML에서 동작 제한이 발생할 수 있다. 그러나 XSS 방지가 우선이다.
- Phase 2에서 서빙 도메인(`view.{서비스}.com`)을 분리하면 `allow-same-origin`을 다시 추가할 수 있다.

**외부 네트워크 요청 차단 (CSP 헤더)**:

`sandbox="allow-scripts"`는 스크립트 실행을 허용하므로, iframe 내 JavaScript가 `fetch()`/`XMLHttpRequest`로 외부 서버에 요청을 보내 방문자 정보를 유출할 수 있다. 이를 차단하기 위해 HTML 서빙 래퍼 페이지에 다음 CSP 헤더를 추가한다.

```
Content-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:
```

Next.js의 `[slug]/page.tsx`에서 `headers()` 또는 `next.config.js`의 헤더 설정으로 적용한다.

- `default-src 'none'`: 기본적으로 모든 외부 리소스 로드를 차단한다.
- `script-src 'unsafe-inline'`: 인라인 스크립트만 허용 (외부 CDN의 JS 차단).
- `style-src 'unsafe-inline'`: 인라인 스타일만 허용 (외부 CDN의 CSS 차단).
- `img-src data:`: data URI 이미지만 허용 (외부 이미지 URL 차단).
- 트레이드오프: LLM 생성 HTML이 외부 CDN의 CSS/JS/이미지를 참조하는 경우 렌더링이 깨진다. 대부분의 LLM 출력물은 인라인 스타일/스크립트를 사용하므로 영향이 제한적이다. Phase 2에서 사용자 피드백에 따라 `img-src https:` 등을 선택적으로 허용할 수 있다.

### 6.3 문서 수정

```
Browser            Next.js              Spring Boot              R2              PostgreSQL
  |                  |                    |                      |                  |
  | PUT /api/v1/documents/{slug}         |                      |                  |
  | {content, title}                     |                      |                  |
  | ----------------->| Forward          |                      |                  |
  |                   | ----------------->|                     |                  |
  |                   |                  |                      |                  |
  |                   |                  | 1. JWT 인증 확인      |                  |
  |                   |                  | 2. 문서 조회 + 소유권 확인               |
  |                   |                  | 3. 입력 검증          |                  |
  |                   |                  | 4. HTML 새니타이징     |                  |
  |                   |                  |                      |                  |
  |                   |                  | 5. R2 덮어쓰기        |                  |
  |                   |                  | --------------------->|                 |
  |                   |                  | <---------------------|                 |
  |                   |                  |                      |                  |
  |                   |                  | 6. DB UPDATE (트랜잭션)                  |
  |                   |                  |    documents + storage_usage             |
  |                   |                  | ---------------------------------------->|
  |                   |                  | <----------------------------------------|
  |                   |                  |                      |                  |
  |                   | { url, slug, updatedAt, ... }          |                  |
  |                   | <----------------|                     |                  |
  | <-----------------|                  |                      |                  |
```

### 6.4 문서 삭제

```
1. JWT 인증 + 소유권 확인
2. R2 파일 삭제
3. DB 트랜잭션:
   - documents.status = 'DELETED', updatedAt = now
   - storage_usage: totalBytes 감소, documentCount 감소
4. 응답 반환
```

### 6.5 만료 문서 정리 (Spring @Scheduled)

기존 C-3(CRON 인증) 검토를 반영한다. Vercel Cron 대신 Spring의 `@Scheduled`를 사용하므로 외부 HTTP 호출이 불필요하여 인증 문제가 자연스럽게 해결된다.

```
@Scheduled(fixedRate = 3600000)  // 매 1시간
cleanupExpiredDocuments()
  |
  | 1. PENDING 문서 정리 (5분 초과)
  |    SELECT * FROM documents
  |    WHERE status = 'PENDING'
  |    AND created_at < NOW() - INTERVAL '5 minutes'
  |    LIMIT 100
  |    -> 각 문서: R2 파일 삭제 시도 + DB DELETE
  |
  | 2. 만료 문서 처리
  |    SELECT * FROM documents
  |    WHERE status = 'ACTIVE'
  |    AND expires_at IS NOT NULL
  |    AND expires_at < NOW()
  |    LIMIT 100
  |    -> 각 문서: R2 파일 삭제 + status = 'EXPIRED'
  |
  | 3. 완전 삭제 (30일 이상 EXPIRED/DELETED)
  |    DELETE FROM documents
  |    WHERE status IN ('EXPIRED', 'DELETED')
  |    AND updated_at < NOW() - INTERVAL '30 days'
```

### 6.6 비로그인 -> 로그인 시 문서 소유권 이전

MVP에서의 전략: **소유권 이전을 지원하지 않는다.**

근거:
- 비로그인 문서는 24시간 만료이므로 장기 보존 필요성이 낮다.
- 소유권 이전 구현 시 프론트엔드(쿠키/localStorage)와 백엔드(JWT) 양쪽에 걸치는 복잡한 로직 필요.
- 대신 UI에서 "로그인하면 문서가 영구 보존됩니다" 안내를 노출하여 로그인 후 문서를 새로 생성하도록 유도한다.
- 문서 생성 완료 모달에 "비로그인 문서는 대시보드에서 관리되지 않습니다" 안내를 포함한다 (기존 S-7 반영).

### 6.7 문서 상태 전이 다이어그램

```
                    +------------------+
                    |                  |
       생성 ------> |     pending      |
                    |                  |
                    +------+-----------+
                           |
              R2 업로드 성공 |
              + DB UPDATE   |
                           v
                    +------------------+
                    |                  |
                    |     active       |
                    |                  |
                    +------+-------+---+
                           |       |
              만료 도달     |       |  사용자 삭제
              (Scheduled)  |       |  (DELETE API)
                           |       |
                           v       v
                    +----------+  +----------+
                    | expired  |  | deleted   |
                    +----+-----+  +----+------+
                         |             |
                         |  30일 경과   |  30일 경과
                         |  (Scheduled)|  (Scheduled)
                         v             v
                    +------------------+
                    |  DB 레코드 삭제   |
                    |  (물리 삭제)      |
                    +------------------+

[PENDING 5분 초과 시]
  pending --> DB DELETE + R2 파일 삭제 (스케줄러)
```

---

## 7. 데이터 모델 (DB 스키마)

### 7.1 ERD

```
+------------------------+       +------------------------+
|        users            |       |     storage_usage       |
+------------------------+       +------------------------+
| id         UUID    PK   |<--+  | user_id  UUID  PK,FK   |
| email      TEXT    UQ   |   +--| (FK -> users.id)       |
| name       TEXT         |      | total_bytes  BIGINT    |
| profile_image TEXT      |      | document_count INT     |
| provider   TEXT         |      | updated_at   TSTZ      |
| provider_id TEXT        |      +------------------------+
| plan       TEXT         |
| created_at TSTZ         |      +------------------------+
| updated_at TSTZ         |      |   refresh_tokens        |
+--------+---------------+      +------------------------+
         | 1                     | id         UUID  PK    |
         |                       | user_id    UUID  FK    |
         | 0..N                  | token      TEXT  UQ    |
+--------+---------------+      | is_revoked BOOLEAN     |
|       documents         |      | expires_at TSTZ        |
+------------------------+      | created_at TSTZ        |
                                +------------------------+
| id         TEXT    PK   |  -- nanoid 8자리
| slug       TEXT    UQ   |  -- = id
| user_id    UUID    FK   |  -- NULL = 비로그인
| title      TEXT         |
| doc_type   TEXT         |  -- 'html' | 'markdown'
| r2_key     TEXT         |
| content_size BIGINT     |
| status     TEXT         |  -- 'pending'|'active'|'expired'|'deleted'
| expires_at TSTZ         |  -- 비로그인: +24h
| created_at TSTZ         |
| updated_at TSTZ         |
+------------------------+
```

### 7.2 DDL (Flyway 마이그레이션)

```sql
-- V1__init.sql

CREATE TABLE users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           TEXT NOT NULL UNIQUE,
  name            TEXT,
  profile_image   TEXT,
  provider        TEXT NOT NULL,
  provider_id     TEXT NOT NULL,
  plan            TEXT NOT NULL DEFAULT 'free',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (provider, provider_id)
);

CREATE TABLE documents (
  id              TEXT PRIMARY KEY,
  slug            TEXT NOT NULL UNIQUE,
  user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
  title           TEXT,
  doc_type        TEXT NOT NULL,
  r2_key          TEXT NOT NULL,
  content_size    BIGINT NOT NULL,
  status          TEXT NOT NULL DEFAULT 'pending',
  expires_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE storage_usage (
  user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  total_bytes     BIGINT NOT NULL DEFAULT 0,
  document_count  INTEGER NOT NULL DEFAULT 0,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token           TEXT NOT NULL UNIQUE,
  is_revoked      BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at      TIMESTAMPTZ NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_documents_user_id ON documents(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_expires_at ON documents(expires_at)
  WHERE expires_at IS NOT NULL AND status = 'active';
CREATE INDEX idx_documents_pending_cleanup ON documents(created_at)
  WHERE status = 'pending';
CREATE INDEX idx_documents_user_status_updated ON documents(user_id, status, updated_at DESC)
  WHERE user_id IS NOT NULL;
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

### 7.3 인덱스 용도

| 인덱스 | 용도 |
|--------|------|
| `idx_documents_user_id` | 내 문서 목록 조회 |
| `idx_documents_status` | 상태별 문서 필터링 |
| `idx_documents_expires_at` | 스케줄러 만료 문서 검색 |
| `idx_documents_pending_cleanup` | 스케줄러 PENDING 문서 정리 |
| `idx_documents_user_status_updated` | 대시보드 정렬 조회 최적화 |
| `idx_refresh_tokens_user_id` | 사용자별 리프레시 토큰 조회 |
| `idx_refresh_tokens_token` | 토큰 값으로 조회 |

---

## 8. R2 버킷 구조

### 8.1 버킷 구조

```
r2-bucket: drafturl-files
|
+-- documents/
    +-- {document_id}/
    |   +-- content.html          <- HTML 문서
    +-- {document_id}/
    |   +-- content.md            <- Markdown 문서
    +-- ...
```

### 8.2 키 네이밍 규칙

```
documents/{document_id}/content.{ext}
```

- `document_id`: nanoid 8자리 (예: `xK9mP2nQ`)
- `ext`: `html` 또는 `md`
- 예시: `documents/xK9mP2nQ/content.html`

### 8.3 R2 접근 방식

R2를 퍼블릭 버킷으로 설정하지 않는다. Spring Boot API를 통해서만 파일을 읽고 쓴다.

이유:
- 만료 체크, 접근 제어 등 비즈니스 로직을 서버에서 처리해야 함
- 퍼블릭 버킷 설정 시 만료된 문서도 직접 접근 가능한 보안 이슈

Spring Boot에서 AWS SDK for Java v2의 `S3Client`를 사용하여 R2에 접근한다. R2는 S3 호환 API를 제공하므로 별도 SDK가 불필요하다.

```
// R2 접속 설정 (application.yml 기반)
r2.endpoint:     https://{account_id}.r2.cloudflarestorage.com
r2.bucket-name:  drafturl-files
r2.access-key:   ${R2_ACCESS_KEY}
r2.secret-key:   ${R2_SECRET_KEY}
r2.region:       auto
```

---

## 9. 프론트엔드 설계 (Next.js)

### 9.1 역할 재정의

Next.js는 UI 렌더링과 Spring Boot API 호출만 담당한다. **Server Components에서 직접 DB/R2에 접근하지 않는다.**

| 역할 | O/X | 설명 |
|------|-----|------|
| UI 렌더링 | O | React Server/Client Components |
| 정적 페이지 | O | 랜딩, 만료 안내 등 |
| Spring Boot API 호출 | O | 서버/클라이언트 모두에서 |
| 직접 DB 접근 | X | 모든 데이터는 Spring Boot API 경유 |
| 직접 R2 접근 | X | 모든 파일 접근은 Spring Boot API 경유 |
| OAuth2 리다이렉트 처리 | O | 인가 코드를 받아 Spring Boot에 전달 |
| JWT 토큰 관리 | O | 쿠키 저장, 자동 갱신 |

### 9.2 API 클라이언트 레이어

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

### 9.3 페이지/라우트 구조

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

### 9.4 주요 페이지 상세

#### 메인 페이지 (`app/page.tsx`)

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

#### 문서 서빙 페이지 (`app/[slug]/page.tsx`)

역할: 공유 URL로 접근 시 문서를 렌더링한다.

서버 컴포넌트에서 Spring Boot `GET /api/v1/documents/{slug}/view` 호출:
- 200: 문서 렌더링 (HTML이면 sandbox iframe, MD이면 unified 변환)
- 404: `notFound()` 호출
- 410: 만료 안내 UI 인라인 렌더링 (리다이렉트 대신 같은 페이지에서 처리하여 slug 정보 유지, 기존 W-6 반영)

#### OAuth2 콜백 페이지 (`app/auth/callback/page.tsx`)

역할: OAuth2 인가 코드를 받아 state를 검증한 후 Spring Boot에 전달하고 JWT를 수신한다.

1. URL 쿼리 파라미터에서 `code`, `state` 추출
2. **`oauth_state` 쿠키에서 저장된 state 값을 읽어 URL의 state와 비교 검증**
3. **불일치 시 `/auth/error`로 리다이렉트 (CSRF 의심)**
4. Spring Boot `POST /api/v1/auth/oauth2/callback/{provider}` 호출 (code, redirectUri, state 전달)
5. 응답의 `accessToken`, `refreshToken`을 쿠키에 저장
6. `oauth_state` 쿠키 삭제 (재사용 방지)
7. 대시보드 또는 이전 페이지로 리다이렉트

#### 대시보드 (`app/dashboard/page.tsx`)

인증 가드: `middleware.ts`에서 `/dashboard/*` 경로에 대해 JWT 쿠키 존재 여부를 확인하여 미인증 시 `/auth/login`으로 리다이렉트한다. 레이아웃에서 중복 확인하지 않는다 (기존 S-5 반영).

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

[링크] 버튼으로 공유 URL 복사 기능 제공 (기존 W-4 반영).

### 9.5 프론트엔드 디렉토리 구조

```
src/
+-- app/                          -- Next.js App Router 페이지/라우트
|   +-- (위 9.3 구조)
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

## 10. 백엔드 설계 (Spring Boot)

### 10.1 패키지 구조

```
src/main/java/com/drafturl/api/
+-- DrafturlApiApplication.java          -- Spring Boot 메인 클래스
+-- config/
|   +-- SecurityConfig.java              -- Spring Security 설정 (필터 체인, CORS)
|   +-- R2Config.java                    -- R2 S3Client 빈 설정
|   +-- JwtConfig.java                   -- JWT 관련 설정값 (비밀키, 만료시간)
|   +-- WebConfig.java                   -- 기타 웹 설정
+-- security/
|   +-- JwtProvider.java                 -- JWT 생성/검증 유틸
|   +-- JwtAuthenticationFilter.java     -- OncePerRequestFilter, JWT 인증
|   +-- RateLimitFilter.java             -- Rate Limiting 필터
|   +-- UserPrincipal.java               -- Authentication에 담길 사용자 정보
+-- controller/
|   +-- DocumentController.java          -- 문서 CRUD REST Controller
|   +-- AuthController.java              -- 인증 REST Controller
+-- service/
|   +-- DocumentService.java             -- 문서 비즈니스 로직
|   +-- StorageUsageService.java         -- 사용량 추적 로직
|   +-- AuthService.java                 -- OAuth2 인증, JWT 발급 로직
|   +-- R2StorageService.java            -- R2 파일 업로드/다운로드/삭제
|   +-- HtmlSanitizer.java              -- HTML 새니타이징
|   +-- SlugGenerator.java              -- nanoid 호환 slug 생성 (SecureRandom 직접 구현)
+-- repository/
|   +-- UserRepository.java             -- Spring Data JPA (User)
|   +-- DocumentRepository.java         -- Spring Data JPA (Document)
|   +-- StorageUsageRepository.java     -- Spring Data JPA (StorageUsage)
|   +-- RefreshTokenRepository.java     -- Spring Data JPA (RefreshToken)
+-- entity/
|   +-- UserEntity.java                 -- JPA 엔티티
|   +-- DocumentEntity.java             -- JPA 엔티티
|   +-- StorageUsageEntity.java         -- JPA 엔티티
|   +-- RefreshTokenEntity.java         -- JPA 엔티티
+-- dto/
|   +-- request/
|   |   +-- CreateDocumentRequest.java  -- 문서 생성 요청 DTO
|   |   +-- UpdateDocumentRequest.java  -- 문서 수정 요청 DTO
|   |   +-- OAuthCallbackRequest.java   -- OAuth 콜백 요청 DTO
|   |   +-- RefreshTokenRequest.java    -- 토큰 갱신 요청 DTO
|   +-- response/
|       +-- ApiResponse.java            -- 공통 응답 래퍼 {success, data/error}
|       +-- DocumentResponse.java       -- 문서 응답 DTO
|       +-- DocumentListResponse.java   -- 문서 목록 응답 DTO
|       +-- DocumentViewResponse.java   -- 문서 서빙 응답 DTO
|       +-- AuthResponse.java           -- 인증 응답 DTO (JWT + 사용자 정보)
|       +-- UserResponse.java           -- 사용자 정보 응답 DTO
|       +-- ErrorResponse.java          -- 에러 응답 DTO
+-- exception/
|   +-- GlobalExceptionHandler.java     -- @RestControllerAdvice
|   +-- BusinessException.java          -- 비즈니스 예외 기본 클래스
|   +-- DocumentNotFoundException.java
|   +-- DocumentExpiredException.java
|   +-- ForbiddenException.java
|   +-- ContentTooLargeException.java
|   +-- RateLimitExceededException.java
+-- scheduler/
|   +-- DocumentCleanupScheduler.java   -- @Scheduled 만료 문서 정리
+-- enums/
    +-- DocType.java
    +-- DocumentStatus.java
    +-- PlanType.java

src/main/resources/
+-- application.yml                      -- 기본 설정
+-- application-local.yml                -- 로컬 개발 설정
+-- application-prod.yml                 -- 프로덕션 설정
+-- db/migration/
    +-- V1__init.sql                     -- Flyway 초기 마이그레이션
```

### 10.2 주요 설정 (application.yml)

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate      # Flyway가 DDL 관리, Hibernate는 검증만
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

# R2 설정
r2:
  endpoint: ${R2_ENDPOINT}
  bucket-name: ${R2_BUCKET_NAME}
  access-key: ${R2_ACCESS_KEY}
  secret-key: ${R2_SECRET_KEY}
  region: auto

# JWT 설정
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 3600         # 1시간 (초)
  refresh-token-expiry: 604800      # 7일 (초)

# OAuth2 설정
oauth2:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    token-url: https://oauth2.googleapis.com/token
    user-info-url: https://www.googleapis.com/oauth2/v2/userinfo
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    token-url: https://github.com/login/oauth/access_token
    user-info-url: https://api.github.com/user

# 서비스 설정
app:
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  max-content-size: 5242880          # 5MB
  slug-length: 8
  anonymous-expiry-hours: 24
```

### 10.3 핵심 서비스 로직 요약

#### DocumentService

```
createDocument(request, userId):
  1. 입력 검증 (Bean Validation 이후 추가 비즈니스 검증)
  2. slug 생성 (nanoid 8자리, 충돌 시 최대 3회 재시도)
  3. HTML이면 새니타이징
  4. DB INSERT (status=PENDING)
  5. R2 업로드
  6. DB UPDATE (status=ACTIVE) + StorageUsage UPDATE (트랜잭션)
  7. 응답 반환

getDocumentForView(slug):
  1. slug로 문서 조회
  2. 상태 확인 (ACTIVE만 서빙, EXPIRED/DELETED -> 410)
  3. 만료 시간 확인 (expiresAt != null && expiresAt < now -> 410 반환)
     * DB UPDATE 없음 (읽기 전용). status=EXPIRED 변경은 스케줄러에 위임.
     * 만료 판단은 항상 expiresAt 값 기준으로 수행한다.
  4. R2에서 파일 로드
  5. content + metadata 반환

updateDocument(slug, request, userId):
  1. 문서 조회 + 소유권 확인
  2. 입력 검증
  3. HTML이면 새니타이징
  4. R2 파일 덮어쓰기
  5. DB UPDATE + StorageUsage UPDATE (트랜잭션)

deleteDocument(slug, userId):
  1. 문서 조회 + 소유권 확인
  2. R2 파일 삭제
  3. DB soft delete (status=DELETED) + StorageUsage UPDATE (트랜잭션)
```

#### 트랜잭션 범위 명시

트랜잭션은 DB 연산 범위에 한정되며, R2와의 원자성은 보장되지 않는다 (기존 S-6 반영). 구체적으로:

- `documents` INSERT/UPDATE + `storage_usage` UPDATE = 하나의 `@Transactional`
- R2 업로드/삭제는 트랜잭션 외부에서 실행
- R2와 DB 간 불일치는 `PENDING` 상태 + 스케줄러 정리로 최종적 일관성(eventual consistency) 보장

---

## 11. 보안

### 11.1 입력 검증

| 항목 | 검증 내용 | 구현 위치 |
|------|----------|-----------|
| content 크기 | 최대 5MB | Bean Validation `@Size` + Spring `maxRequestSize` |
| doc_type | `html` 또는 `markdown` | Bean Validation `@Pattern` |
| Rate Limiting | IP 기반 분당 10/30회 | `RateLimitFilter` |

### 11.2 HTML 서빙 보안

| 위협 | 대응책 |
|------|--------|
| XSS (사용자 HTML 내 스크립트) | `sandbox="allow-scripts"` iframe 격리 (allow-same-origin 제거) |
| 쿠키 탈취 | sandbox iframe의 origin 격리 + Phase 2에서 서빙 도메인 분리 |
| 외부 데이터 유출 (iframe 내 fetch/XHR) | CSP 헤더로 외부 네트워크 요청 차단: `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:` |
| 악성 HTML (iframe, object, embed) | 서버 새니타이징: `<iframe>`, `<object>`, `<embed>` 태그 제거 |
| javascript: URL 스킴 | 서버 새니타이징: `href="javascript:..."` 제거 |

### 11.3 인증 보안

| 위협 | 대응책 |
|------|--------|
| JWT 탈취 | `httpOnly` + `Secure` + `SameSite=Lax` 쿠키 저장 |
| CSRF (일반) | `SameSite=Lax` 쿠키 + JWT Bearer 토큰 이중 확인 |
| CSRF (OAuth2) | `state` 파라미터 생성/쿠키 저장/검증 (섹션 5.1.1) |
| 리프레시 토큰 재사용 | Refresh Token Rotation: 갱신 시 이전 토큰 즉시 revoke + 새 토큰 발급 (섹션 5.3.1). revoked 토큰 재사용 시 해당 사용자 전체 토큰 무효화 (탈취 감지) |
| 브루트포스 | Rate Limiting |

### 11.4 비로그인 문서 보안

- 24시간 자동 만료 (expires_at)
- slug가 nanoid 8자리 (경우의 수: 2.8조 개)로 URL 추측 불가
- 스케줄러로 만료 문서 주기적 삭제 (R2 파일 + DB 레코드)

---

## 12. 비기능 요구사항 대응

### 12.1 성능

| 항목 | 목표 | 전략 |
|------|------|------|
| 문서 서빙 응답 시간 | < 800ms (캐시 미스), < 100ms (Next.js 캐시 히트) | Spring Boot 응답 캐싱 헤더 + Next.js ISR/fetch 캐시 |
| 문서 생성 응답 시간 | < 1.5s | 2단계 커밋이지만 단순 연산이므로 충분 |
| 에디터 로딩 | < 2s | Monaco Editor 동적 import + 코드 스플리팅 |
| 미리보기 갱신 | < 300ms (타이핑 후) | debounce 300ms, 클라이언트 렌더링 (API 호출 불필요) |
| Spring Boot 콜드 스타트 | < 5s | Railway 상시 가동, Spring Boot 최적화 |

캐싱 전략:
- 문서 서빙 API(`/view`) 응답에 `Cache-Control` 헤더 설정
- Next.js 서버 컴포넌트에서 `fetch` 호출 시 `next: { revalidate: 300 }` 옵션으로 ISR 캐싱
- 문서 수정 시에는 Next.js의 `revalidatePath`/`revalidateTag`로 캐시 무효화

### 12.2 확장성

| 항목 | MVP | 확장 시 |
|------|-----|---------|
| FE | Vercel (자동 스케일) | 동일 |
| BE | Railway 단일 인스턴스 | Railway Pro (수평 확장) 또는 Fly.io |
| DB | Supabase Free (500MB) | Supabase Pro (8GB+) |
| 파일 스토리지 | R2 (10GB 무료) | R2 (자동 스케일) |
| Rate Limiting | 인메모리 ConcurrentHashMap | Redis (Upstash) |
| 스케줄러 | @Scheduled (단일 인스턴스) | ShedLock + DB 기반 분산 락 |

---

## 13. 인프라/배포 설계

### 13.1 배포 아키텍처

```
GitHub Repository
  |
  +-- .github/workflows/
  |     +-- frontend-deploy.yml    -- Next.js -> Vercel
  |     +-- backend-deploy.yml     -- Spring Boot -> Railway
  |
  +-- frontend/                     -- Next.js 프로젝트
  |     +-- Vercel 자동 배포 (GitHub 연동)
  |
  +-- backend/                      -- Spring Boot 프로젝트
        +-- Dockerfile
        +-- Railway 배포 (Dockerfile 기반)
```

모노레포 구조를 사용하여 `frontend/`와 `backend/`를 하나의 저장소에서 관리한다.

### 13.2 Railway Dockerfile (Spring Boot)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

GitHub Actions에서 Gradle 빌드 후 JAR를 생성하고, Railway에 Docker 이미지로 배포한다.

### 13.3 환경변수 관리

| 변수 | 위치 | 용도 |
|------|------|------|
| `DATABASE_URL` | Railway | PostgreSQL 연결 |
| `R2_ENDPOINT` | Railway | Cloudflare R2 엔드포인트 |
| `R2_ACCESS_KEY` | Railway | R2 인증 |
| `R2_SECRET_KEY` | Railway | R2 인증 |
| `R2_BUCKET_NAME` | Railway | R2 버킷명 |
| `JWT_SECRET` | Railway | JWT 서명 비밀키 |
| `GOOGLE_CLIENT_ID` | Railway | Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | Railway | Google OAuth2 |
| `GITHUB_CLIENT_ID` | Railway | GitHub OAuth2 |
| `GITHUB_CLIENT_SECRET` | Railway | GitHub OAuth2 |
| `FRONTEND_URL` | Railway | CORS 허용 origin |
| `NEXT_PUBLIC_API_URL` | Vercel | Spring Boot API URL |

### 13.4 비용 추산

#### MVP 출시 후 1~2개월 (일 100개 문서 가정)

| 항목 | 비용 | 비고 |
|------|------|------|
| **Vercel (FE)** | **$0** | Hobby 플랜 |
| **Railway (BE)** | **$5/월** | Hobby 플랜 (512MB RAM, 상시 가동) |
| **Supabase PostgreSQL** | **$0** | Free 티어 (500MB DB, 50K 행) |
| **Cloudflare R2 저장** | **$0** | 50MB << 10GB 무료 |
| **R2 쓰기 (Class A)** | **$0** | 월 ~3,000회 << 1M 무료 |
| **R2 읽기 (Class B)** | **$0** | 월 ~10,000회 << 10M 무료 |
| **R2 이그레스** | **$0** | 항상 무료 |
| **도메인** | **~$10/년** | .com 도메인 |
| **총합** | **~$5/월** | 기존 $0에서 백엔드 호스팅 비용 추가 |

#### 스케일 시 (사용자 1,000명, 프로덕션)

| 항목 | 비용 |
|------|------|
| Vercel Pro (FE) | $20/월 |
| Railway Pro (BE) | $20/월 (1GB RAM, 자동 스케일) |
| Supabase Pro (DB) | $25/월 |
| R2 | $0 (10GB 미만) |
| **총합** | **~$65/월** |

기존 설계 대비 월 $20 증가 (백엔드 호스팅 비용). 그러나 Spring Boot의 안정성, Java 생태계의 풍부한 라이브러리, 백엔드 독립적 스케일링의 이점을 고려하면 합리적이다.

---

## 14. 구현 가이드

### 14.1 구현 순서 권장사항

```
Phase 1 (Day 1-3): 백엔드 기반 설정
  1. Spring Boot 프로젝트 생성 (Spring Initializr)
     - Spring Web, Spring Security, Spring Data JPA, Validation, Flyway
  2. Gradle 의존성 설정 (PostgreSQL, AWS SDK, jjwt 등, nanoid는 직접 구현)
  3. Flyway 마이그레이션 (V1__init.sql)
  4. Supabase PostgreSQL 연결 설정
  5. R2 S3Client 빈 설정
  6. 공통 응답 래퍼 (ApiResponse) + 글로벌 예외 핸들러
  7. JwtProvider + JwtAuthenticationFilter
  8. SecurityConfig (필터 체인 + CORS)
  9. RateLimitFilter

Phase 2 (Day 4-6): 백엔드 핵심 기능
  10. Entity + Repository 클래스
  11. SlugGenerator (nanoid)
  12. HtmlSanitizer
  13. R2StorageService (upload, download, delete)
  14. DocumentService (생성 -- 2단계 커밋)
  15. DocumentController -- POST /api/v1/documents
  16. DocumentService (서빙 -- view)
  17. DocumentController -- GET /api/v1/documents/{slug}/view
  18. AuthService (OAuth2 콜백 -> JWT 발급)
  19. AuthController

Phase 3 (Day 7-8): 백엔드 관리 기능
  20. DocumentService (목록, 상세, 수정, 삭제)
  21. DocumentController -- GET, PUT, DELETE
  22. StorageUsageService
  23. DocumentCleanupScheduler (@Scheduled)

Phase 4 (Day 9-11): 프론트엔드
  24. Next.js 15 프로젝트 생성 (Tailwind + shadcn/ui)
  25. API 클라이언트 레이어 (client.ts, auth.ts, documents.ts)
  26. middleware.ts (인증 가드 + 토큰 갱신)
  27. 메인 페이지 (에디터 + 미리보기 + 공유하기)
  28. 문서 서빙 페이지 ([slug])
  29. OAuth2 로그인 + 콜백 페이지
  30. 대시보드 (문서 목록 + URL 복사 + 편집 + 삭제)
  31. 문서 편집 페이지
  32. 만료/404/에러 페이지

Phase 5 (Day 12-14): 통합 + 배포
  33. 프론트엔드 <-> 백엔드 통합 테스트
  34. Railway 배포 설정 (Dockerfile)
  35. Vercel 배포 설정
  36. 환경변수 설정 (프로덕션)
  37. 도메인 연결
  38. E2E 테스트 (핵심 플로우)
  39. 모바일 반응형 점검
  40. 소프트 런칭
```

### 14.2 주의사항

| 항목 | 주의사항 |
|------|----------|
| Spring Boot 콜드 스타트 | Railway에서 상시 가동 설정 확인. 슬립 모드가 활성화되면 첫 요청 시 10-15초 지연 발생 가능 |
| CORS | 프론트엔드 도메인을 정확히 지정. 와일드카드(`*`) 사용 금지 (credentials 포함 요청과 호환 불가) |
| JWT 비밀키 | 최소 256비트(32바이트) 이상의 안전한 랜덤 값 사용. 환경변수로 관리하고 코드에 하드코딩 금지 |
| nanoid 충돌 | 8자리 nanoid의 충돌 확률은 극히 낮으나, DB INSERT 시 unique constraint 위반 시 최대 3회 재시도 |
| R2 + DB 정합성 | PENDING 상태 도입으로 고아 파일 문제를 완화했으나, 스케줄러가 정상 동작하는지 모니터링 필요 |
| HTML 새니타이징 | Java에서 정규식 기반 경량 처리. jsoup 등 HTML 파서 라이브러리 사용도 검토 가능하나 MVP에서는 정규식으로 충분 |
| Monaco Editor | Next.js에서 `next/dynamic`으로 SSR: false 동적 import 필수 |
| 미리보기 debounce | 300ms debounce 적용. 클라이언트에서 처리하므로 API 호출 불필요 |
| 파일 읽기 인코딩 | 프론트엔드에서 `FileReader.readAsText(file, 'utf-8')`로 통일 |
| API 버전 | `/api/v1/` 접두사를 사용하여 향후 breaking change 시 `/api/v2/` 병행 가능 |

---

## 15. 계획 문서 커버리지 자체 검증

| 계획서 요구사항 | 설계 반영 여부 | 비고 |
|----------------|---------------|------|
| HTML/MD 텍스트 붙여넣기 배포 | O | 6.1 문서 생성, POST /api/v1/documents |
| 단일 파일 업로드 배포 | O | 클라이언트에서 텍스트로 읽어 동일 API |
| 실시간 미리보기 | O | 9.4 PreviewPanel (클라이언트 iframe/unified) |
| 고유 URL 생성 | O | nanoid 8자리, slug = id |
| 정적 파일 서빙 | O | 6.2 서빙 플로우, GET /api/v1/documents/{slug}/view |
| 비로그인 임시 배포 (24h) | O | DR-1, expiresAt, 스케줄러 정리 |
| 사용자 인증 (Google, GitHub) | O | Spring Security + OAuth2 + JWT, 5장 |
| 내 문서 목록 | O | GET /api/v1/documents, 9.4 대시보드 |
| 문서 수정/재배포 | O | PUT /api/v1/documents/{slug} |
| 문서 삭제 | O | DELETE /api/v1/documents/{slug} |
| MD 기본 테마 렌더링 | O | 프론트엔드 unified + 기본 테마 |
| Rate Limiting | O | RateLimitFilter, IP 기반 |
| HTML 새니타이징 | O | HtmlSanitizer, iframe/object/embed + javascript: 제거 |
| 만료 문서 정리 | O | DocumentCleanupScheduler (@Scheduled) |
| DB 스키마 (4개 테이블) | O | users, documents, storage_usage, refresh_tokens |
| R2 비공개 버킷 + 서버 경유 | O | R2StorageService, AWS SDK for Java |
| AI 수정 기능 제외 | O | 범위 외 명시 |
| 버전 히스토리 제외 | O | 범위 외 명시 |
| ZIP/멀티파일 제외 | O | 범위 외 명시 |
| 비밀번호 보호 제외 | O | 범위 외 명시 |

### 기존 검토 결과(Critical 3건) 반영 확인

| 검토 ID | 내용 | 반영 위치 |
|---------|------|-----------|
| C-1 | 고아 R2 파일 -> pending/active 2단계 커밋 | 3.2 Document 엔티티 status에 PENDING 추가, 6.1 2단계 커밋 시퀀스, 6.5 스케줄러 PENDING 정리, DR-9 |
| C-2 | sandbox allow-same-origin 제거 | 6.2 HTML 서빙 래퍼에서 `sandbox="allow-scripts"`만 사용, 트레이드오프 명시 |
| C-3 | CRON 엔드포인트 인증 | Spring @Scheduled 사용으로 외부 HTTP 엔드포인트 불필요, 인증 문제 자연 해결 |

---

## 16. 설계 결정 및 트레이드오프 기록

### 결정 1: 프론트엔드/백엔드 분리 아키텍처

- **선택**: Next.js (FE) + Spring Boot (BE) 분리
- **대안**: Next.js 풀스택 (API Routes + Server Components)
- **이유**: 백엔드 개발자의 기술 스택(Spring Boot/Java)을 활용하여 생산성 극대화. 백엔드 독립적 스케일링 가능. 향후 모바일 앱 등 다른 클라이언트 추가 시 API 재사용 가능.
- **트레이드오프**: 호스팅 비용 월 $5 증가. 배포 복잡도 증가 (두 개 서비스 관리). 프론트엔드-백엔드 간 네트워크 레이턴시 추가.

### 결정 2: JWT 인증 (NextAuth.js 대체)

- **선택**: Spring Security + 자체 JWT 발급 + OAuth2 클라이언트
- **대안**: NextAuth.js 유지 + Spring Boot에서 세션 검증
- **이유**: 백엔드가 인증의 단일 진실 공급원(Single Source of Truth)이 되어 아키텍처가 깔끔해진다. Next.js는 토큰을 중계하는 역할만 한다.
- **트레이드오프**: OAuth2 콜백 처리를 직접 구현해야 한다 (NextAuth.js의 편의성 상실). 리프레시 토큰 관리를 위한 DB 테이블 추가.

### 결정 3: Railway 선택

- **선택**: Railway Hobby ($5/월)
- **대안**: Fly.io Free, Render Free
- **이유**: 슬립 없이 상시 가동. Spring Boot 콜드 스타트(10-15초)로 인한 UX 저하 방지. 간편한 배포/관리.
- **트레이드오프**: 월 $5 비용 발생 (Render Free는 $0이나 15분 비활성 슬립).

### 결정 4: 문서 타입 변경 불허 (MVP)

- **선택**: PUT 요청에서 `type` 필드를 허용하지 않음
- **대안**: 타입 전환 시 기존 R2 파일 삭제 + 새 키로 업로드
- **이유**: R2 키가 확장자를 포함하므로 타입 변환 시 R2 파일 관리 복잡도 증가. MVP 단순화 우선.
- **Phase 2 검토**: 사용자 피드백에 따라 추가

### 결정 5: sandbox에서 allow-same-origin 제거

- **선택**: `sandbox="allow-scripts"`만 사용
- **대안**: `sandbox="allow-scripts allow-same-origin"` + 서빙 도메인 분리를 MVP에 포함
- **이유**: `allow-scripts` + `allow-same-origin` 조합은 동일 도메인에서 sandbox를 무력화한다. 보안이 LLM HTML 호환성보다 우선.
- **트레이드오프**: `localStorage`, `sessionStorage`, `fetch` with credentials 등 일부 LLM 생성 HTML 기능이 제한됨. 대부분의 LLM 출력물(표, 차트, 스타일링된 문서)은 영향 없음.

### 결정 6: id와 slug 동일 유지 (기존 설계 계승)

- **선택**: nanoid 8자리 = document.id = document.slug
- **대안**: UUID id + 별도 slug, 또는 slug 컬럼 제거
- **이유**: 기존 설계 계승. Phase 2 커스텀 slug 지원 시 slug 컬럼을 업데이트하고 id는 유지.

---

## Sources

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [AWS SDK for Java v2 - S3](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html)
- [Cloudflare R2 - S3 API Compatibility](https://developers.cloudflare.com/r2/api/s3/)
- [Railway Pricing](https://railway.app/pricing)
- [Fly.io Pricing](https://fly.io/docs/about/pricing/)
- [Render Pricing](https://render.com/pricing)
- [Cloudflare R2 Pricing](https://developers.cloudflare.com/r2/pricing/)
- [Supabase Pricing](https://supabase.com/pricing)
- [nanoid](https://github.com/ai/nanoid)
- [unified (remark/rehype)](https://unifiedjs.com/)

---

## 검토 결과

> 검토일: 2026-03-21
> 검토 대상: docs/design/mvp-service-design.md (Spring Boot + Next.js 분리 아키텍처)
> 참조 문서: docs/plans/service-plan.md, docs/plans/file-storage/file-upload-storage-plan.md

### 이전 검토 결과(Critical 3건) 반영 확인

이전 검토에서 식별된 Critical 3건이 모두 적절히 반영되었다.

| 검토 ID | 이전 문제 | 반영 방법 | 평가 |
|---------|----------|-----------|------|
| C-1 | 고아 R2 파일 (R2 업로드 성공 후 DB INSERT 실패) | PENDING/ACTIVE 2단계 커밋 + 스케줄러 정리 (섹션 6.1, 6.5, DR-9) | 완전 반영 |
| C-2 | `allow-same-origin` sandbox 무력화 | `sandbox="allow-scripts"`만 사용, 트레이드오프 명시 (섹션 6.2) | 완전 반영 |
| C-3 | CRON 엔드포인트 외부 HTTP 인증 누락 | Spring `@Scheduled`로 전환하여 외부 엔드포인트 자체를 제거 (섹션 6.5) | 구조적으로 해결 |

이전 Warning 6건, Suggestion 7건 반영 여부도 각 섹션에 "(기존 W-N 반영)", "(기존 S-N 반영)" 주석으로 명시되어 있어 추적 가능성이 높다.

---

### Critical (치명적 문제)

#### C-1: OAuth2 state 파라미터 검증 로직 명세 누락 (CSRF 취약점)

**위치**: 섹션 5.1 OAuth2 인증 플로우, 섹션 4.4 `POST /api/v1/auth/oauth2/callback/{provider}`

**문제**: 시퀀스 다이어그램에서 `state={csrf_token}`을 OAuth2 요청에 포함한다고 명시했지만, Spring Boot의 콜백 핸들러(`POST /api/v1/auth/oauth2/callback/{provider}`)가 `state` 파라미터를 수신하고 검증하는 방법이 전혀 설계되어 있지 않다.

현재 콜백 API의 요청 body:
```json
{
  "code": "authorization_code_from_oauth_provider",
  "redirectUri": "https://{서비스도메인}/auth/callback"
}
```

`state` 필드가 없다. `state`를 검증하지 않으면 공격자가 피해자를 자신의 code로 로그인시켜 세션을 탈취하는 CSRF 공격(OAuth2 인가 코드 주입)이 가능하다.

**개선 방법**:
1. 로그인 시작 시 Next.js에서 cryptographically random `state` 값을 생성하여 `httpOnly` 세션 쿠키에 저장한다.
2. OAuth2 요청 URL에 `state` 파라미터를 포함한다.
3. 콜백 시 URL의 `state`와 쿠키의 `state`를 비교하여 일치하지 않으면 요청을 거부한다.
4. 콜백 API 요청 body에 `state` 필드를 추가하고, `OAuthCallbackRequest.java`에 포함한다.

```json
// 수정된 콜백 요청 body
{
  "code": "authorization_code_from_oauth_provider",
  "redirectUri": "https://{서비스도메인}/auth/callback",
  "state": "랜덤 state 값"
}
```

Spring Boot에서는 `AuthService`의 OAuth2 콜백 처리에 state 검증 단계를 명시적으로 추가해야 한다.

---

#### C-2: Refresh Token Rotation 구현 명세 불완전 (보안 취약점)

**위치**: 섹션 11.3 인증 보안 (`리프레시 토큰 재사용` 항목), 섹션 5.3 Refresh Token 테이블

**문제**: 11.3에 "사용 시 교체 (rotation)"라고 언급했지만, 구체적인 구현 명세가 없다. rotation의 핵심인 이전 토큰 즉시 무효화와 새 토큰 발급이 원자적으로 처리되지 않으면 Race Condition이 발생할 수 있다.

또한 현재 `refresh_tokens` 테이블 스키마에 `revoked` 또는 `used_at` 컬럼이 없어서 rotation 구현이 불가능하다.

**개선 방법**:

```sql
-- refresh_tokens 테이블에 컬럼 추가
ALTER TABLE refresh_tokens ADD COLUMN is_revoked BOOLEAN NOT NULL DEFAULT FALSE;
```

Rotation 로직:
1. 클라이언트가 `POST /api/v1/auth/refresh`로 Refresh Token 전송
2. DB에서 토큰 조회: `is_revoked = true`이면 즉시 거부 (재사용 의심 → 해당 사용자의 모든 토큰 무효화 검토)
3. 만료 확인
4. 새 Access Token + 새 Refresh Token 발급
5. 기존 Refresh Token: `is_revoked = true` 업데이트
6. 새 Refresh Token DB INSERT
7. 응답 반환

Refresh Token 테이블 설계와 `AuthService` 명세에 이 흐름을 명시해야 한다.

---

### Warning (주요 개선사항)

#### W-1: 편집용 문서 조회 API에서 대용량 콘텐츠 JSON body 포함 문제

**위치**: 섹션 4.3 `GET /api/v1/documents/{slug}` 응답, 섹션 10.3 `DocumentService.createDocument`

**문제**: 편집용 문서 조회 응답에 `content` 필드(최대 5MB HTML 문자열)가 JSON body에 포함된다. 5MB 텍스트를 JSON 문자열로 직렬화하면 이스케이핑으로 인해 실제 페이로드가 더 커질 수 있으며, Spring Boot와 Next.js 양쪽에서 메모리에 올려 처리해야 하므로 대용량 문서에서 응답 지연과 메모리 압박이 발생한다.

**개선 방법**: 두 가지 선택지가 있다.
- 방법 A: 콘텐츠를 별도 API로 분리한다. `GET /api/v1/documents/{slug}/content`에서 `Content-Type: text/plain` (HTML) 또는 `text/markdown`으로 원본 텍스트를 직접 반환한다.
- 방법 B: 현재 설계를 유지하되, `@Size` 검증 외에 Spring Boot의 `spring.servlet.multipart.max-request-size`를 명시적으로 설정하고, Jackson의 스트리밍 API 사용을 검토한다.

MVP 규모(일 100개 문서, 평균 50KB)에서는 방법 B로 충분하나, 5MB 문서를 자주 편집하는 사용자가 있을 경우 방법 A가 필요하다. 현재 문서에 이 트레이드오프가 언급되어 있지 않으므로 의도적 선택임을 명시해야 한다.

---

#### W-2: sandbox iframe에서 외부 네트워크 요청 차단 누락

**위치**: 섹션 6.2 HTML 서빙 래퍼, 섹션 11.2 HTML 서빙 보안

**문제**: `sandbox="allow-scripts"`를 사용하면 스크립트 실행을 허용하므로 iframe 내 JavaScript가 `fetch()`, `XMLHttpRequest`로 외부 서버에 요청을 보낼 수 있다. 악의적인 HTML 업로더가 방문자의 브라우저 정보, IP, 또는 브라우저에 저장된 일부 데이터를 외부로 유출할 수 있다.

`allow-scripts`가 네트워크 요청을 차단하지 않는다는 점은 설계 문서에 언급되어 있지 않다.

**개선 방법**:
```html
<!-- Content-Security-Policy 헤더 추가 -->
Content-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'
```

Next.js의 `[slug]/page.tsx`에서 HTML 서빙 래퍼 페이지를 렌더링할 때 위 CSP 헤더를 추가하면 iframe 내에서 외부 리소스 로드와 네트워크 요청을 차단할 수 있다. 단, LLM 생성 HTML이 외부 CDN의 CSS/JS를 참조하는 경우 렌더링이 깨질 수 있으므로 이 트레이드오프를 문서에 명시해야 한다.

---

#### W-3: 만료 체크 시 동시 요청에 의한 중복 DB UPDATE

**위치**: 섹션 6.2 `getDocumentForView` 로직, 섹션 10.3

**문제**: `getDocumentForView`에서 `expiresAt < now`이면 `status = EXPIRED`로 DB를 업데이트하고 응답을 반환한다. 같은 만료 직후 슬러그에 다수의 동시 요청이 들어오면 여러 스레드가 동시에 같은 문서에 대해 `status = EXPIRED` UPDATE를 시도한다. 기능적으로 문제없지만(결과는 동일), 불필요한 DB 쓰기가 증가한다.

**개선 방법**: 스케줄러가 이미 만료 처리를 담당하므로, 서빙 경로에서는 상태 변경 없이 만료 판단만 하는 방식을 검토할 수 있다.

```java
// 수정 예시: DB 업데이트 없이 만료 판단
if (document.getExpiresAt() != null && document.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new DocumentExpiredException();
}
// status=EXPIRED 업데이트는 스케줄러에만 위임
```

이렇게 하면 서빙 경로가 읽기 전용이 되어 성능이 향상되고, 스케줄러가 만료 처리의 단일 책임자가 된다. 단, 스케줄러 주기(1시간)만큼 `status` 컬럼의 상태가 늦게 반영되므로, 만료 판단은 항상 `expiresAt` 값 기준으로 해야 함을 명시해야 한다.

---

#### W-4: `storageUsage.documentCount` 부정확성 위험

**위치**: 섹션 3.2 StorageUsage 엔티티, 섹션 10.3 `StorageUsageService`

**문제**: `documentCount`는 사용자의 문서 수를 캐싱한 카운터다. `createDocument`, `updateDocument`, `deleteDocument`, 스케줄러의 만료 처리 등 여러 경로에서 이 카운터를 업데이트한다. 트랜잭션 범위 외부에서 R2 작업이 실패하고 재시도가 없을 경우, 또는 스케줄러가 `EXPIRED`로 처리하면서 카운터를 감소시키지 않으면 실제 ACTIVE 문서 수와 `documentCount`가 불일치할 수 있다.

**개선 방법**:
1. `DocumentCleanupScheduler`의 만료 처리 시 로그인 사용자(`user_id IS NOT NULL`)의 문서에 대해 `storage_usage.document_count` 감소 처리를 명시적으로 추가한다.
2. 대시보드의 "사용량" 표시는 `storage_usage` 카운터 대신 `SELECT COUNT(*) FROM documents WHERE user_id = ? AND status = 'active'` 쿼리로 실시간 조회하는 방안을 검토한다 (MVP 규모에서 성능 문제 없음).

---

#### W-5: Java용 nanoid 라이브러리 의존성 명세 없음

**위치**: 섹션 10.1 패키지 구조 (`SlugGenerator.java`), 섹션 14.1 구현 순서 (Step 2 의존성 설정)

**문제**: 구현 가이드 Step 2에 "nanoid" 의존성을 추가하라고 명시했지만, JavaScript 원본 nanoid(`github.com/ai/nanoid`)는 Java 라이브러리가 아니다. Java/Kotlin에서 사용 가능한 nanoid 구현체가 별도로 필요한데, 어떤 라이브러리를 사용할지 명세가 없다.

**개선 방법**: 다음 중 하나를 선택하고 명시해야 한다.
- `com.aventrix.jnanoid:jnanoid` (Gradle 의존성 추가)
- `UUID.randomUUID()`를 Base62 인코딩하여 직접 구현 (외부 의존성 없음, URL-safe 보장)
- `SecureRandom` + 커스텀 문자셋으로 직접 구현

직접 구현 예시:
```java
private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
private static final SecureRandom RANDOM = new SecureRandom();

public String generate(int size) {
    StringBuilder sb = new StringBuilder(size);
    for (int i = 0; i < size; i++) {
        sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return sb.toString();
}
```

---

### Suggestion (사소한 개선사항)

#### S-1: HtmlSanitizer 구현 방법 재검토 필요

**위치**: 섹션 14.2 주의사항 (`HTML 새니타이징` 항목)

**문제**: "Java에서 정규식 기반 경량 처리"로 구현한다고 명시했다. 그러나 HTML 파싱을 정규식으로 처리하면 nested 태그, 속성 내 특수문자, 비표준 HTML 등에서 우회가 가능하다. 문서 자체에서도 "jsoup 등 HTML 파서 라이브러리 사용도 검토 가능"이라고 언급하고 있어, 정규식 선택의 근거가 약하다.

**개선 방법**: jsoup는 Spring Boot 생태계에서 널리 사용되며 의존성 추가가 간단하다. MVP에서 `jsoup` 사용을 권장한다.

```gradle
implementation 'org.jsoup:jsoup:1.17.2'
```

```java
// jsoup 화이트리스트 기반 새니타이징 예시
Whitelist whitelist = Whitelist.relaxed()
    .removeAttributes(":all", "onload", "onclick", "onerror"); // 인라인 이벤트 제거
    // iframe, object, embed는 relaxed에서 기본 제외됨
String cleaned = Jsoup.clean(rawHtml, whitelist);
```

단, 섹션 7.3에서 "나머지는 그대로 유지 (`<script>`, `<style>`, 인라인 이벤트 허용)"라고 명시했는데, jsoup의 `Whitelist.relaxed()`는 `<script>` 태그를 제거한다. `allow-scripts` sandbox가 있으므로 `<script>` 허용은 의도적 설계 결정이다. jsoup를 사용하면서 `<script>` 태그를 허용하려면 커스텀 화이트리스트 설정이 필요하다. 이 트레이드오프를 명시적으로 기록해야 한다.

---

#### S-2: `DELETE` 응답에서 `deletedAt` 필드의 출처 명확화

**위치**: 섹션 4.3 `DELETE /api/v1/documents/{slug}` 응답

**문제**: 응답에 `deletedAt`이 포함되어 있고 "soft delete 시점의 `updatedAt` 값"이라고 명시했다. 별도 `deleted_at` 컬럼을 두지 않는 결정은 합리적이나, `DocumentResponse` DTO에서 `deletedAt`이 `updatedAt`의 alias임을 코드 수준에서 명시하지 않으면 혼란이 생길 수 있다.

**개선 방법**: `DocumentResponse.java`의 `deletedAt` 필드는 삭제 성공 응답 전용 DTO인 `DeleteDocumentResponse.java`로 분리하여 일반 `DocumentResponse`와의 혼용을 방지한다. 또는 응답에서 `deletedAt`을 제거하고 `updatedAt`으로 통일한다.

---

#### S-3: 스케줄러 배치 처리에서 R2 삭제 실패 시 재시도 전략 미명세

**위치**: 섹션 6.5 `DocumentCleanupScheduler`

**문제**: 스케줄러가 R2 파일 삭제 + DB 상태 변경을 순차 처리하는데, R2 삭제가 실패하면 DB UPDATE만 성공하여 DB는 EXPIRED지만 R2에 파일이 남는 역방향 고아 파일이 발생할 수 있다. 로직의 "R2 파일 삭제 시도"에서 실패 시 처리 방법이 명세에 없다.

**개선 방법**: R2 삭제 실패 시 해당 문서를 skip하고 다음 배치에서 재시도하도록 명시한다. DB 상태 변경은 R2 삭제 성공 후에만 수행하거나, R2 삭제 실패를 로깅하고 별도 데드레터 처리 방안을 기록한다.

---

#### S-4: `GET /api/v1/documents` 목록에서 `status` 필터 기본값 미명시

**위치**: 섹션 4.3 `GET /api/v1/documents` 요청 파라미터

**문제**: 내 문서 목록 API의 쿼리 파라미터에 `status` 필터가 없다. `DELETED`, `EXPIRED`, `PENDING` 문서가 목록에 노출되는지 여부가 명시되어 있지 않다. 기본적으로 `ACTIVE` 문서만 반환해야 하지만 이 제약이 API 명세에 없다.

**개선 방법**: API 명세에 "인증된 사용자의 `ACTIVE` 상태 문서만 반환한다"를 명시적으로 추가한다. 또는 선택적 `status` 쿼리 파라미터를 추가하여 기본값을 `active`로 설정한다.

---

#### S-5: `middleware.ts` 토큰 만료 감지 방법 미명세

**위치**: 섹션 9.4 대시보드, 섹션 5.4 프론트엔드 토큰 관리

**문제**: `middleware.ts`에서 JWT 쿠키 존재 여부를 확인한다고 명시했지만, 쿠키가 존재하더라도 Access Token이 만료된 경우의 처리가 middleware 레벨에서 어떻게 이루어지는지 설명이 없다. 만료 토큰으로 Spring Boot API를 호출하면 401이 반환되고, 이를 `client.ts`에서 catch하여 refresh를 시도한다고 섹션 9.2에 명시되어 있으나, server component에서 호출할 때 middleware와 client.ts의 역할 분담이 불명확하다.

**개선 방법**: middleware에서는 쿠키 존재 여부만 확인하고 만료 여부 검증은 `client.ts`의 401 처리 로직에 위임한다는 결정을 명시한다. 또는 middleware에서 JWT decode(검증 없이)로 `exp` 클레임을 확인하여 만료 임박 시 미리 refresh를 시도하는 방식을 기술한다.

---

### 전체 평가

이 설계 문서는 이전 검토 결과를 충실히 반영했으며, Spring Boot 아키텍처로의 전환 근거와 트레이드오프가 명확하게 기술되어 있다. 도메인 규칙(DR-1~DR-9), 에러 코드 총괄, 트랜잭션 범위 명시, 의사결정 기록(섹션 16) 등 구현 착수에 필요한 상세도를 갖추고 있다.

신규 Critical 2건(C-1: OAuth2 state 검증 누락, C-2: Refresh Token Rotation 불완전)은 보안 관련 항목으로 구현 전 반드시 해결해야 한다. 나머지 Warning과 Suggestion은 구현 진행 중 또는 이후에 반영해도 무방하다.

| 심각도 | 건수 | 이전 검토 대비 |
|--------|------|---------------|
| Critical | 2건 | 이전 3건 모두 해결, 신규 2건 발생 |
| Warning | 5건 | 신규 발견 |
| Suggestion | 5건 | 신규 발견 |

---

## 수정 이력

> 수정일: 2026-03-21
> 수정 근거: 검토 결과 섹션의 Critical 2건 + Warning 5건 반영 + 서비스명 변경

### Critical 반영 (2건)

| 검토 ID | 수정 내용 | 수정 위치 |
|---------|----------|-----------|
| C-1 | OAuth2 state 검증 흐름 추가 | 섹션 4.4 콜백 API 요청 body에 `state` 필드 추가 + 검증 실패 에러 응답 추가. 섹션 5.1에 `5.1.1 OAuth2 State (CSRF 방어) 흐름` 서브섹션 신설하여 state 생명주기(생성/쿠키저장/검증/삭제) 전체 흐름 명시. 시퀀스 다이어그램에 state 생성/검증/쿠키 삭제 단계 추가. 섹션 9.4 OAuth2 콜백 페이지에 state 검증 단계 추가. 에러 코드에 `INVALID_OAUTH_STATE` 추가. 섹션 11.3에 `CSRF (OAuth2)` 행 추가 |
| C-2 | Refresh Token Rotation 구현 명세 | 섹션 5.3 `refresh_tokens` DDL에 `is_revoked BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 추가. `5.3.1 Refresh Token Rotation 흐름` 서브섹션 신설하여 정상 rotation + 탈취 감지 시나리오 명세. 섹션 4.4 토큰 갱신 API 응답에 `refreshToken` 필드 추가 + 에러 응답 테이블 추가. 섹션 7.1 ERD에 `is_revoked` 컬럼 추가. 섹션 7.2 DDL에 `is_revoked` 컬럼 추가. 에러 코드에 `TOKEN_EXPIRED`, `TOKEN_REUSE_DETECTED` 추가. 섹션 11.3 인증 보안 테이블의 rotation 설명 구체화 |

### Warning 반영 (5건)

| 검토 ID | 수정 내용 | 수정 위치 |
|---------|----------|-----------|
| W-1 | 편집용 조회 API의 대용량 JSON body 트레이드오프 명시 | 섹션 4.3 `GET /api/v1/documents/{slug}` 설명에 트레이드오프 주석 추가. MVP에서는 현재 설계 유지, Phase 2에서 콘텐츠 분리 API 검토 명시. `spring.servlet.multipart.max-request-size=10MB` 설정 추가 |
| W-2 | sandbox iframe 외부 네트워크 요청 차단 CSP 헤더 추가 | 섹션 6.2 HTML 서빙 래퍼에 CSP 헤더(`default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:`) 명세 추가. 섹션 11.2 보안 테이블에 `외부 데이터 유출` 대응책 행 추가 |
| W-3 | 만료 체크 시 중복 DB UPDATE 제거 | 섹션 6.2 서빙 플로우와 섹션 10.3 `getDocumentForView` 로직을 읽기 전용으로 변경. status=EXPIRED 업데이트는 스케줄러에만 위임. 만료 판단은 항상 `expiresAt` 값 기준 |
| W-4 | storageUsage.documentCount 불일치 대응 | 섹션 3.2 StorageUsage 엔티티에 documentCount 정확성 주의사항 추가. 대시보드 표시는 실시간 COUNT 쿼리 사용, 스케줄러 만료 처리 시 document_count 감소 명시 |
| W-5 | Java용 nanoid 구현 방식 명시 | 섹션 3.3에 SecureRandom + 커스텀 알파벳 직접 구현 방침 명시 (외부 라이브러리 의존 없음). 섹션 10.1 SlugGenerator 설명 업데이트. 섹션 14.1 Gradle 의존성 목록에서 nanoid 제거 |

### 서비스명 변경 (3건)

| 변경 전 | 변경 후 | 위치 |
|---------|---------|------|
| `docshare-files` | `drafturl-files` | 섹션 8.1 R2 버킷명, 섹션 10.2 application.yml |
| `com.docshare.api` | `com.drafturl.api` | 섹션 10.1 패키지 경로 |
| `DocshareApiApplication` | `DrafturlApiApplication` | 섹션 10.1 메인 클래스명 |

---

## 수정 후 검토 결과

> 검토일: 2026-03-21
> 검토 대상: 수정 이력 섹션에 기재된 변경사항 (Critical 2건 + Warning 5건 + 서비스명 전체 변경)
> 검토 방법: 수정 이력 표의 반영 위치를 기준으로 본문 실제 반영 여부를 교차 확인

### 반영 확인 결과 요약

| 항목 | 반영 여부 | 비고 |
|------|----------|------|
| C-1: OAuth2 state 검증 | 완전 반영 | |
| C-2: Refresh Token Rotation | 완전 반영 | |
| W-1: 대용량 JSON body 트레이드오프 | 완전 반영 | |
| W-2: CSP 헤더 명세 | 완전 반영 (단, 경미한 불일치 존재) | 하단 상세 참조 |
| W-3: 만료 처리 읽기 전용 변경 | 완전 반영 | |
| W-4: documentCount 정확성 주의사항 | 완전 반영 | |
| W-5: nanoid Java 구현 방침 | 완전 반영 | |
| 서비스명 DocShare/docshare -> DraftURL/drafturl | 완전 반영 | 본문에 잔여 없음 |

---

### C-1: OAuth2 State 검증 반영 확인

반영 확인 완료. 아래 4개 위치 모두 정확히 반영되었다.

- 섹션 4.4 콜백 API 요청 body: `state` 필드(String, 필수) 추가, 필드 설명 포함
- 섹션 4.4 콜백 API: `state` 검증 실패 시 `INVALID_OAUTH_STATE` 에러 응답 예시 추가
- 섹션 4.5 에러 코드 총괄: `400 INVALID_OAUTH_STATE` 행 추가
- 섹션 5.1.1: state 생명주기 7단계 및 시퀀스 다이어그램 신설
- 섹션 9.4 OAuth2 콜백 페이지: state 검증 단계(2~3번) 및 쿠키 삭제 단계(6번) 명시
- 섹션 11.3: `CSRF (OAuth2)` 대응책 행 추가

생명주기의 단계 4에서 "Next.js에서 비교 검증"하고 단계 6에서 "Spring Boot가 state 존재 여부를 확인(null/빈값 거부)"하는 역할 분담이 명확히 기술되어 있다. Spring Boot가 state의 쿠키 일치 여부를 재검증하지 않고 존재 여부만 확인하는 설계 결정도 다이어그램에 명시적으로 드러나 있어 의도가 분명하다.

---

### C-2: Refresh Token Rotation 반영 확인

반영 확인 완료. 아래 위치 모두 정확히 반영되었다.

- 섹션 5.3 DDL(`CREATE TABLE refresh_tokens`): `is_revoked BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 포함
- 섹션 5.3.1: 정상 Rotation 6단계 및 탈취 감지 4단계 명세 신설. `@Transactional` 원자 처리 명시
- 섹션 4.4 토큰 갱신 API: 성공 응답에 `refreshToken` 필드 추가, 에러 응답 표 추가
- 섹션 4.5 에러 코드 총괄: `TOKEN_EXPIRED`, `TOKEN_REUSE_DETECTED` 행 추가
- 섹션 7.1 ERD: `is_revoked BOOLEAN` 컬럼 표시
- 섹션 7.2 DDL(`V1__init.sql`): `is_revoked BOOLEAN NOT NULL DEFAULT FALSE` 포함
- 섹션 11.3: rotation 설명이 "사용 시 교체(rotation)"에서 구체적인 탈취 감지 동작으로 업데이트

수정 이력 표(C-2)에는 "섹션 4.4 토큰 갱신 API 응답에 refreshToken 필드 추가"가 명시되어 있으며, 실제로 섹션 4.4의 성공 응답 예시(line 629-637)에 `refreshToken` 필드가 포함되어 있다.

---

### W-1: 대용량 JSON body 트레이드오프 반영 확인

반영 확인 완료.

- 섹션 4.3 `GET /api/v1/documents/{slug}` 본문에 트레이드오프 주석 블록이 추가되었다 (line 447).
- MVP에서는 현재 설계 유지, Phase 2에서 콘텐츠 분리 API 검토 방침을 명시하였다.
- `spring.servlet.multipart.max-request-size=10MB` 설정 언급이 포함되어 있다.

---

### W-2: CSP 헤더 명세 반영 확인

반영 확인 완료. 단, 경미한 불일치가 존재한다.

반영된 내용:
- 섹션 6.2 HTML 서빙 래퍼에 CSP 헤더 블록 신설 (line 973-983)
- 섹션 11.2 보안 테이블에 `외부 데이터 유출` 대응책 행 추가 (line 1650)

**불일치 사항**: 기존 검토 결과(W-2)에서 권고한 CSP 헤더와 실제 반영된 CSP 헤더가 `img-src` 지시어에서 다르다.

| 위치 | CSP 헤더 값 |
|------|-------------|
| 기존 검토(W-2) 권고안 | `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'` |
| 실제 반영(섹션 6.2) | `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:` |
| 섹션 11.2 보안 테이블 | `default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:` |

권고안보다 `img-src data:`가 추가되어 data URI 이미지를 허용하는 방향으로 확장된 것은 LLM 생성 HTML에서 base64 인라인 이미지가 흔하다는 점을 감안하면 합리적인 결정이다. 섹션 6.2와 섹션 11.2의 CSP 값이 일치하므로 내부 일관성은 유지되고 있다. 의도적 변경이라면 수정 이력 표에 "img-src data: 추가 이유" 한 줄을 덧붙이면 완결성이 높아진다.

---

### W-3: 만료 처리 읽기 전용 변경 반영 확인

반영 확인 완료.

- 섹션 6.2 서빙 플로우 시퀀스 다이어그램(line 925-929): 만료 시간 확인 시 "DB UPDATE 없음, status 변경은 스케줄러에 위임" 명시
- 섹션 10.3 `getDocumentForView` 로직(line 1605-1607): "DB UPDATE 없음 (읽기 전용). status=EXPIRED 변경은 스케줄러에 위임. 만료 판단은 항상 expiresAt 값 기준으로 수행" 명시

---

### W-4: documentCount 정확성 주의사항 반영 확인

반영 확인 완료.

- 섹션 3.2 StorageUsage 엔티티 아래에 상세한 주의사항 블록 추가 (line 217): 대시보드의 `documentCount` 표시를 실시간 COUNT 쿼리로 조회한다는 방침, `storage_usage.document_count`는 근사값 용도에 한정한다는 결정, 스케줄러 만료 처리 시 document_count 감소 명시를 모두 포함하고 있다.

---

### W-5: nanoid Java 구현 방침 반영 확인

반영 확인 완료.

- 섹션 3.3 값 객체: `SecureRandom + 커스텀 알파벳으로 직접 구현 (외부 라이브러리 의존 없음)` 및 `com.aventrix.jnanoid:jnanoid 대신 직접 구현을 선택`하는 이유 명시 (line 234-237)
- 섹션 10.1 `SlugGenerator.java` 설명: `(SecureRandom 직접 구현)` 명시 (line 1486)
- 섹션 14.1 구현 순서 Step 2: `nanoid는 직접 구현` 명시 (line 1790)

---

### 서비스명 변경 반영 확인

반영 확인 완료. 본문 전체에서 `DocShare`, `docshare` 잔여 사용은 없다.

- 섹션 8.1 R2 버킷명: `drafturl-files` (line 1223)
- 섹션 10.1 패키지 경로: `com.drafturl.api` (line 1465)
- 섹션 10.1 메인 클래스명: `DrafturlApiApplication` (line 1466)
- 섹션 10.2 application.yml `r2.bucket-name`: `drafturl-files` (line 1258)

수정 이력 표에 기재된 3개소 이외에 `DocShare`/`docshare`가 등장하는 위치는 수정 이력 표 자체(이전 값 표기용)뿐이며, 이는 변경 전 값을 기록한 것이므로 잔여 오류가 아니다.

---

### 신규 발견 사항

수정 반영 자체는 모두 올바르게 이루어졌다. 검토 과정에서 추가로 발견된 사항을 아래에 기록한다.

#### Minor-1: 섹션 15 "기존 검토 결과(Critical 3건) 반영 확인" 표와 실제 검토 결과의 ID 체계 불일치

**위치**: 섹션 15 (line 1881-1888)

**설명**: 섹션 15의 표는 이전 최초 검토 결과(C-1: 고아 R2 파일, C-2: sandbox allow-same-origin, C-3: CRON 인증)를 기준으로 작성되어 있다. 그런데 "검토 결과" 섹션의 Critical 항목(C-1: OAuth2 state, C-2: Refresh Token Rotation)은 이후 검토에서 새로 부여된 ID이므로 번호가 겹친다. 문서를 처음 읽는 사람이 두 C-1, 두 C-2를 혼동할 수 있다.

**개선 제안**: 섹션 15의 표 제목을 "이전 1차 검토 결과(Critical 3건) 반영 확인"으로 변경하거나, 검토 결과 섹션의 Critical ID를 C-4, C-5로 순번을 이어서 부여하는 방식을 검토한다.

#### Minor-2: W-2 권고 CSP와 반영 CSP의 차이를 수정 이력에 미기재

**위치**: 수정 이력 섹션 W-2 행 (line 2227)

**설명**: W-2 항목에서 확인한 대로, 기존 검토 권고안에는 없던 `img-src data:`가 추가되었으나 수정 이력 표에는 이 추가 사항이 설명되어 있지 않다. 의도적인 확장임을 수정 이력에 한 줄로 기록하면 문서의 추적 가능성이 높아진다.

#### Minor-3: 섹션 6.5 스케줄러에서 만료 처리 시 document_count 감소 로직이 의사코드에 미반영

**위치**: 섹션 6.5 (line 1029-1052)

**설명**: W-4 반영으로 섹션 3.2의 주의사항 블록에 "스케줄러의 만료 처리 시에도 로그인 사용자의 문서에 대해 document_count 감소를 명시적으로 처리한다"고 명시되었다. 그러나 섹션 6.5의 스케줄러 의사코드(`2. 만료 문서 처리` 블록)에는 `document_count` 감소 처리가 표시되어 있지 않다. 두 위치가 일치해야 한다.

**개선 제안**: 섹션 6.5의 `2. 만료 문서 처리` 블록에 `-> 로그인 사용자 문서인 경우: storage_usage.document_count 감소` 한 줄을 추가한다.

---

### 전체 평가

이번 수정은 Critical 2건과 Warning 5건 모두 올바르게 반영되었다. 특히 다음 사항이 잘 처리되었다.

- C-1의 시퀀스 다이어그램이 Next.js/Spring Boot 역할 분담(Next.js는 쿠키 일치 검증, Spring Boot는 존재 여부 확인)을 명확히 표현하고 있다.
- C-2의 탈취 감지 시나리오가 별도 블록으로 분리되어 정상 Rotation과 비정상 케이스가 구분된다.
- W-3의 읽기 전용 변경이 시퀀스 다이어그램과 서비스 로직 양쪽에 일관되게 반영되었다.
- 서비스명 변경은 본문 전체에서 누락 없이 완료되었다.

신규 발견 사항 3건은 모두 Minor 수준이며, 문서 기능성에 영향을 주지 않는다. 우선순위상 Minor-3(섹션 6.5 의사코드 불일치)은 구현자가 오해할 여지가 있으므로 가장 먼저 처리하는 것을 권장한다.

| 심각도 | 건수 |
|--------|------|
| Critical | 0건 |
| Major | 0건 |
| Minor | 3건 |
