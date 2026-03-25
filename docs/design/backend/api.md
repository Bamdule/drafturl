# DraftURL -- API 명세

> 상태: active
> 작성일: 2026-03-21

---

## 요약

- REST API 공통 규격(Base URL, 응답 포맷, CORS, Rate Limiting)을 정의하고, 12개 엔드포인트 명세를 포함
- 문서 CRUD(생성/목록/상세/수정/삭제/서빙)와 인증 API(OAuth2 콜백/토큰 갱신/사용자 정보/로그아웃) 상세 명세 제공
- 18종 에러 코드를 HTTP 상태 코드별로 정의하여 프론트엔드 에러 처리의 기반 제공

---

## 1. API 공통 규격

### Base URL

```
프론트엔드:  https://{서비스도메인}              (Vercel)
백엔드 API: https://api.{서비스도메인}           (Railway)
            또는 https://{app-name}.up.railway.app
```

### 공통 응답 포맷

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

### 공통 헤더

| 헤더 | 용도 |
|------|------|
| `Content-Type: application/json` | 요청/응답 본문 형식 |
| `Authorization: Bearer {JWT}` | 인증이 필요한 API 호출 시 |
| `X-Forwarded-For` | Rate Limiting용 클라이언트 IP 식별 |

### CORS 설정

```
Access-Control-Allow-Origin: https://{서비스도메인}
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

Spring Security의 `CorsConfigurationSource` 빈으로 설정한다. 개발 환경에서는 `http://localhost:3000`을 추가 허용한다.

### Rate Limiting

| 대상 | 제한 |
|------|------|
| 비로그인 IP | 분당 10회 API 호출 |
| 로그인 사용자 | 분당 30회 API 호출 |

Spring Boot에서 `bucket4j-spring-boot-starter` 또는 직접 구현한 필터로 처리한다. 토큰 버킷 알고리즘을 사용하며, 상태는 인메모리 `ConcurrentHashMap`에 저장한다 (단일 인스턴스 MVP 기준). 프로덕션 스케일 시 Redis로 전환한다.

Rate limit 초과 시 `429 Too Many Requests` 응답과 함께 `Retry-After` 헤더를 포함한다.

---

## 2. 엔드포인트 목록

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

---

## 3. API 상세 명세

### POST /api/v1/documents -- 문서 생성

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

### GET /api/v1/documents -- 내 문서 목록

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
        "expiresAt": null,
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

### GET /api/v1/documents/{slug} -- 문서 상세 조회 (편집용)

인증 필수. 소유자만 접근 가능 (403 Forbidden 반환). R2에서 원본 콘텐츠를 함께 로드하여 반환한다.

> **대용량 콘텐츠 트레이드오프**: `content` 필드(최대 5MB)가 JSON body에 포함된다. JSON 직렬화 시 이스케이핑으로 페이로드가 증가할 수 있으나, MVP 규모(일 100개 문서, 평균 50KB)에서는 허용 가능하다. 5MB 문서를 빈번하게 편집하는 유스케이스가 확인되면 Phase 2에서 `GET /api/v1/documents/{slug}/content` (Content-Type: text/plain)로 콘텐츠를 분리하는 방안을 적용한다.

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

### 편집용 vs 서빙용 응답 필드 비교

| 필드 | `GET /{slug}` (편집용) | `GET /{slug}/view` (서빙용) |
|------|:---------------------:|:--------------------------:|
| `id` | O | O |
| `slug` | O | - |
| `url` | O | - |
| `title` | O | O |
| `docType` | O | O |
| `content` | O | O |
| `contentSize` | O | - |
| `status` | O | - |
| `expiresAt` | O | - |
| `createdAt` | O | O |
| `updatedAt` | O | - |

### GET /api/v1/documents/{slug}/view -- 문서 서빙용 (공개)

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
ETag: "{updatedAt의 해시}"
```

**에러 응답**

| 상황 | HTTP | 코드 |
|------|------|------|
| 문서 없음 | 404 | `DOCUMENT_NOT_FOUND` |
| 만료된 문서 | 410 | `DOCUMENT_EXPIRED` |
| 삭제된 문서 | 410 | `DOCUMENT_GONE` |

만료 문서에 `410 Gone`을 사용하여 프론트엔드가 만료 안내 UI를 분기 렌더링할 수 있게 한다.

### PUT /api/v1/documents/{slug} -- 문서 수정

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

최소 하나의 필드는 포함되어야 한다. **타입 변경(`type` 필드)은 MVP에서 허용하지 않는다.** R2 키 충돌 및 추가 복잡도를 방지한다.

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

### DELETE /api/v1/documents/{slug} -- 문서 삭제

> **설계 결정**: 204 No Content 대신 200 OK를 사용하는 이유 -- 프론트엔드에서 삭제된 문서의 slug와 삭제 시점을 즉시 활용(토스트 메시지, 목록 갱신)할 수 있도록 응답 body를 포함한다.

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

서버에서 `status = DELETED`로 변경한 시점의 타임스탬프를 `deletedAt`으로 반환한다. DB에 별도 `deleted_at` 컬럼은 두지 않는다.

---

## 4. 인증 API

### POST /api/v1/auth/oauth2/callback/{provider} -- OAuth2 콜백

OAuth2 인가 코드를 받아 사용자를 생성/조회하고 JWT를 발급한다. **CSRF 방어를 위해 `state` 파라미터를 반드시 검증한다.** 인증 플로우 상세는 [auth.md](auth.md)를 참조한다.

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

### POST /api/v1/auth/refresh -- 토큰 갱신

Refresh Token Rotation을 적용한다. 갱신 시 기존 Refresh Token은 즉시 무효화되고 새 Refresh Token이 발급된다. 이미 무효화된 토큰으로 갱신 시도 시 해당 사용자의 모든 토큰을 일괄 무효화한다 (탈취 감지). 상세 Rotation 흐름은 [auth.md](auth.md)를 참조한다.

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

### POST /api/v1/auth/logout -- 로그아웃

Refresh Token을 무효화하여 로그아웃을 처리한다.

**요청**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `refreshToken` | String | O | 무효화할 Refresh Token |

**처리**: 해당 Refresh Token을 `is_revoked = true`로 업데이트한다.

**성공 응답 (200 OK)**

```json
{
  "success": true
}
```

**에러 응답**

| 상황 | HTTP | 코드 |
|------|------|------|
| 토큰 없거나 이미 무효화됨 | 401 | `INVALID_TOKEN` |

### GET /api/v1/auth/me -- 현재 사용자

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

> **Phase 2 검토 사항**: `storageUsage`를 별도 엔드포인트(`GET /api/v1/users/me/storage`)로 분리하여 인증과 스토리지 관심사를 분리하는 방안 검토.

---

## 5. 에러 코드 총괄

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

## 관련 문서

- [백엔드 설계 개요](README.md) -- 기술 스택, 패키지 구조
- [인증/인가 설계](auth.md) -- OAuth2 플로우, JWT, Refresh Token Rotation
- [핵심 로직](logic.md) -- 문서 생성/수정/삭제 흐름
- [프론트엔드 API 클라이언트](../frontend/client.md) -- 프론트엔드의 API 호출 방식
