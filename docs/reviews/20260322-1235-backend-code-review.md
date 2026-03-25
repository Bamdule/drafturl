# DraftURL 백엔드 코드 리뷰

> 검토일: 2026-03-22
> 검토 대상: `backend/src/main/java/com/drafturl/api/` 하위 전체 Java 소스 (65개 파일)
> 참조 설계 문서: `docs/architecture/backend/README.md`, `docs/architecture/backend/api.md`

---

## 요약

전체적으로 설계 문서에 정의된 3계층(domain/infra/global) 구조와 Port-Adapter 패턴을 충실히 구현하고 있다. Java 21 Record를 DTO에 적극 활용하고, 커스텀 Bean Validation, 토큰 버킷 Rate Limiter, 2단계 커밋 패턴 등이 잘 설계되어 있다. 다만 **스케줄러 메서드의 트랜잭션 부재**, **ETag 이중 DB 조회**, **StorageUsage 음수 가능성**, **OAuth2 state 검증의 CSRF 방어 불충분** 등 운영 환경에서 문제가 될 수 있는 이슈가 식별되었다.

---

## 조치 우선순위 테이블

| 우선순위 | ID | 분류 | 제목 | 위치 |
|---------|-----|------|------|------|
| CRITICAL | C-1 | 보안 | OAuth2 state 검증이 CSRF를 실질적으로 방어하지 못함 | `AuthService.validateState()` |
| CRITICAL | C-2 | 데이터 정합성 | 스케줄러 메서드에 `@Transactional` 없음 -- 부분 실패 시 데이터 불일치 | `DocumentService.processExpiredDocuments()` 외 |
| CRITICAL | C-3 | 데이터 정합성 | `StorageUsage.totalBytes`/`documentCount`가 음수로 갈 수 있음 | `StorageUsageRepository` |
| CRITICAL | C-4 | 보안 | JsoupContentSanitizer가 Safelist를 정의하지만 실제로 사용하지 않음 | `JsoupContentSanitizer.sanitize()` |
| MAJOR | M-1 | 성능 | viewDocument에서 ETag 생성 시 DB를 이중 조회함 | `DocumentController.viewDocument()` |
| MAJOR | M-2 | 설계 | `DocumentService`가 500줄로 비대 -- 스케줄러 로직 분리 필요 | `DocumentService` |
| MAJOR | M-3 | 에러 처리 | `SlugGenerator.generate()`가 `IllegalStateException`을 던짐 -- `BusinessException`이어야 함 | `SlugGenerator` |
| MAJOR | M-4 | 설계 | `ForbiddenException` 클래스가 두 개 존재 (document/user) | `document.exception`, `user.exception` |
| MAJOR | M-5 | 보안 | `UpdateDocumentRequest`에 `@Valid` 누락 | `DocumentController.updateDocument()` |
| MAJOR | M-6 | 에러 처리 | OAuth2 Provider RestClient 호출에 에러 핸들링 부족 | `GoogleOAuth2Provider`, `GitHubOAuth2Provider` |
| MAJOR | M-7 | 설계 | `MdcFilter`에서 userId 추출이 `getName()`을 사용 -- UserPrincipal의 toString이 아닌 userId를 직접 꺼내야 함 | `MdcFilter.extractUserId()` |
| MAJOR | M-8 | 성능 | 스케줄러 배치 처리에서 개별 save/delete 호출 -- N+1 쓰기 | `DocumentService.processExpiredDocuments()` 외 |
| MINOR | m-1 | 코드 품질 | `BaseEntity`에 `LocalDateTime` 사용 -- 타임존 이슈 가능 | `BaseEntity`, `Document`, `RefreshToken` |
| MINOR | m-2 | 코드 품질 | `StorageException` 생성자에서 `initCause()` 대신 `super()`로 cause를 전달해야 함 | `StorageException` |
| MINOR | m-3 | 코드 품질 | `Document.id`와 `Document.slug`가 동일 값 -- 중복 필드 | `Document` 엔티티 |
| MINOR | m-4 | 설정 | `R2FileStorage`에 `@Service` 대신 `@Component`가 더 적절 | `R2FileStorage` |
| MINOR | m-5 | 코드 품질 | `RefreshToken.createdAt`이 BaseEntity를 상속하지 않고 직접 관리 | `RefreshToken` |
| MINOR | m-6 | 테스트 용이성 | `LocalDateTime.now()` 직접 호출 -- Clock 주입 패턴이 더 테스트 가능 | 전반 |

---

## 상세 리뷰

### CRITICAL 이슈

#### C-1. OAuth2 state 검증이 CSRF를 실질적으로 방어하지 못함

**파일**: `domain/user/service/AuthService.java` (라인 150-155)

```java
private void validateState(String state) {
    if (state == null || state.isBlank()) {
        throw new BusinessException(...);
    }
}
```

**문제**: state 값이 비어있지 않은지만 확인하고, **서버 측에 저장된 기대값과의 비교를 수행하지 않는다**. 설계 문서(`api.md` 라인 336)에는 "로그인 시작 시 생성하여 쿠키에 저장한 값과 일치해야 함"이라고 명시되어 있지만, 실제 구현은 null 체크만 한다. 공격자가 임의의 state 값으로 콜백 요청을 보낼 수 있다.

**제안**: 프론트엔드에서 state를 생성하여 쿠키/세션에 저장하고 백엔드로 함께 전달하는 방식이면, 프론트엔드 측에서 state를 비교한 후 백엔드에 코드를 전달하는 구조가 되어야 한다. 백엔드에서 검증할 것이라면 서버 사이드 세션 또는 Redis에 state를 저장하고 비교해야 한다. 현재 Stateless JWT 아키텍처에서 state 검증의 책임 소재를 명확히 해야 한다.

---

#### C-2. 스케줄러 메서드에 `@Transactional` 없음

**파일**: `domain/document/service/DocumentService.java` (라인 362-471)

`processExpiredDocuments()`, `cleanupPendingDocuments()`, `cleanupOrphanDocuments()` 메서드가 `@Transactional` 없이 개별 `documentRepository.save()`/`delete()`와 `storageUsageRepository.incrementUsage()`를 호출한다.

```java
// processExpiredDocuments() 내부 -- 트랜잭션 없음
document.expire();
documentRepository.save(document);   // 성공
// 여기서 예외 발생하면?
storageUsageRepository.incrementUsage(document.getUserId(), -document.getContentSize(), -1);  // 실행 안 됨
```

**문제**: `save()`가 성공한 후 `incrementUsage()`가 실패하면 문서는 EXPIRED인데 StorageUsage는 감소하지 않는 불일치가 발생한다.

**제안**: 개별 문서 처리를 별도의 `@Transactional` 메서드로 추출하거나, `DocumentTransactionService`에 `expireDocument(id, userId, contentSize)` 메서드를 추가하여 트랜잭션 경계를 설정한다.

---

#### C-3. StorageUsage 값이 음수로 갈 수 있음

**파일**: `domain/storage/repository/StorageUsageRepository.java` (라인 33-40)

```sql
UPDATE StorageUsage s SET s.totalBytes = s.totalBytes - :bytes,
    s.documentCount = s.documentCount - :count ...
```

**문제**: DB 레벨에서 `totalBytes >= 0` 또는 `documentCount >= 0` 제약 조건이 없으면, 스케줄러의 만료 처리와 사용자의 삭제 요청이 동시에 실행될 때 이중 차감으로 음수 값이 발생할 수 있다. 예를 들어 문서가 만료 처리 중에 사용자가 동시에 삭제 요청을 보내면 contentSize가 두 번 차감된다.

**제안**:
1. DDL에 `CHECK (total_bytes >= 0)`, `CHECK (document_count >= 0)` 제약 조건 추가
2. 또는 JPQL에 `GREATEST(s.totalBytes - :bytes, 0)` 사용
3. 만료 처리 시 문서 상태를 먼저 확인하여 이미 DELETED인 문서는 건너뛰는 로직 추가

---

#### C-4. JsoupContentSanitizer가 Safelist를 정의하지만 사용하지 않음

**파일**: `infra/sanitizer/JsoupContentSanitizer.java`

```java
private static final Safelist SAFELIST = createSafelist();  // 정의됨

@Override
public String sanitize(String html) {
    Document document = Jsoup.parse(html);           // parse만 수행
    document.select("iframe, object, embed").remove(); // 수동 제거
    // ...
    return document.html();
}
```

**문제**: `SAFELIST`가 정의되어 있지만 `Jsoup.clean(html, SAFELIST)` 호출이 없다. `Jsoup.parse()`는 HTML을 파싱만 하고 새니타이징하지 않는다. 현재 구현은 iframe/object/embed와 javascript: URL만 수동 제거하고, Safelist에 정의하지 않은 다른 위험한 태그(예: `<applet>`, `<form action="evil">`)나 속성은 통과시킨다.

**제안**:
- 설계 의도가 "sandbox iframe에서 격리하므로 느슨한 새니타이징"이라면 주석으로 명시하고, 사용하지 않는 `SAFELIST` 필드를 제거한다.
- 엄격한 새니타이징이 목적이라면 `Jsoup.clean(html, SAFELIST)`를 사용한다. 단, `clean()`은 body fragment만 반환하므로 전체 HTML 문서 구조가 깨질 수 있어 별도 처리가 필요하다.
- 현재 코드는 두 방식 사이에 걸쳐 있어 혼란을 준다.

---

### MAJOR 이슈

#### M-1. viewDocument에서 ETag 생성 시 DB 이중 조회

**파일**: `domain/document/controller/DocumentController.java` (라인 66-78)

```java
DocumentViewResponse response = documentService.getDocumentForView(slug); // DB 조회 1회 + R2 조회
String etag = documentService.generateETag(slug);                         // DB 조회 1회 추가
```

**문제**: 동일한 slug로 DB를 두 번 조회한다. `generateETag()`는 별도로 `documentRepository.findBySlug()`를 호출한다.

**제안**: `getDocumentForView()` 반환값에 updatedAt을 포함시키거나, ETag 값을 DocumentViewResponse 또는 별도 래퍼에 담아 한 번의 서비스 호출로 처리한다.

---

#### M-2. DocumentService가 500줄로 비대

**파일**: `domain/document/service/DocumentService.java`

스케줄러 전용 메서드(`processExpiredDocuments`, `cleanupPendingDocuments`, `purgeDeletedDocuments`, `cleanupOrphanDocuments`)가 DocumentService에 포함되어 CRUD 로직과 배치 로직이 혼재한다.

**제안**: `DocumentCleanupService` 같은 별도 서비스로 스케줄러 로직을 분리한다. 이렇게 하면 C-2의 트랜잭션 경계 문제도 더 깔끔하게 해결할 수 있다.

---

#### M-3. SlugGenerator가 IllegalStateException을 던짐

**파일**: `domain/document/service/SlugGenerator.java` (라인 40)

```java
throw new IllegalStateException("slug 생성 실패: 최대 재시도 횟수(" + MAX_RETRIES + ")를 초과했습니다");
```

**문제**: `IllegalStateException`은 `GlobalExceptionHandler`의 `BusinessException` 핸들러에 잡히지 않고 최종 fallback `Exception` 핸들러에 잡혀 500 INTERNAL_ERROR가 된다. 사용자에게는 "예상치 못한 서버 오류"라고 표시되지만, 실제로는 예상 가능한 상황이다.

**제안**: `BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "...")`으로 교체하거나 전용 예외 클래스를 만든다.

---

#### M-4. ForbiddenException 클래스가 두 개 존재

- `domain/document/exception/ForbiddenException` (인자 없는 생성자)
- `domain/user/exception/ForbiddenException` (String message 생성자)

**문제**: 같은 이름의 예외가 두 패키지에 존재하여 import 혼란을 유발한다. 두 클래스 모두 같은 HTTP 403 + "FORBIDDEN" 코드를 사용한다.

**제안**: `global/exception/ForbiddenException`으로 통합하거나, 도메인별로 명확히 구분할 필요가 있으면 `DocumentForbiddenException`, `UserForbiddenException`으로 네이밍한다.

---

#### M-5. UpdateDocumentRequest에 @Valid 누락

**파일**: `domain/document/controller/DocumentController.java` (라인 124-127)

```java
@PutMapping("/{slug}")
public ResponseEntity<...> updateDocument(
        @PathVariable String slug,
        @RequestBody UpdateDocumentRequest request,  // @Valid 없음
        ...
```

**문제**: `CreateDocumentRequest`에는 `@Valid`가 붙어 있지만 `UpdateDocumentRequest`에는 빠져 있다. 현재 `UpdateDocumentRequest`에 Bean Validation 어노테이션이 없어 당장 동작에 영향은 없지만, 향후 검증 규칙을 추가할 때 누락될 수 있다.

**제안**: `@Valid`를 추가하고, content 필드에 `@ContentSize` 어노테이션도 적용을 고려한다 (5MB 제한은 서비스 계층에서 수동 체크하고 있지만, Bean Validation과 이중으로 가드하는 것이 일관적이다).

---

#### M-6. OAuth2 Provider RestClient 호출에 에러 핸들링 부족

**파일**: `infra/oauth2/GoogleOAuth2Provider.java`, `infra/oauth2/GitHubOAuth2Provider.java`

```java
GoogleTokenResponse response = restClient.post()
        .uri(TOKEN_URL)
        .body(request)
        .retrieve()                    // HTTP 4xx/5xx 시 RestClientResponseException 발생
        .body(GoogleTokenResponse.class);
```

**문제**: RestClient의 `retrieve()`는 HTTP 에러 응답 시 `RestClientResponseException`을 던진다. 이 예외는 `GlobalExceptionHandler`의 `Exception` fallback에 잡혀 500 INTERNAL_ERROR + Sentry 알림이 된다. 그러나 Google/GitHub 측 에러(예: 잘못된 인가 코드)는 Sentry에 보낼 필요 없는 정상적인 실패 케이스다.

**제안**: `.onStatus()` 핸들러를 추가하여 4xx는 `OAuth2Exception`으로 래핑하고, 5xx만 Sentry 보고 대상으로 남긴다.

---

#### M-7. MdcFilter에서 userId 추출 로직 문제

**파일**: `global/filter/MdcFilter.java` (라인 35-42)

```java
private String extractUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();  // UsernamePasswordAuthenticationToken.getName()
    }
    return "anonymous";
}
```

**문제**: `UsernamePasswordAuthenticationToken`의 `getName()`은 principal의 `toString()`을 호출한다. `UserPrincipal`은 record이므로 `toString()`은 `UserPrincipal[userId=..., email=..., plan=...]` 형태가 된다. userId만 MDC에 기록하려는 의도와 다르다.

**제안**:
```java
if (authentication.getPrincipal() instanceof UserPrincipal principal) {
    return principal.userId().toString();
}
```

---

#### M-8. 스케줄러 배치 처리에서 개별 save/delete 호출

**파일**: `domain/document/service/DocumentService.java` (라인 370-402, 419-442)

```java
for (Document document : expiredDocuments) {
    document.expire();
    documentRepository.save(document);  // 건별 UPDATE
}
```

**문제**: 100건 배치 처리 시 100번의 개별 UPDATE/DELETE SQL이 실행된다.

**제안**: `documentRepository.saveAll(batch)`, `documentRepository.deleteAllInBatch(batch)` 또는 벌크 UPDATE JPQL을 사용한다. StorageUsage 업데이트도 합산하여 한 번에 처리할 수 있다.

---

### MINOR 이슈

#### m-1. BaseEntity에 LocalDateTime 사용

`BaseEntity`, `Document.expiresAt`, `RefreshToken.expiresAt` 등이 `LocalDateTime`을 사용한다. `LocalDateTime`은 타임존 정보가 없어 서버 타임존에 의존한다. Railway 배포 환경의 기본 타임존이 UTC가 아니면 만료 시간 계산에 오차가 발생할 수 있다.

**제안**: `Instant` 또는 `OffsetDateTime`으로 전환하거나, application.yml에 `spring.jackson.time-zone=UTC` + JVM 옵션 `-Duser.timezone=UTC`를 설정한다.

---

#### m-2. StorageException 생성자에서 cause 전달 방식

```java
public StorageException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", message);
    initCause(cause);  // 비표준 패턴
}
```

**문제**: `BusinessException`의 부모인 `RuntimeException`에 `(String, Throwable)` 생성자가 있으므로, `BusinessException`에 cause를 받는 생성자를 추가하고 `super(message, cause)`로 전달하는 것이 표준 패턴이다. `initCause()`는 cause가 이미 설정되었을 때 `IllegalStateException`을 던질 수 있다.

---

#### m-3. Document.id와 Document.slug가 동일

`DocumentService.createDocument()`에서 `String id = slug;`로 동일 값을 할당한다. 엔티티에 `id`(PK)와 `slug`(UNIQUE) 두 필드가 있지만 값이 항상 같다.

**제안**: MVP에서는 현행 유지가 합리적이나, 향후 slug 변경 기능 추가 시 id와 slug를 분리할 수 있음을 주석으로 명시하면 좋다.

---

#### m-4. R2FileStorage에 @Service 사용

```java
@Service
public class R2FileStorage implements FileStorage {
```

`@Service`는 비즈니스 로직 계층에 사용하는 것이 관례이고, 인프라 어댑터에는 `@Component`가 더 적절하다. `JsoupContentSanitizer`도 동일. 기능적 차이는 없지만 계층 의도를 명확히 전달한다.

---

#### m-5. RefreshToken이 BaseEntity를 상속하지 않음

`RefreshToken`은 `createdAt`을 직접 `LocalDateTime.now()`로 설정하고, `updatedAt`이 없다. `BaseEntity`를 상속하면 `@CreatedDate` 자동 관리와 일관성을 확보할 수 있다. 단, `updatedAt`이 필요 없다면 현행 유지도 가능하다.

---

#### m-6. LocalDateTime.now() 직접 호출

`DocumentService`, `AuthService`, `RefreshToken` 등에서 `LocalDateTime.now()`를 직접 호출한다. 단위 테스트에서 시간 관련 로직(만료 확인 등)을 검증하기 어렵다.

**제안**: `java.time.Clock`을 Bean으로 등록하고 주입받아 `LocalDateTime.now(clock)` 형태로 사용하면 테스트에서 시간을 고정할 수 있다.

---

## 잘된 점

1. **Port-Adapter 패턴 충실히 구현**: `FileStorage`, `ContentSanitizer`, `OAuth2Provider` 인터페이스를 domain에 정의하고 infra에서 구현. 의존성 방향이 설계 문서대로 domain -> global, infra -> domain 방향을 잘 지킨다.

2. **2단계 커밋 패턴**: `DocumentTransactionService`를 분리하여 Spring AOP 프록시의 self-invocation 문제를 회피하고, PENDING -> ACTIVE 상태 전이를 안전하게 처리한다. 실패 시나리오 별 복구 전략이 주석으로 명확히 문서화되어 있다.

3. **커스텀 Bean Validation (`@ContentSize`)**: 5MB 크기 제한을 어노테이션 기반으로 선언적으로 처리하면서, 서비스 계층에서도 방어적으로 재검증하는 이중 가드가 좋다.

4. **토큰 버킷 Rate Limiter**: 직접 구현한 `RateLimitFilter`가 인메모리 `ConcurrentHashMap` + `synchronized TokenBucket`으로 정확한 토큰 버킷 알고리즘을 구현하고, stale 버킷 정리까지 포함한다.

5. **Refresh Token Rotation + 탈취 감지**: 이미 revoke된 토큰 재사용 시 해당 사용자의 모든 토큰을 일괄 무효화하는 보안 로직이 구현되어 있다.

6. **Record 타입 활용**: DTO, Properties, Port 응답 등에 Java Record를 적극 활용하여 보일러플레이트를 최소화했다.

7. **에러 코드 체계**: `BusinessException` 계층 구조로 HTTP 상태 코드 + 비즈니스 에러 코드를 일관되게 관리하며, API 명세의 에러 코드 목록과 일치한다.

8. **MDC 필터**: 요청별 requestId와 userId를 MDC에 설정하여 구조화된 로깅의 기반을 갖추었다.

---

## 추가 고려사항

1. **CORS 헤더에 `X-Forwarded-For` 미포함**: `SecurityConfig.corsConfigurationSource()`의 `setAllowedHeaders`에 `X-Forwarded-For`가 포함되지 않았다. 브라우저에서 이 헤더를 직접 보내지는 않으므로 당장 문제는 없지만, 설계 문서의 공통 헤더 목록과 불일치한다.

2. **Sentry 직접 호출**: `GlobalExceptionHandler`, `DocumentService`, 스케줄러에서 `Sentry.captureException()`를 직접 호출한다. `sentry-spring-boot-starter`의 자동 통합을 사용하면 `@ExceptionHandler`에서 500 응답을 반환하는 예외를 자동으로 Sentry에 보고할 수 있다. 직접 호출과 자동 보고가 겹치면 동일 예외가 Sentry에 두 번 전송될 수 있다.

3. **API 명세와의 차이**: `api.md`에서 `GET /api/v1/documents`의 sort 파라미터(`updatedAt,desc`, `createdAt,asc`, `title,asc`)를 지원한다고 명시하지만, 컨트롤러와 서비스에서는 `updatedAt desc`로 고정 조회한다. sort 파라미터를 받지 않는다.

4. **GitHub OAuth2: email이 null일 수 있음**: GitHub은 email을 비공개로 설정한 사용자의 경우 `/user` API에서 `email: null`을 반환한다. `User` 엔티티의 `email` 필드는 `nullable = false`이므로 DB INSERT 시 예외가 발생한다. GitHub `/user/emails` API를 추가 호출하여 primary email을 가져오는 처리가 필요하다.

5. **ContentSizeValidator의 메모리 사용**: `value.getBytes(StandardCharsets.UTF_8).length`는 전체 문자열의 UTF-8 바이트 배열을 생성한다. 5MB 문자열이면 추가로 5MB의 byte[]가 생성된다. 이후 `DocumentService.createDocument()`에서도 동일한 `getBytes()` 호출이 있어 최악의 경우 15MB(원본 String + Validator byte[] + Service byte[])가 힙에 동시에 존재할 수 있다.

6. **`deleteDocument`에서 R2 삭제 후 DB 실패 시 복구 불가**: `DocumentService.deleteDocument()`는 R2 파일을 먼저 삭제한 후 DB soft delete를 수행한다. DB 업데이트 실패 시 R2 파일은 이미 삭제되었으나 문서는 여전히 ACTIVE 상태다. `getDocumentForView()`에서 R2 다운로드 시 `StorageException`이 발생한다. 생성의 2단계 커밋 패턴처럼 삭제도 순서를 `DB soft delete -> R2 삭제`로 바꾸면 안전하다.
