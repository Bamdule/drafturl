# MCP Dynamic Client Registration (RFC 7591)

## Problem

Claude Code의 MCP OAuth2 플로우는 RFC 7591 Dynamic Client Registration을 필수로 요구한다.
현재 DraftURL MCP 서버는 `registration_endpoint`가 없어 인증이 실패한다:
> "Incompatible auth server: does not support dynamic client registration"

## Decision Summary

| 항목 | 결정 |
|------|------|
| 저장소 | 인메모리 (ConcurrentHashMap) |
| 검증 수준 | 엄격 (redirect_uri, auth method, grant_types) |
| 만료 정책 | TTL 7일 + 스케줄러 정리 |
| 구현 방식 | 단일 컨트롤러 추가 (최소 변경) |

## Data Model

### RegisteredClient

```java
record RegisteredClient(
    String clientId,           // UUID 자동 생성
    List<String> redirectUris, // localhost/127.0.0.1 또는 HTTPS만 허용
    String clientName,         // 선택 (e.g., "Claude Code")
    Instant createdAt,
    Instant expiresAt          // createdAt + 7일
)
```

### ClientRegistrationStore

- `ConcurrentHashMap<String, RegisteredClient>` 기반
- `register(request)` -> 검증 후 RegisteredClient 반환
- `findByClientId(clientId)` -> Optional<RegisteredClient>
- `@Scheduled(fixedRate = 60000)` -> 만료 클라이언트 자동 정리

## API

### POST /oauth2/register

**Request:**
```json
{
  "redirect_uris": ["http://127.0.0.1:12345/callback"],
  "client_name": "Claude Code",
  "token_endpoint_auth_method": "none",
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"]
}
```

**Validation:**
- `redirect_uris` 필수. 각 URI는 localhost/127.0.0.1 또는 HTTPS만 허용 (기존 `isValidRedirectUri` 로직 재사용)
- `token_endpoint_auth_method` 있으면 `"none"`만 허용
- `grant_types` 있으면 `"authorization_code"` 포함 필수

**Success (201 Created):**
```json
{
  "client_id": "generated-uuid",
  "client_name": "Claude Code",
  "redirect_uris": ["http://127.0.0.1:12345/callback"],
  "token_endpoint_auth_method": "none",
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"],
  "client_id_issued_at": 1711785600
}
```

**Error (400 Bad Request):**
```json
{
  "error": "invalid_redirect_uri",
  "error_description": "redirect_uri must be localhost or HTTPS"
}
```

## Changes to Existing Code

### OAuthMetadataController

`/.well-known/oauth-authorization-server` 응답에 추가:
```java
Map.entry("registration_endpoint", issuer + "/oauth2/register")
```

### SecurityConfig

변경 없음. `/oauth2/**`가 이미 `permitAll()`이므로 `/oauth2/register`는 자동으로 공개 접근 가능.

### OAuthAuthorizeController

변경 없음. 등록되지 않은 client_id도 기존처럼 허용하여 호환성 유지.

## File Changes

| File | Action |
|------|--------|
| `domain/oauth2/RegisteredClient.java` | New |
| `domain/oauth2/ClientRegistrationStore.java` | New |
| `domain/oauth2/controller/OAuthClientRegistrationController.java` | New |
| `domain/oauth2/controller/OAuthMetadataController.java` | Modify (1 line) |

## Out of Scope

- client_id 검증 강화 (authorize/token 엔드포인트에서 등록된 client만 허용)
- client_secret 지원 (현재 public client only)
- DB 영속화
