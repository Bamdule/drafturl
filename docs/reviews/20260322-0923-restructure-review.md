# 문서 구조 재편 검토 결과

> 검토일: 2026-03-22
> 검토 대상: product.md, market.md, domain.md, backend.md, frontend.md, infrastructure.md
> 검토 기준: 중복, 누락, 불필요 내용, 교차 참조, 일관성

---

## 요약

문서 구조 재편은 전반적으로 잘 수행되었다. 6개 문서의 역할 분담이 명확하고, 의존성 방향(`market -> product -> architecture/*`)이 깔끔하다. 다만 **중복 5건, 누락 2건, 불필요 내용 3건, 깨진 링크 1건, 일관성 충돌 2건**이 식별되었다. 아래에 심각도별로 정리한다.

---

## 1. 중복 확인

### D-1. R2 버킷 구조/접근 방식 (domain.md + backend.md)

- **domain.md 섹션 9**: R2 버킷 구조, 키 네이밍 규칙, R2 접근 방식(퍼블릭 버킷 불가 이유, S3Client 사용), `application.yml` R2 접속 설정까지 상세 기술
- **backend.md 섹션 11**: `application.yml`에 동일한 R2 설정 포함 (`r2.endpoint`, `r2.bucket-name` 등)
- **backend.md 섹션 10**: `R2Config.java`, `R2StorageService.java` 등 R2 관련 구현 참조

**권장 조치**: domain.md의 R2 관련 내용은 **데이터 모델(키 네이밍 규칙)만 남기고**, 접근 방식/설정/SDK 선택은 backend.md로 이관한다. domain.md는 "무엇을 저장하는가"에 집중하고, "어떻게 접근하는가"는 backend.md의 영역이다.

### D-2. 기술 스택 반복 (backend.md + infrastructure.md)

- **backend.md 섹션 1**: PostgreSQL(Supabase), Cloudflare R2(AWS SDK), Spring Boot 3.4.x 등 기술 스택 나열
- **infrastructure.md 섹션 1**: Supabase PostgreSQL, Cloudflare R2, Railway 등 인프라 기술 스택 나열

두 문서 모두 DB, 스토리지, 호스팅 선택을 기술하지만 관점이 다르다(backend: 개발 관점, infra: 운영 관점). 현재 수준에서는 허용 가능한 중복이나, 기술 선택 이유가 양쪽에 분산되어 있어 변경 시 두 곳을 수정해야 한다.

**권장 조치**: infrastructure.md를 인프라 선택의 원천(Source of Truth)으로 삼고, backend.md 섹션 1에서는 "인프라 선택은 [인프라 & 배포](infrastructure.md)를 참조"로 링크하여 기술 스택 테이블의 인프라 항목(DB, R2, 호스팅)을 제거한다.

### D-3. sandbox iframe 정책 (backend.md 내부 중복)

- **backend.md 섹션 7.2** (HTML 서빙 래퍼): sandbox 설명 + 트레이드오프
- **backend.md 섹션 8.2** (HTML 서빙 보안): 동일한 sandbox 정책 재설명
- **backend.md 섹션 8.4** (사용자 HTML 처리 정책): 세 번째로 동일 내용 기술

**권장 조치**: 섹션 8.4를 정책의 원천으로 삼고, 섹션 7.2는 구현 흐름에 필요한 최소 언급 + "상세 정책은 섹션 8.4 참조"로 축소. 섹션 8.2는 표에서 섹션 8.4로 링크만 남긴다.

### D-4. 만료 정책 (product.md + domain.md)

- **product.md 섹션 5**: 비로그인 24h, Free 7일, Starter+ 영구
- **domain.md 섹션 6**: DR-1(비로그인 24h), DR-2(로그인 영구)

product.md에는 Free 7일 만료가 있으나 domain.md에는 없다. product.md에서 "Free 플랜 만료 정책은 Phase 2에서 구현"이라고 명시하고 있어 현재는 의도된 차이이다. 그러나 이 관계가 불명확하다.

**권장 조치**: domain.md DR-2에 주석을 추가하여 "Phase 2에서 Free 플랜 7일 만료로 세분화 예정. [product.md 섹션 5](../product.md) 참조"라고 명시한다.

### D-5. 설계 결정 기록 위치 (infrastructure.md)

- **infrastructure.md 섹션 12**: "설계 결정 및 트레이드오프 기록"이 6건 수록되어 있다. 이 중 결정 1(FE/BE 분리), 결정 2(JWT 인증), 결정 4(문서 타입 변경 불허), 결정 5(sandbox), 결정 6(id=slug)은 인프라가 아닌 아키텍처/백엔드 설계 결정이다.

**권장 조치**: 인프라 관련 결정(결정 3: Railway 선택)만 infrastructure.md에 남기고, 나머지는 해당 문서로 이관한다. 또는 별도 `decisions.md` (ADR) 문서를 만들어 일괄 관리한다.

---

## 2. 누락 확인

### M-1. CI/CD 파이프라인 상세 부재

infrastructure.md 섹션 4에서 GitHub Actions 워크플로우 파일명(`frontend-deploy.yml`, `backend-deploy.yml`)을 언급하지만, 워크플로우의 트리거 조건(push/PR, 브랜치), 테스트 단계, 빌드 단계, 배포 단계의 상세가 없다. README.md 의존성 다이어그램에서도 CI/CD는 infrastructure.md에 포함된다고 명시되어 있다.

**권장 조치**: infrastructure.md에 CI/CD 워크플로우의 트리거 조건, 주요 단계(lint, test, build, deploy), 환경별 분기(staging/production) 최소한의 정의를 추가한다.

### M-2. 에러 처리/로깅 전략 부재

backend.md에 에러 코드 총괄(섹션 5)은 있으나, 로깅 전략(로그 레벨, 구조화 로깅, Sentry 연동 방식)이 없다. infrastructure.md에서 Sentry를 모니터링 도구로 명시했지만 연동 방식은 기술되지 않았다.

**권장 조치**: backend.md 또는 infrastructure.md에 로깅/모니터링 연동에 대한 최소한의 지침을 추가한다. MVP 범위에서는 간략하게라도 로그 레벨 정책과 Sentry SDK 연동 위치를 명시하면 구현 시 혼란을 줄일 수 있다.

---

## 3. 불필요한 내용

### U-1. infrastructure.md 섹션 10: 구현 순서 권장사항

40단계에 걸친 상세 구현 순서가 인프라 문서에 포함되어 있다. 이 내용은 프로젝트 관리/태스크 계획의 영역이며, 인프라 설계 문서의 목적에 맞지 않는다. product.md 섹션 4에도 Phase별 주차 계획이 별도로 존재하여 역할이 겹친다.

**권장 조치**: infrastructure.md에서 섹션 10을 제거한다. 구현 순서가 필요하면 별도의 `task-plan.md`나 이슈 트래커에서 관리한다.

### U-2. infrastructure.md 섹션 11: 주의사항

"Monaco Editor SSR: false", "미리보기 debounce 300ms", "파일 읽기 인코딩" 등 프론트엔드 구현 주의사항이 인프라 문서에 포함되어 있다. 이들은 frontend.md나 backend.md에서 다루어야 할 내용이다.

**권장 조치**: 프론트엔드 관련 주의사항(Monaco, debounce, FileReader)은 frontend.md로, 백엔드 관련(CORS, JWT, nanoid, R2 정합성, HTML 새니타이징)은 backend.md로 이관하고, 인프라 관련(Spring Boot 콜드 스타트, API 버전)만 infrastructure.md에 남긴다.

### U-3. backend.md 섹션 12: 핵심 서비스 로직 요약

섹션 7(핵심 로직 설계)에서 시퀀스 다이어그램과 함께 상세히 기술한 내용을 섹션 12에서 텍스트로 다시 요약하고 있다. 동일 문서 내에서의 불필요한 반복이다.

**권장 조치**: 섹션 12를 제거하거나, 섹션 7의 다이어그램을 참조하는 간단한 인덱스로 축소한다.

---

## 4. 교차 참조 (링크 검증)

### L-1. frontend.md 관련 문서: 깨진 링크

- **frontend.md 하단**: `[UI 목업](../mockups/) -- HTML 목업 5개 페이지`
  - `docs/mockups/` 디렉토리와 5개 HTML 파일이 실제 존재하므로 경로 자체는 유효하다. 단, 디렉토리 링크(`../mockups/`)는 Markdown 뷰어에 따라 동작이 불안정할 수 있다.
  - **권장**: `../mockups/index.html` 등 구체적 파일을 링크하거나, mockups에 대한 README를 생성하여 링크한다.

### L-2. 교차 참조 정합성 검증 결과

| 출발 문서 | 링크 대상 | 상태 |
|-----------|----------|------|
| product.md -> market.md | `market.md` | OK |
| product.md -> architecture/domain.md | `architecture/domain.md` | OK |
| product.md -> architecture/backend.md | `architecture/backend.md` | OK |
| product.md -> architecture/frontend.md | `architecture/frontend.md` | OK |
| product.md -> architecture/infrastructure.md | `architecture/infrastructure.md` | OK |
| market.md -> product.md | `product.md` | OK |
| market.md -> architecture/backend.md | `architecture/backend.md` | OK |
| domain.md -> backend.md | `backend.md` | OK |
| domain.md -> ../product.md | `../product.md` | OK |
| backend.md -> domain.md | `domain.md` | OK |
| backend.md -> frontend.md | `frontend.md` | OK |
| backend.md -> infrastructure.md | `infrastructure.md` | OK |
| backend.md -> ../product.md | `../product.md` | OK |
| frontend.md -> backend.md | `backend.md` | OK |
| frontend.md -> infrastructure.md | `infrastructure.md` | OK |
| frontend.md -> ../mockups/ | `../mockups/` | 주의 (L-1) |
| infrastructure.md -> backend.md | `backend.md` | OK |
| infrastructure.md -> frontend.md | `frontend.md` | OK |
| infrastructure.md -> domain.md | `domain.md` | OK |

**결론**: 교차 참조는 전반적으로 정확하다. L-1의 디렉토리 링크만 개선이 필요하다.

---

## 5. 일관성 (수치/정책/스펙 충돌)

### C-1. 만료 정책: DR-2 vs Free 플랜 7일 (product.md vs domain.md)

- **domain.md DR-2**: "로그인 사용자의 문서는 `expiresAt`이 null이다 (영구)"
- **product.md 섹션 5**: "Free 플랜 = 7일 만료"

product.md에서 "Free 플랜 만료 정책은 Phase 2에서 구현 (MVP에서는 DR-2 그대로 로그인 사용자 영구)"라고 부연하고 있어 의도된 차이이다. 그러나 domain.md에서는 Phase 2 예정 사항에 대한 언급이 전혀 없어, domain.md만 읽는 개발자는 Free 플랜 7일 만료를 인지하지 못할 수 있다.

**권장 조치**: domain.md DR-2에 "(Phase 2에서 Free 플랜 7일 만료로 변경 예정, product.md 참조)" 주석을 추가한다.

### C-2. PlanType enum: Team 플랜 누락 (domain.md vs product.md)

- **domain.md 섹션 3**: `enum PlanType { FREE, STARTER, PRO }` (3개)
- **product.md 섹션 6**: Free, Starter, Pro, **Team** 4개 플랜 정의

Team 플랜이 Phase 3 기능이므로 MVP 도메인 모델에서 의도적으로 제외한 것으로 보이나, 명시적 언급이 없다.

**권장 조치**: domain.md의 PlanType enum 아래에 "Team 플랜은 Phase 3에서 추가 예정"이라는 주석을 넣는다.

---

## 잘된 점

1. **문서 역할 분담이 명확하다**: product(무엇을), market(왜), domain(데이터 모델), backend(구현), frontend(UI), infrastructure(운영) -- 각 문서의 경계가 뚜렷하다.
2. **README.md 인덱스가 의존성 방향까지 명시한다**: 문서 간 관계를 한눈에 파악할 수 있다.
3. **교차 참조가 거의 정확하다**: 19개 링크 중 깨진 것은 0개, 주의가 필요한 것 1개뿐이다.
4. **각 문서의 "관련 문서" 섹션이 일관되게 존재한다**: 어느 문서에서 시작하든 관련 문서로 이동할 수 있다.
5. **Phase 구분이 명확하다**: MVP 범위와 이후 단계의 경계가 product.md에 잘 정리되어 있다.

---

## 조치 우선순위 요약

| 심각도 | ID | 내용 | 조치 |
|--------|-----|------|------|
| Major | D-1 | R2 접근 방식 중복 (domain/backend) | domain.md에서 접근 방식/설정 제거, backend.md로 이관 |
| Major | D-5 | 설계 결정이 infrastructure.md에 몰려있음 | 해당 문서로 분산 이관 |
| Major | U-1 | 구현 순서가 인프라 문서에 포함 | infrastructure.md에서 제거 |
| Major | U-2 | FE/BE 주의사항이 인프라 문서에 포함 | 해당 문서로 이관 |
| Minor | D-2 | 기술 스택 반복 (backend/infra) | backend.md에서 인프라 항목을 링크로 대체 |
| Minor | D-3 | sandbox 정책 3회 반복 (backend 내부) | 원천 하나로 통합 |
| Minor | D-4 | 만료 정책 차이 불명확 | domain.md에 Phase 2 주석 추가 |
| Minor | U-3 | 서비스 로직 요약 중복 (backend 내부) | 섹션 12 제거 또는 축소 |
| Minor | C-1 | DR-2 vs Free 7일 만료 관계 불명확 | domain.md에 주석 추가 |
| Minor | C-2 | PlanType에 Team 누락 | domain.md에 주석 추가 |
| Minor | M-1 | CI/CD 상세 부재 | infrastructure.md에 워크플로우 정의 추가 |
| Minor | M-2 | 로깅/모니터링 전략 부재 | backend.md 또는 infrastructure.md에 추가 |
| Minor | L-1 | 목업 디렉토리 링크 | 구체적 파일 링크로 변경 |
