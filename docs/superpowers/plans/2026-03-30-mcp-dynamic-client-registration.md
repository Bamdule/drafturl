# MCP Dynamic Client Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Claude Code가 DraftURL MCP 서버에 OAuth2 인증할 수 있도록 RFC 7591 Dynamic Client Registration 엔드포인트를 추가한다.

**Architecture:** 인메모리 `ConcurrentHashMap` 기반 클라이언트 저장소를 추가하고, `POST /oauth2/register` 엔드포인트에서 redirect_uri/grant_type/auth_method를 엄격 검증한 뒤 client_id를 발급한다. 7일 TTL + 스케줄러로 만료 클라이언트를 정리한다. 메타데이터에 `registration_endpoint`를 추가하여 Claude Code가 자동 발견할 수 있게 한다.

**Tech Stack:** Spring Boot 3.4, Java 21

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `domain/oauth2/RegisteredClient.java` | Create | 등록된 클라이언트 데이터 레코드 |
| `domain/oauth2/ClientRegistrationStore.java` | Create | 인메모리 저장소 + TTL 스케줄러 |
| `domain/oauth2/controller/OAuthClientRegistrationController.java` | Create | POST /oauth2/register 엔드포인트 |
| `domain/oauth2/controller/OAuthMetadataController.java` | Modify | registration_endpoint 추가 |

모든 파일은 `backend/src/main/java/com/drafturl/api/` 하위에 위치한다.

---

### Task 1: RegisteredClient 레코드 생성

**Files:**
- Create: `backend/src/main/java/com/drafturl/api/domain/oauth2/RegisteredClient.java`

- [ ] **Step 1: RegisteredClient 레코드 작성**

```java
package com.drafturl.api.domain.oauth2;

import java.time.Instant;
import java.util.List;

public record RegisteredClient(
        String clientId,
        List<String> redirectUris,
        String clientName,
        Instant createdAt,
        Instant expiresAt
) {}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/oauth2/RegisteredClient.java
git commit -m "feat: RegisteredClient 레코드 추가"
```

---

### Task 2: ClientRegistrationStore 생성

**Files:**
- Create: `backend/src/main/java/com/drafturl/api/domain/oauth2/ClientRegistrationStore.java`

- [ ] **Step 1: ClientRegistrationStore 작성**

`AuthorizationCodeStore`와 동일한 패턴으로 인메모리 저장소를 구현한다. TTL은 7일 (604800초).

```java
package com.drafturl.api.domain.oauth2;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientRegistrationStore {

    private static final long TTL_SECONDS = 604_800; // 7 days

    private final ConcurrentHashMap<String, RegisteredClient> store = new ConcurrentHashMap<>();

    public RegisteredClient register(List<String> redirectUris, String clientName) {
        validateRedirectUris(redirectUris);

        String clientId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        RegisteredClient client = new RegisteredClient(
                clientId, redirectUris, clientName, now, now.plusSeconds(TTL_SECONDS));
        store.put(clientId, client);
        return client;
    }

    public Optional<RegisteredClient> findByClientId(String clientId) {
        RegisteredClient client = store.get(clientId);
        if (client == null || client.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(client);
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private void validateRedirectUris(List<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new InvalidRegistrationException("invalid_redirect_uri",
                    "redirect_uris is required and must not be empty");
        }
        for (String uri : redirectUris) {
            if (!isValidRedirectUri(uri)) {
                throw new InvalidRegistrationException("invalid_redirect_uri",
                        "redirect_uri must be localhost or HTTPS: " + uri);
            }
        }
    }

    private boolean isValidRedirectUri(String uri) {
        try {
            URI parsed = URI.create(uri);
            String host = parsed.getHost();
            String scheme = parsed.getScheme();
            return "localhost".equals(host) || "127.0.0.1".equals(host)
                    || "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    public static class InvalidRegistrationException extends RuntimeException {
        private final String error;
        private final String errorDescription;

        public InvalidRegistrationException(String error, String errorDescription) {
            super(errorDescription);
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public String getError() { return error; }
        public String getErrorDescription() { return errorDescription; }
    }
}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/oauth2/ClientRegistrationStore.java
git commit -m "feat: ClientRegistrationStore 인메모리 저장소 구현"
```

---

### Task 3: OAuthClientRegistrationController 생성

**Files:**
- Create: `backend/src/main/java/com/drafturl/api/domain/oauth2/controller/OAuthClientRegistrationController.java`

- [ ] **Step 1: 컨트롤러 작성**

RFC 7591에 따라 요청을 검증하고 클라이언트를 등록한다.

```java
package com.drafturl.api.domain.oauth2.controller;

import com.drafturl.api.domain.oauth2.ClientRegistrationStore;
import com.drafturl.api.domain.oauth2.RegisteredClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OAuthClientRegistrationController {

    private final ClientRegistrationStore clientRegistrationStore;

    public OAuthClientRegistrationController(ClientRegistrationStore clientRegistrationStore) {
        this.clientRegistrationStore = clientRegistrationStore;
    }

    @PostMapping(value = "/oauth2/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {

        // redirect_uris 추출
        @SuppressWarnings("unchecked")
        List<String> redirectUris = (List<String>) request.get("redirect_uris");

        // token_endpoint_auth_method 검증
        String authMethod = (String) request.get("token_endpoint_auth_method");
        if (authMethod != null && !"none".equals(authMethod)) {
            return errorResponse("invalid_client_metadata",
                    "Only token_endpoint_auth_method 'none' is supported");
        }

        // grant_types 검증
        @SuppressWarnings("unchecked")
        List<String> grantTypes = (List<String>) request.get("grant_types");
        if (grantTypes != null && !grantTypes.contains("authorization_code")) {
            return errorResponse("invalid_client_metadata",
                    "grant_types must include 'authorization_code'");
        }

        // client_name 추출
        String clientName = (String) request.get("client_name");

        // 등록
        RegisteredClient client = clientRegistrationStore.register(redirectUris, clientName);

        // RFC 7591 응답
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client_id", client.clientId());
        response.put("client_name", client.clientName());
        response.put("redirect_uris", client.redirectUris());
        response.put("token_endpoint_auth_method", "none");
        response.put("grant_types", grantTypes != null ? grantTypes : List.of("authorization_code"));
        response.put("response_types", List.of("code"));
        response.put("client_id_issued_at", client.createdAt().getEpochSecond());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(ClientRegistrationStore.InvalidRegistrationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRegistration(
            ClientRegistrationStore.InvalidRegistrationException e) {
        return errorResponse(e.getError(), e.getErrorDescription());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/oauth2/controller/OAuthClientRegistrationController.java
git commit -m "feat: POST /oauth2/register 엔드포인트 구현"
```

---

### Task 4: OAuthMetadataController에 registration_endpoint 추가

**Files:**
- Modify: `backend/src/main/java/com/drafturl/api/domain/oauth2/controller/OAuthMetadataController.java:29-41`

- [ ] **Step 1: registration_endpoint 추가**

`authorizationServerMetadata()` 메서드의 반환값에 한 줄 추가:

```java
Map.entry("registration_endpoint", issuer + "/oauth2/register")
```

기존 `Map.ofEntries(...)` 안에 추가한다. 변경 전:

```java
Map.entry("scopes_supported", List.of("document:read", "document:write"))
```

변경 후:

```java
Map.entry("registration_endpoint", issuer + "/oauth2/register"),
Map.entry("scopes_supported", List.of("document:read", "document:write"))
```

- [ ] **Step 2: 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/drafturl/api/domain/oauth2/controller/OAuthMetadataController.java
git commit -m "feat: OAuth2 메타데이터에 registration_endpoint 추가"
```

---

### Task 5: 통합 검증

- [ ] **Step 1: 전체 빌드 확인**

Run: `cd backend && ./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 프로덕션 메타데이터 확인 (curl)**

Run: `curl -s https://drafturl.com/.well-known/oauth-authorization-server | python3 -m json.tool`
Expected: `registration_endpoint` 필드가 응답에 포함되어야 한다. (배포 전이면 로컬에서 확인)

- [ ] **Step 3: 클라이언트 등록 테스트 (curl)**

Run:
```bash
curl -s -X POST http://localhost:8080/oauth2/register \
  -H "Content-Type: application/json" \
  -d '{
    "redirect_uris": ["http://127.0.0.1:3000/callback"],
    "client_name": "Test Client",
    "token_endpoint_auth_method": "none",
    "grant_types": ["authorization_code", "refresh_token"],
    "response_types": ["code"]
  }' | python3 -m json.tool
```
Expected: 201 응답 + `client_id` 포함 JSON

- [ ] **Step 4: 잘못된 redirect_uri 거부 테스트**

Run:
```bash
curl -s -X POST http://localhost:8080/oauth2/register \
  -H "Content-Type: application/json" \
  -d '{
    "redirect_uris": ["http://evil.com/callback"],
    "client_name": "Evil Client"
  }' | python3 -m json.tool
```
Expected: 400 응답 + `"error": "invalid_redirect_uri"`

- [ ] **Step 5: Claude Code MCP 재연결 테스트**

Claude Code에서 `/mcp` 실행 후 drafturl 서버에서 Authenticate 시도.
Expected: 인증 플로우가 진행되어야 한다 (로그인 → 동의 → 토큰 발급).

- [ ] **Step 6: 최종 커밋 (필요 시)**

모든 검증이 통과하면 추가 수정사항이 있을 경우에만 커밋.
