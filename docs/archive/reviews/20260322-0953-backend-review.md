# backend.md 검토 결과

> 검토일: 2026-03-22
> 검토 대상: docs/architecture/backend.md (2026-03-21 작성)
> 참조 문서: architecture/domain.md, architecture/frontend.md, architecture/infrastructure.md, product.md

---

## 요약

backend.md는 약 1,060줄 규모의 백엔드 설계 문서로, API 명세, 인증/인가, 보안, 핵심 로직, 패키지 구조를 높은 완성도로 다루고 있다. 전체적으로 구현에 바로 착수할 수 있는 수준이며, 특히 인증 플로우(OAuth2 + JWT + Refresh Token Rotation)와 2단계 커밋 패턴의 설계가 치밀하다. 주요 개선 포인트는 (1) 실제 코드의 패키지 구조와 문서의 3계층 구조가 불일치하는 점, (2) HTML 새니타이징을 정규식으로 처리하겠다는 위험한 전략, (3) 문서 수정 시 R2 -> DB 순서에서 DB 실패 시 정합성 문제, (4) domain.md/infrastructure.md와의 중복 내용 정리 필요성이다.

---

## 잘된 점

1. **API 명세가 구현 가능한 수준이다.** 각 엔드포인트의 요청/응답 JSON, 필수/선택 필드, HTTP 상태 코드, 에러 코드가 일관되게 기술되어 있어 프론트엔드와 백엔드 개발자가 동시에 작업을 시작할 수 있다.

2. **인증 설계가 치밀하다.** OAuth2 state 흐름(생성 -> 쿠키 저장 -> 검증 -> 삭제)을 7단계로 명시하고, Refresh Token Rotation의 탈취 감지 시나리오까지 포함한 점이 우수하다. 시퀀스 다이어그램이 함께 제공되어 이해도가 높다.

3. **2단계 커밋 패턴의 실패 복구가 명확하다.** 문서 생성 시 PENDING -> ACTIVE 전이에서 5/6/7단계 각각의 실패 케이스와 복구 전략(스케줄러 정리)이 구체적으로 기술되어 있다.

4. **에러 코드 체계가 일관적이다.** 섹션 5의 에러 코드 총괄 테이블이 HTTP 상태 코드, 커스텀 에러 코드, 상황 설명을 한눈에 볼 수 있도록 정리되어 있으며, 각 API 상세에서도 동일 코드를 참조한다.

5. **패키지 구조의 의존성 규칙이 명시적이다.** domain -> infra/global, infra -> global, global은 독립이라는 규칙과 금지 방향(infra -> domain 불가, global -> domain/infra 불가)이 표로 정리되어 있다.

6. **보안 설계가 다층적이다.** sandbox iframe(allow-same-origin 제거) + CSP 헤더 + 서버 새니타이징의 3중 방어 레이어를 명확히 구분하고, 각 레이어의 역할과 트레이드오프를 설명한다.

---

## Critical (치명적 문제)

### C-1. 실제 패키지 구조와 문서의 3계층(domain/infra/global) 구조 불일치

섹션 10에서 `com.drafturl.api` 아래 `domain/`, `infra/`, `global/` 3계층 구조를 정의하고 있다. 그러나 실제 코드에서 `SecurityConfig.java`는 `com.drafturl.api.config` 패키지에 위치하며, 이는 문서가 정의한 `global/config/SecurityConfig.java` 경로와 다르다.

현재 프로젝트는 초기 스캐폴딩 단계(Application 클래스 + SecurityConfig만 존재)이므로, 코드가 더 진행되기 전에 둘 중 하나를 확정해야 한다:
- (a) 문서대로 3계층 구조로 코드를 리팩터링한다 (`config` -> `global/config`)
- (b) 문서를 현재 코드의 flat 구조에 맞게 수정한다

**제안:** 문서의 3계층 구조가 더 나은 설계이므로 (a)를 권장한다. 다만 현재 코드와의 갭을 인지하고, 구현 착수 전에 패키지를 먼저 정리하는 것이 좋다.

### C-2. HTML 새니타이징을 정규식으로 처리하겠다는 전략은 위험하다

섹션 14(구현 주의사항)에서 "Java에서 정규식 기반 경량 처리. jsoup 등 HTML 파서 라이브러리 사용도 검토 가능하나 MVP에서는 정규식으로 충분"이라고 기술하고 있다.

**문제점:**
- HTML은 정규 언어가 아니므로 정규식으로 안전하게 파싱할 수 없다. `<iframe>`, `<object>`, `<embed>`, `javascript:` 패턴을 정규식으로 제거하려 하면 대소문자 혼합(`<iFrame>`), HTML 엔티티 인코딩(`&#106;avascript:`), 속성 내 줄바꿈, 주석 내 태그 등으로 우회 가능하다.
- 이 서비스는 사용자가 임의의 HTML을 업로드하는 것이 핵심 기능이므로, 새니타이징 우회는 직접적인 보안 위협이다.
- sandbox iframe + CSP가 2차 방어선이라고 하더라도, 방어 레이어 중 하나를 의도적으로 약하게 만들 이유가 없다.

**제안:** jsoup의 `Safelist` 기반 allowlist 방식 새니타이징을 사용한다. jsoup은 Spring Boot 생태계에서 널리 쓰이는 라이브러리이며, 의존성 하나 추가로 보안을 크게 강화할 수 있다. "MVP에서는 정규식으로 충분"이라는 판단을 재고해야 한다.

### C-3. 문서 수정(7.3) 시 R2 실패/DB 실패 정합성 미해결

섹션 7.3(문서 수정)에서 R2 덮어쓰기(5단계) -> DB UPDATE(6단계) 순서로 처리한다. 그런데:
- R2 덮어쓰기 성공 후 DB UPDATE 실패 시: R2에는 새 콘텐츠가 저장되었지만 DB의 `content_size`, `updated_at`은 갱신되지 않는다. 문서 서빙 시 새 콘텐츠가 노출되지만 메타데이터는 이전 상태이다.
- R2는 기존 파일을 덮어쓰므로 이전 버전으로 롤백할 수 없다.

문서 생성(7.1)에서는 PENDING 상태를 도입하여 이 문제를 해결했지만, 수정에서는 동일한 패턴이 적용되지 않았다.

**제안:** 최소한 R2 + DB 순서에서의 실패 케이스를 명시적으로 기술하고, 허용 가능한 불일치인지 판단을 문서에 남겨야 한다. 또는 수정 시에도 (1) 새 R2 키에 업로드 -> (2) DB UPDATE(R2 키 변경) -> (3) 이전 R2 파일 삭제 패턴을 적용하면 원자성에 가까워진다.

---

## Major (주요 개선사항)

### M-1. domain.md와의 중복: 값 객체(Enum), 도메인 규칙, R2 키 패턴

backend.md 곳곳에서 domain.md에 이미 정의된 내용을 반복한다:
- 섹션 1의 기술 스택에서 nanoid 8자리, slug 생성 방식 언급 (domain.md 섹션 3에서 상세 정의)
- 섹션 7.5 스케줄러에서 만료 규칙을 다시 서술 (domain.md DR-1, DR-6, DR-9 참조)
- 섹션 8.4 비로그인 문서 보안에서 "24시간 자동 만료"를 다시 명시 (domain.md DR-1)
- 섹션 7.7 상태 전이 다이어그램과 domain.md 섹션 5 도메인 이벤트가 동일 정보를 다른 형태로 제공

중복 자체가 나쁜 것은 아니지만, 향후 한쪽만 수정하면 불일치가 발생한다.

**제안:** backend.md에서는 "domain.md의 DR-N 규칙을 구현한다"는 참조 형태로 기술하고, 규칙 자체의 정의는 domain.md에 단일화한다. 상태 전이 다이어그램은 backend.md에 있는 것이 구현 맥락상 적절하므로 유지하되, "domain.md의 도메인 이벤트와 대응됨"이라는 주석을 추가한다.

### M-2. infrastructure.md와의 중복: 환경변수, 캐싱 전략, CORS

- 섹션 11(application.yml)의 환경변수 목록이 infrastructure.md 섹션 7의 환경변수 테이블과 대부분 겹친다.
- CORS 설정(섹션 2)에서 "개발 환경에서는 `http://localhost:3000` 추가 허용"이 infrastructure.md의 로컬 환경 설명과 중복된다.
- 캐싱 전략(섹션 7.2 Cache-Control 헤더)이 infrastructure.md 섹션 9의 캐싱 전략과 분산되어 있다.

**제안:** backend.md의 application.yml 섹션은 백엔드 고유 설정(JPA, JWT, OAuth2 등)만 포함하고, 인프라 관련 환경변수 전체 목록은 infrastructure.md로 위임한다. 캐싱 전략은 backend.md의 API 응답 헤더 정의로 통합하고, infrastructure.md에서는 Next.js 캐싱만 다룬다.

### M-3. `GET /api/v1/auth/me` 응답에 `storageUsage`를 포함하는 것이 적절한지

섹션 4에서 `/auth/me` 응답에 `storageUsage` (totalBytes, documentCount)를 포함한다. 인증 API에 스토리지 정보가 결합되면:
- 사용량이 변경될 때마다 `/auth/me` 응답 캐싱이 무효화된다.
- 인증과 스토리지라는 서로 다른 관심사가 하나의 엔드포인트에 결합된다.
- 프론트엔드 대시보드에서 사용량을 실시간으로 갱신하려면 별도 요청이 필요해질 수 있다.

**제안:** MVP에서는 편의상 유지하되, 향후 `GET /api/v1/users/me/storage` 같은 별도 엔드포인트 분리를 문서에 Phase 2 검토 사항으로 명시한다.

### M-4. `DELETE /api/v1/documents/{slug}` 응답이 204 No Content가 아닌 200 OK인 이유 미기재

섹션 3에서 삭제 응답으로 `200 OK`와 함께 `{ id, slug, deletedAt }`을 반환한다. REST 관례상 삭제는 `204 No Content`가 일반적이다. 현재 방식의 이유(프론트엔드에서 삭제 확인 용도 등)가 있다면 설계 결정으로 명시해야 한다.

### M-5. 로그아웃 API(`POST /api/v1/auth/logout`) 상세 명세 누락

섹션 3 엔드포인트 목록에 로그아웃이 포함되어 있지만, 섹션 4 인증 API 상세에서 요청/응답 형식이 기술되어 있지 않다. 다른 인증 API(callback, refresh, me)는 모두 상세가 있는데 logout만 빠져 있다.

**제안:** 로그아웃 API의 요청(리프레시 토큰 전달 방식), 응답(성공/실패), 처리 로직(해당 리프레시 토큰 revoke)을 추가한다.

### M-6. 문서 목록 조회 시 `expiresAt` 필드 누락

`GET /api/v1/documents` 응답의 documents 배열에 `expiresAt` 필드가 없다. Phase 2에서 Free 플랜 7일 만료가 도입되면 대시보드에서 만료 시점을 표시해야 할 가능성이 높다. MVP에서도 로그인 사용자의 문서가 영구인지 확인하는 용도로 유용하다.

---

## Minor (사소한 개선사항)

### m-1. 설계 결정 번호가 불연속적이다

섹션 13의 설계 결정이 "결정 1, 결정 2, 결정 4, 결정 5"로, 결정 3이 빠져 있다. infrastructure.md에 "결정 3: Railway 선택"이 있으므로 교차 참조를 위해 의도된 것으로 보이나, "결정 3은 infrastructure.md에 기록됨"이라는 주석을 추가하면 혼동을 방지할 수 있다.

### m-2. `DELETE` 응답의 `deletedAt` 필드 설명이 모호하다

"`deletedAt`은 soft delete 시점의 `updatedAt` 값이다. DB에 별도 `deleted_at` 컬럼은 두지 않는다."라는 설명에서, 프론트엔드 개발자가 `deletedAt`이 DB 컬럼이 아닌 서버가 계산한 값임을 즉시 이해하기 어렵다.

**제안:** "서버에서 `status = DELETED`로 변경한 시점의 타임스탬프를 `deletedAt`으로 반환한다"로 명확히 한다.

### m-3. `GET /api/v1/documents/{slug}` (편집용)과 `/view` (서빙용) 응답 필드 차이가 표로 정리되면 좋다

두 엔드포인트가 동일 리소스를 다른 용도로 반환하므로, 어떤 필드가 각각에 포함/제외되는지 비교표가 있으면 프론트엔드 개발 시 혼동이 줄어든다.

### m-4. `@Scheduled` CRON 표현식이 아닌 `fixedRate` 사용

섹션 7.5에서 `fixedRate = 3600000`(1시간)을 사용한다. 이는 애플리케이션 시작 시점부터 1시간 간격으로 실행되므로, 서버 재시작 시 실행 시점이 변동된다. 운영 예측성을 위해 CRON 표현식(`@Scheduled(cron = "0 0 * * * *")`)으로 매시 정각 실행을 고려할 수 있다.

### m-5. 문서 서빙 API의 캐시 헤더에 ETag 미포함

섹션 7.2에서 `Cache-Control` 헤더만 정의하고 `ETag`는 포함하지 않는다. 문서 수정 후 캐시 무효화를 위해 `ETag` (예: `updatedAt`의 해시)를 포함하면 클라이언트 캐싱 효율이 높아진다.

---

## 추가 고려사항

1. **문서 크기 검증 위치**: 섹션 8.1에서 `@Size(max=5MB)`를 사용한다고 하는데, `@Size`는 문자열 길이 제한이지 바이트 크기 제한이 아니다. UTF-8 멀티바이트 문자가 포함된 경우 5MB 바이트 제한과 `@Size` 문자 수 제한이 일치하지 않는다. `maxRequestSize`와 커스텀 validator를 병행해야 한다.

2. **만료 문서 서빙 시 실시간 체크 vs 스케줄러 의존**: 섹션 7.2에서 "expiresAt < now이면 410 반환하되, DB status 변경은 스케줄러에 위임"한다. 이는 올바른 접근이지만, 스케줄러가 장시간 실패하면 `status = ACTIVE`이면서 `expiresAt`이 지난 문서가 계속 쌓인다. 스케줄러 실패 시 알림(Sentry)을 모니터링 섹션에 명시하면 좋다.

3. **content 필드의 JSON 이스케이핑**: 섹션 3의 문서 상세/서빙 API에서 HTML/MD 콘텐츠를 JSON body의 `content` 필드로 반환한다. HTML에 `"`, `\`, 제어 문자가 포함될 경우 JSON 직렬화가 올바른지 프레임워크(Jackson) 기본 동작에 의존하지만, 5MB HTML의 JSON 직렬화 성능을 실제 테스트로 검증할 필요가 있다.

4. **`@Scheduled` 단일 인스턴스 전제**: 섹션 7.5에서 스케줄러가 단일 인스턴스 전제로 설계되었다. infrastructure.md의 확장성 계획에서 "스케일 시 ShedLock + DB 기반 분산 락"을 언급하지만, backend.md에서는 이 전환점이 누락되어 있다.

5. **frontend.md와의 API URL 일관성**: frontend.md 섹션 5에서 "Spring Boot `POST /api/v1/documents` 호출"이라고 명시하고 있어 backend.md의 엔드포인트와 일치한다. 다만 frontend.md에서 410 응답 시 "만료 안내 UI 인라인 렌더링"이라고 하고, backend.md에서는 `DOCUMENT_EXPIRED`와 `DOCUMENT_GONE` 두 가지 410 코드를 정의한다. 프론트엔드가 이 두 코드를 어떻게 구분하여 UI를 렌더링하는지 frontend.md에 반영이 필요하다.
