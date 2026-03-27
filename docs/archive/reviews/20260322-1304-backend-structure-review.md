# Backend 패키지 구조 및 코드 설계 검토

> **요약**: 전반적으로 3-tier 구조(domain/infra/global)와 Port-Adapter 패턴이 일관되게 적용되어 있으며, 2단계 커밋 패턴과 스케줄러 기반 정합성 보장 설계가 잘 되어 있다. 주요 개선 포인트는 (1) DTO가 domain 패키지에 있어 표현 계층 관심사가 도메인에 침투하는 문제, (2) enum이 global에 있어 도메인 소속이 불분명한 문제, (3) DocumentService의 비즈니스 검증 로직 집중, (4) 컨트롤러에 비즈니스 로직이 일부 누출된 점이다.

---

## 1. DTO 위치 문제

### 현재 상태

```
domain/document/dto/request/CreateDocumentRequest.java   -- @NotBlank, @ContentSize
domain/document/dto/request/UpdateDocumentRequest.java    -- @ContentSize
domain/document/dto/response/DocumentResponse.java        -- Document 엔티티에 직접 의존
domain/user/dto/request/OAuthCallbackRequest.java         -- @NotBlank
domain/user/dto/request/RefreshTokenRequest.java          -- @NotBlank
domain/user/dto/response/AuthResponse.java                -- User 엔티티에 직접 의존
domain/user/dto/response/UserResponse.java                -- User 엔티티에 직접 의존
```

### 문제점

**1-1. 표현(Presentation) 계층의 관심사가 domain에 침투한다.**

`CreateDocumentRequest`에 `@NotBlank`, `@ContentSize`(커스텀 Bean Validation) 등 HTTP 요청 검증 어노테이션이 붙어 있다. Bean Validation은 컨트롤러가 `@Valid`로 트리거하는 표현 계층의 메커니즘이다. 이것이 domain 패키지에 있으면:

- domain 패키지만 보고 "순수 도메인 로직"이라고 기대하는 개발자에게 혼란을 준다
- 내부 서비스 간 호출 시 동일한 DTO를 사용하게 되면 HTTP 검증이 불필요하게 따라온다
- Jakarta Validation 의존성이 domain 계층까지 전파된다

**1-2. API 요청/응답 DTO인지, 서비스 간 내부 전달 DTO인지 구분이 안 된다.**

현재 `CreateDocumentRequest`는 컨트롤러에서 `@RequestBody`로 받는 API 요청 DTO이면서, 동시에 `DocumentService.createDocument()`의 파라미터로 그대로 전달된다. 서비스 메서드의 시그니처가 HTTP 계층에 결합된다.

**1-3. 커스텀 Validator(`ContentSizeValidator`)도 domain 패키지에 위치한다.**

`ContentSizeValidator`는 `ConstraintValidator`를 구현하는 Jakarta Validation 인프라 코드인데, `domain/document/dto/request/` 패키지에 있다.

### 개선 방안

**방안 A: presentation 패키지 신설 (권장)**

```
com.drafturl.api
  +-- presentation (신규)
  |     +-- document
  |     |     +-- DocumentController.java
  |     |     +-- request/
  |     |     |     +-- CreateDocumentRequest.java    (HTTP 검증 어노테이션 포함)
  |     |     |     +-- UpdateDocumentRequest.java
  |     |     |     +-- ContentSize.java
  |     |     |     +-- ContentSizeValidator.java
  |     |     +-- response/
  |     |           +-- DocumentResponse.java
  |     |           +-- DocumentViewResponse.java
  |     |           +-- DocumentEditResponse.java
  |     |           +-- DocumentListResponse.java
  |     |           +-- DocumentDeleteResponse.java
  |     +-- user
  |           +-- AuthController.java
  |           +-- request/
  |           +-- response/
  +-- domain
  |     +-- document
  |     |     +-- service/
  |     |     +-- entity/
  |     |     +-- repository/
  |     |     +-- port/
  |     |     +-- exception/
  |     +-- user
  |           +-- service/
  |           +-- entity/
  |           +-- repository/
  |           +-- port/
  +-- infra
  +-- global
```

이 방안에서 서비스 메서드의 시그니처를 변경하여 HTTP DTO 대신 원시 값 또는 서비스 전용 커맨드 객체를 받도록 한다:

```java
// 변경 전: 서비스가 HTTP DTO에 직접 의존
public DocumentResponse createDocument(CreateDocumentRequest request, UUID userId)

// 변경 후 옵션 1: 서비스 전용 커맨드 객체
public DocumentResponse createDocument(CreateDocumentCommand command)

// 변경 후 옵션 2: 원시 파라미터 (필드가 적을 때)
// 현재 3개 필드(content, type, title)이므로 커맨드 객체가 더 적절하다
```

**방안 B: 현재 구조 유지 + 주석/컨벤션으로 구분 (최소 변경)**

MVP 단계에서 구조 변경 비용이 크다면, 현재 구조를 유지하되:
- 패키지 레벨 Javadoc(`package-info.java`)에 "이 패키지의 DTO는 API 요청/응답 전용"임을 명시
- 서비스 간 내부 전달이 필요할 때는 별도 VO/Command를 domain 패키지에 생성

**추천: MVP에서는 방안 B로 시작하고, Phase 2에서 방안 A로 전환하는 것이 현실적이다.** 다만 새 기능 추가 시부터는 presentation 패키지에 DTO를 만드는 것을 규칙으로 정할 수 있다.

---

## 2. 패키지/클래스 배치 개선점

### 2-1. Enum이 global에 있는 문제

**현재 상태:**
```
global/enums/DocType.java
global/enums/DocumentStatus.java
global/enums/PlanType.java
```

**문제점:** `DocType`과 `DocumentStatus`는 Document 엔티티 전용 enum인데 `global`에 있다. `PlanType`은 User 엔티티 전용이다. 이들이 global에 있으면 도메인 간 의존 관계가 모호해진다. "global"은 어디에도 소속되지 않는 공통 유틸리티를 위한 곳이어야 한다.

**개선 방안:**
```
domain/document/entity/DocType.java         (또는 domain/document/DocType.java)
domain/document/entity/DocumentStatus.java
domain/user/entity/PlanType.java            (또는 domain/user/PlanType.java)
```

enum을 해당 엔티티와 같은 패키지에 두면 도메인 소속이 명확해지고, 패키지 응집도가 높아진다.

### 2-2. storage 도메인의 불완전한 구조

**현재 상태:**
```
domain/storage/entity/StorageUsage.java
domain/storage/repository/StorageUsageRepository.java
```

`storage`가 독립 도메인으로 분리되어 있지만, 서비스 클래스가 없다. `StorageUsageRepository`는 `DocumentTransactionService`와 `AuthService`에서 직접 사용된다.

**문제점:**
- 독립 도메인으로 분리한 의도는 좋지만, 서비스 없이 리포지토리만 다른 도메인 서비스에서 직접 참조하는 것은 계층 분리의 이점을 살리지 못한다
- `DocumentTransactionService`가 `StorageUsageRepository`를 직접 호출하므로, document 도메인이 storage 도메인의 리포지토리 구현에 직접 결합된다

**개선 방안:**
- 현재 규모에서는 `StorageUsage`를 `domain/user` 하위로 이동하는 것이 더 자연스럽다 (사용량은 사용자의 속성)
- 또는 `StorageUsageService`를 만들어 도메인 경계를 유지하고, `DocumentTransactionService`에서는 이를 통해 간접 접근하도록 한다

### 2-3. 컨트롤러에 비즈니스 로직 누출

**현재 상태 (DocumentController.deleteDocument):**
```java
Document deleted = documentService.deleteDocument(slug, userPrincipal.userId());

DocumentDeleteResponse response = new DocumentDeleteResponse(
        deleted.getId(),
        deleted.getSlug(),
        LocalDateTime.now()  // 컨트롤러에서 삭제 시각을 생성
);
```

**문제점:**
- 서비스가 `Document` 엔티티를 그대로 컨트롤러에 반환하고, 컨트롤러가 응답 DTO를 직접 조립한다
- `LocalDateTime.now()`로 삭제 시각을 컨트롤러에서 생성하지만, 실제 soft delete는 서비스 내부에서 이미 완료된 상태이므로 미세한 시간 차이가 발생한다
- 다른 엔드포인트에서는 서비스가 응답 DTO를 반환하는데, 이것만 패턴이 다르다

**개선 방안:**
```java
// DocumentService.deleteDocument()가 DocumentDeleteResponse를 직접 반환하도록 통일
public DocumentDeleteResponse deleteDocument(String slug, UUID userId) {
    // ... 기존 로직 ...
    return new DocumentDeleteResponse(deleted.getId(), deleted.getSlug(), deleted.getUpdatedAt());
}
```

### 2-4. DocumentController에서 size 상한 제한

```java
if (size > 50) {
    size = 50;
}
```

단순하지만 컨트롤러에 비즈니스 규칙이 하드코딩되어 있다. `@Max(50)` 어노테이션을 사용하거나, 서비스 레이어에서 처리하는 것이 더 일관적이다.

### 2-5. UserPrincipal 위치

**현재 상태:** `global/auth/UserPrincipal.java`

Spring Security의 Authentication principal로 사용되므로 `global/auth`에 있는 것이 크게 문제되지는 않으나, `UserPrincipal`은 결국 사용자 인증 정보이므로 `global/security` 또는 현재 위치가 적절하다. 현재 유지해도 된다.

---

## 3. 서비스 계층 설계

### 3-1. DocumentService의 책임 범위

**현재 상태:** DocumentService가 담당하는 책임:
- 문서 생성 (2단계 커밋 오케스트레이션)
- 문서 조회 (공개 서빙, 편집용, 목록)
- 문서 수정
- 문서 삭제
- 입력 검증 (바이트 크기, docType 파싱, 빈 수정 요청)
- 소유권 확인
- R2 키 생성
- ETag용 URL 생성 로직 위임 (frontendUrl 보유)

**문제점:**
- 340줄 규모로 아직 관리 가능하지만, 입력 검증과 비즈니스 로직이 혼재되어 있다
- `parseDocType()` 메서드는 String을 enum으로 변환하는 것인데, DTO나 enum 자체에서 처리하는 것이 더 자연스럽다
- `frontendUrl`을 서비스가 알아야 하는 것은 표현 계층의 관심사가 서비스에 침투한 것이다

**개선 방안:**

(a) `parseDocType()`을 `DocType` enum의 정적 메서드로 이동:
```java
public enum DocType {
    HTML, MARKDOWN;

    public static DocType fromString(String type) {
        return switch (type.toLowerCase()) {
            case "html" -> HTML;
            case "markdown" -> MARKDOWN;
            default -> throw new BusinessException(...);
        };
    }
}
```

(b) `frontendUrl` 기반 URL 생성은 응답 DTO의 팩토리 메서드에서 이미 처리되고 있으므로, 서비스에서 frontendUrl을 주입받아 DTO에 전달하는 현재 방식은 수용 가능하다. 다만 이상적으로는 컨트롤러에서 URL을 조립하는 것이 맞다.

### 3-2. DocumentService와 DocumentTransactionService의 분리

**현재 상태:** DocumentTransactionService는 Spring AOP 프록시 문제를 해결하기 위해 트랜잭션 경계만 담당하는 별도 서비스로 분리되어 있다.

**평가:** 이 분리는 잘 되어 있다. 코드 주석에 분리 이유가 명확히 설명되어 있고, 역할이 뚜렷하다. DocumentTransactionService는 DB 쓰기 트랜잭션만 담당하고, DocumentService는 오케스트레이션을 담당한다.

**개선 포인트:**
- `DocumentTransactionService.deletePendingSingleDocument()`에서 `fileStorage.delete()`를 트랜잭션 내부에서 호출한다. R2 삭제가 실패하면 catch로 계속 진행하지만, 트랜잭션 안에서 외부 I/O를 수행하는 것은 트랜잭션 유지 시간을 늘리는 원인이 된다. R2 삭제를 트랜잭션 밖으로 빼는 것이 좋다.

### 3-3. AuthService의 책임 범위

**현재 상태:** AuthService가 담당하는 책임:
- OAuth2 로그인 (code 교환 -> 사용자 조회/생성 -> 토큰 발급)
- Refresh Token 갱신 (Rotation 포함)
- 로그아웃
- 현재 사용자 조회 (StorageUsage 포함)

**문제점:**
- `getCurrentUser()`가 `StorageUsageRepository`를 직접 참조한다. 사용자 정보 조회에 storage 도메인의 데이터 접근이 섞여 있다
- OAuth 로그인 로직에서 `providerMap.get(provider)`로 provider를 찾는데, null 체크 후 `BusinessException`을 던지는 것은 적절하지만, provider 이름 검증을 enum이나 별도 로직으로 분리하면 더 명확하다

**트랜잭션 경계 평가:**
- `oauthLogin()`: `@Transactional` -- User 조회/생성과 RefreshToken 생성이 하나의 트랜잭션에서 처리된다. 외부 HTTP 호출(`exchangeCode`, `getUserInfo`)이 트랜잭션 안에 있다. 이것은 문제가 될 수 있다. 외부 API 호출이 느려지면 DB 커넥션을 오래 잡게 된다.
- `refreshToken()`: `@Transactional` -- 적절하다. DB 읽기/쓰기가 원자적이어야 한다.
- `logout()`: `@Transactional` -- 적절하다.
- `getCurrentUser()`: `@Transactional(readOnly = true)` -- 적절하다.

**개선 방안:**
`oauthLogin()`에서 외부 API 호출을 트랜잭션 밖으로 분리:
```java
public AuthResponse oauthLogin(String provider, String code, String redirectUri, String state) {
    validateState(state);
    OAuth2Provider oAuth2Provider = resolveProvider(provider);

    // 트랜잭션 밖에서 외부 API 호출
    OAuthTokenResponse tokenResponse = oAuth2Provider.exchangeCode(code, redirectUri);
    OAuthUserInfo userInfo = oAuth2Provider.getUserInfo(tokenResponse.accessToken());

    // 트랜잭션 안에서 DB 작업만
    return processOAuthLogin(provider, userInfo);
}

@Transactional
protected AuthResponse processOAuthLogin(String provider, OAuthUserInfo userInfo) {
    // User 조회/생성 + 토큰 발급
}
```

다만 이것도 AOP 프록시 문제가 있으므로, DocumentService/DocumentTransactionService 패턴을 동일하게 적용하거나 별도 트랜잭션 서비스를 만들어야 한다.

### 3-4. Port 인터페이스 설계 평가

**FileStorage, ContentSanitizer, OAuth2Provider** -- 3개의 Port 인터페이스가 정의되어 있다.

**잘된 점:**
- Port가 domain 패키지에, Adapter가 infra 패키지에 있어 의존성 방향이 올바르다
- 인터페이스가 간결하고 역할이 명확하다
- Javadoc이 잘 작성되어 있다

**개선 포인트:**
- `FileStorage`의 메서드 파라미터명이 `key`인데, Javadoc에서 "R2 키"로 설명한다. Port 인터페이스는 특정 구현(R2)에 독립적이어야 하므로, Javadoc에서 "R2"를 제거하고 "파일 저장소 키"로 표현하는 것이 더 적절하다. 인터페이스 자체는 잘 추상화되어 있다.
- `OAuthTokenResponse`와 `OAuthUserInfo`가 `domain/user/port/`에 있다. 이들은 Port 인터페이스의 반환/입력 타입이므로 Port와 함께 위치하는 것은 적절하다.

---

## 4. 엔티티 설계

### 4-1. JPA 엔티티가 도메인 모델 역할을 겸하는 트레이드오프

**현재 상태:** `Document`, `User`, `RefreshToken`, `StorageUsage` -- JPA 엔티티가 곧 도메인 모델이다.

**트레이드오프 분석:**

| 항목 | 장점 | 단점 |
|------|------|------|
| 개발 속도 | 변환 레이어 없이 바로 사용 | -- |
| 코드량 | 도메인 모델과 엔티티가 하나이므로 코드가 적다 | -- |
| JPA 결합 | -- | 도메인 로직이 JPA 생명주기에 영향 받음 |
| 테스트 | -- | 엔티티 테스트 시 JPA 컨텍스트 의존 가능 |
| 도메인 순수성 | -- | @Entity, @Column 등 인프라 어노테이션 오염 |

**평가:** MVP 규모에서 이 접근은 현실적이다. Document 엔티티에 비즈니스 메서드(`activate()`, `markDeleted()`, `expire()`)가 있어 Anemic Domain Model은 아니다. Phase 2에서 도메인이 복잡해지면 분리를 고려할 수 있다.

### 4-2. Document 엔티티 메서드 평가

```java
public void activate()       -- PENDING -> ACTIVE 전이
public void markDeleted()    -- -> DELETED 전이
public void expire()         -- -> EXPIRED 전이
public void updateTitle()    -- 제목 변경
public void updateContentSize() -- 콘텐츠 크기 변경
public void setExpiresAt()   -- 만료 시간 설정
```

**잘된 점:**
- 상태 전이를 엔티티 메서드로 캡슐화한 것은 좋다
- `activate()`, `markDeleted()`, `expire()`는 의도를 명확히 드러내는 네이밍이다

**문제점:**

(a) **상태 전이 검증이 없다.**
`activate()`는 아무 상태에서나 호출할 수 있다. DELETED 상태에서 `activate()`를 호출하면 다시 ACTIVE가 된다. 상태 전이 규칙을 엔티티 내부에서 검증해야 한다:
```java
public void activate() {
    if (this.status != DocumentStatus.PENDING) {
        throw new IllegalStateException("PENDING 상태에서만 활성화할 수 있습니다. 현재: " + status);
    }
    this.status = DocumentStatus.ACTIVE;
}
```

(b) **`setExpiresAt()`은 setter 패턴이다.**
다른 메서드들은 의도를 드러내는 이름(`markDeleted`, `expire`)인데, 이것만 setter이다. `assignExpiration(LocalDateTime expiresAt)` 같은 이름이 더 일관적이다.

(c) **`updateContentSize()`는 단순 setter이다.**
`updateDocument()` 같은 상위 메서드로 묶어서 title과 contentSize를 함께 변경하는 것이 더 응집도가 높다.

### 4-3. User 엔티티

**잘된 점:** `updateProfile()` 메서드로 프로필 업데이트를 캡슐화했다.

**문제점:** email에 unique 제약이 있는데, 동일 이메일로 다른 OAuth provider에서 가입하면 충돌이 발생한다. 이는 엔티티 설계라기보다 비즈니스 요구사항 문제이지만, 코드에서 이에 대한 처리가 보이지 않는다.

### 4-4. RefreshToken 엔티티

**잘된 점:** BaseEntity를 상속하지 않는 이유가 주석으로 설명되어 있다. `revoke()` 메서드로 상태 변경을 캡슐화했다.

**문제점:** `createdAt`을 `LocalDateTime.now()`로 직접 설정한다. Phase 2에서 Clock 주입 전환 시 이 부분도 변경이 필요하다. 또한 `BaseEntity`의 JPA Auditing과 다른 방식이므로 일관성이 떨어진다.

### 4-5. StorageUsage 엔티티

**문제점:** 엔티티에 비즈니스 메서드가 전혀 없고, `StorageUsageRepository`의 `@Query`를 통한 원자적 증감만으로 관리된다. 엔티티가 완전한 Anemic Model이다. 원자적 증감이 필요한 현재 설계에서는 불가피하지만, 이 트레이드오프를 문서화할 가치가 있다.

---

## 5. 예외 처리 구조

### 현재 구조

```
global/exception/
  BusinessException.java          -- 모든 비즈니스 예외의 부모 (HttpStatus + code + message)
  GlobalExceptionHandler.java     -- @RestControllerAdvice
  ForbiddenException.java         -- 403
  RateLimitExceededException.java -- 429

domain/document/exception/
  DocumentNotFoundException.java  -- 404
  DocumentExpiredException.java   -- 410
  DocumentGoneException.java      -- 410
  ContentTooLargeException.java   -- 400

infra/storage/StorageException.java   -- 500
infra/oauth2/OAuth2Exception.java     -- 502/400
```

### 잘된 점

- `BusinessException`을 루트로 한 예외 계층이 일관적이다
- `GlobalExceptionHandler`에서 `BusinessException`을 한 번에 처리하는 구조가 깔끔하다
- 도메인별 예외(`DocumentNotFoundException` 등)가 해당 도메인 패키지에 있어 응집도가 높다
- 인프라 예외(`StorageException`, `OAuth2Exception`)가 infra 패키지에 있어 적절하다
- HTTP 상태 코드가 의미에 맞게 잘 매핑되어 있다

### 문제점

**5-1. `ForbiddenException`이 global에 있다.**

`ForbiddenException`은 현재 `DocumentService.verifyOwnership()`에서만 사용된다. global에 있어도 되지만, 여러 도메인에서 공통으로 사용할 예외가 아니라면 `domain/document/exception/`에 있는 것이 응집도가 더 높다. 만약 User 도메인에서도 사용한다면 global이 맞다.

**5-2. `BusinessException`에서 `HttpStatus`를 직접 사용한다.**

도메인 예외가 HTTP 상태 코드를 알아야 하는 것은 표현 계층의 관심사가 도메인에 침투한 것이다. 이상적으로는 도메인 예외는 비즈니스 의미만 담고, HTTP 매핑은 `GlobalExceptionHandler`에서 처리해야 한다. 다만 이는 Spring 생태계에서 매우 일반적인 패턴이고, MVP에서 과도한 추상화는 오히려 비생산적이므로 현재 방식도 수용 가능하다.

**5-3. `DocumentExpiredException`과 `DocumentGoneException`의 의미 중복.**

둘 다 410 Gone을 반환한다. HTTP 관점에서는 같은 상태 코드인데, 코드("DOCUMENT_EXPIRED" vs "DOCUMENT_GONE")로만 구분한다. 클라이언트가 이 두 코드를 다르게 처리해야 하는지 명확하지 않다. 처리 방식이 같다면 하나로 통합하는 것이 좋다.

**5-4. GlobalExceptionHandler에서 Validation 에러 처리가 첫 번째 필드 에러만 반환한다.**

```java
.findFirst()
.map(error -> error.getField() + ": " + error.getDefaultMessage())
```

여러 필드가 동시에 유효성 검증에 실패할 수 있는데, 첫 번째 에러만 반환하면 클라이언트가 여러 번 요청해야 모든 에러를 파악할 수 있다. 모든 에러를 한 번에 반환하는 것이 UX에 더 좋다.

---

## 6. 기타 코드 품질

### 6-1. record 타입 활용 (잘 됨)

`ApiResponse`, `UserPrincipal`, `JwtProperties`, `R2Properties`, `OAuth2Properties`, 모든 DTO와 Port 관련 VO -- record를 적극적으로, 올바르게 활용하고 있다. 불변성이 보장되고 boilerplate가 줄어든다.

**한 가지 개선점:** `ApiResponse.error()`의 반환 타입이 `ApiResponse<?>`인데, 제네릭 와일드카드를 사용하면 타입 안전성이 떨어진다. `ApiResponse<Void>`가 더 명확하다.

### 6-2. @ConfigurationProperties 구조 (잘 됨)

`JwtProperties`, `R2Properties`, `OAuth2Properties` -- record 기반 `@ConfigurationProperties`를 사용하여 불변 설정을 보장한다. 계층적 설정(`OAuth2Properties.ProviderProperties`)도 적절하다.

**개선점:** `R2Properties`와 `R2Config`가 infra/storage에, `OAuth2Properties`와 `OAuth2Config`가 infra/oauth2에 있다. 일관적이다. 다만 `JwtProperties`는 `global/auth`에 있고 `@EnableConfigurationProperties`는 `SecurityConfig`에서 처리된다. 각 Properties를 사용하는 곳에서 Enable하는 패턴은 되지만, 별도 `@Configuration`으로 묶는 것이 더 명확할 수 있다.

### 6-3. 테스트 용이성

**현재 문제점:**

(a) **`LocalDateTime.now()` 직접 호출:**
코드 전반에서 `LocalDateTime.now()`를 직접 호출한다 (DocumentService, AuthService, DocumentCleanupService, RefreshToken 생성자, StorageUsage 생성자). 주석에 "Phase 2에서 Clock 주입으로 전환"이라고 적혀 있다. Clock 주입 없이는 시간 관련 테스트가 어렵다.

영향 범위:
- `DocumentService.createDocument()` -- `expiresAt` 계산
- `DocumentService.getDocumentForView()` -- 만료 확인
- `DocumentCleanupService` -- cutoff 시간 계산
- `AuthService.issueTokens()` -- RefreshToken 만료 계산
- `RefreshToken` 생성자 -- `createdAt`
- `DocumentController.deleteDocument()` -- `LocalDateTime.now()`로 `deletedAt` 생성

(b) **`DocumentService`에 `@Transactional`이 없다:**
`DocumentService`는 읽기 전용 메서드에도 `@Transactional(readOnly = true)`가 없다. `getDocumentForView()`, `getMyDocuments()`, `getDocumentForEdit()` 등은 읽기 전용 트랜잭션을 명시하는 것이 좋다. 이는 성능(읽기 전용 최적화)과 의도 표현 양면에서 이점이 있다.

(c) **SlugGenerator가 DocumentRepository에 직접 의존:**
`SlugGenerator`는 `@Component`로 등록되어 있고 `DocumentRepository`에 의존한다. 단위 테스트 시 리포지토리를 mock해야 한다. 현재 구조에서는 불가피하지만, `existsBySlug(String slug)` 같은 경량 메서드를 사용하면 `findBySlug`보다 효율적이다.

### 6-4. R2FileStorage의 동기 I/O

`download()`에서 `getObjectAsBytes()`를 사용하여 전체 파일을 메모리에 로드한다. 5MB 제한이 있으므로 현재는 괜찮지만, 동시 요청이 많으면 메모리 압박이 생길 수 있다. Phase 2에서 스트리밍 방식 전환을 고려할 수 있다.

### 6-5. RateLimitFilter의 내부 TokenBucket 클래스

`RateLimitFilter` 안에 `TokenBucket` static inner class가 있다. 약 200줄 규모의 파일에서 필터와 버킷 구현이 함께 있어 응집도가 높다. 규모가 커지면 분리를 고려할 수 있지만, 현재는 적절하다.

다만 `@Scheduled`로 버킷 정리를 하는 것은 필터의 책임이 아닌 인프라 관심사이다. 분리하면 테스트가 더 쉬워진다.

### 6-6. DocumentCleanupService에서 Sentry 직접 호출

```java
Sentry.captureException(e);
```

`GlobalExceptionHandler` 주석에는 "sentry-spring-boot-starter가 미처리 예외를 자동 보고하므로 직접 호출하지 않는다"고 되어 있는데, `DocumentCleanupService`에서는 직접 호출한다. 이는 스케줄러에서 발생하는 예외가 `@RestControllerAdvice`를 타지 않기 때문에 의도적인 것이다. 이 차이를 주석으로 설명하면 좋다.

---

## 개선 우선순위 요약

### Critical (반드시 수정 권장)

1. **Document 엔티티 상태 전이 검증 부재** -- `activate()`가 아무 상태에서나 호출 가능하여 잘못된 상태 전이가 발생할 수 있다
2. **AuthService.oauthLogin()에서 외부 API 호출이 @Transactional 안에 있다** -- 외부 API 지연 시 DB 커넥션을 장시간 점유한다

### Major (품질을 크게 높이는 개선)

3. **Enum을 global에서 해당 도메인으로 이동** -- 도메인 응집도 향상
4. **DTO 위치 정리** -- 최소한 `package-info.java`로 DTO의 역할을 명시하고, 새 기능부터 presentation 분리 시작
5. **DocumentController.deleteDocument()의 응답 DTO 조립을 서비스로 이동** -- 컨트롤러/서비스 간 패턴 일관성
6. **DocumentService 읽기 메서드에 @Transactional(readOnly = true) 추가**
7. **DocumentTransactionService.deletePendingSingleDocument()에서 R2 삭제를 트랜잭션 밖으로 이동**

### Minor (있으면 좋은 개선)

8. **`setExpiresAt()`을 의도를 드러내는 이름으로 변경**
9. **GlobalExceptionHandler의 Validation 에러에서 모든 필드 에러 반환**
10. **`ApiResponse.error()` 반환 타입을 `ApiResponse<Void>`로 변경**
11. **`DocumentRepository.findBySlug()`를 `existsBySlug()`로 대체 (SlugGenerator)**
12. **`DocumentCleanupService`의 Sentry 직접 호출 이유를 주석으로 설명**
