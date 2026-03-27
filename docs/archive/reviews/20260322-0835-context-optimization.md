# DR1 컨텍스트 최적화 가이드

> 작성일: 2026-03-22
> 목적: doc-reviewer(DR1) 에이전트의 문서 검토 작업 효율 최적화

---

## 요약

DraftURL 프로젝트의 문서는 4개 디렉토리에 10개 파일(총 4,094줄)로 구성되어 있다. 핵심 문서는 `mvp-service-design.md`(2,407줄)이며, 나머지 문서는 이 설계서의 상위 계획이거나 보조 자료이다. 검토 작업 시 문서 유형별 우선순위와 의존성 방향을 파악하면 불필요한 탐색을 60% 이상 줄일 수 있다.

---

## 1. 문서 구조 전체 맵

```
docs/
  benchmarking/
    tiiny-host-analysis.md          (90줄)  -- 경쟁사 분석
  plans/
    service-plan.md                 (501줄) -- 전체 서비스 계획서 (로드맵, 수익 모델)
    domain-analysis.md              (458줄) -- 목업 기반 도메인/엔티티 분석
    file-storage/
      file-upload-storage-plan.md   (504줄) -- 파일 업로드/저장 전략 (구버전, Next.js 풀스택 기준)
  design/
    mvp-service-design.md           (2407줄) -- MVP 상세 설계 (Spring Boot + Next.js, 확정본)
    mockups/
      index.html                            -- 에디터/랜딩 페이지 목업
      shared.html                           -- 공유 완료 페이지 목업
      viewer.html                           -- 문서 뷰어 목업
      dashboard.html                        -- 대시보드 목업
      login.html                            -- 로그인 페이지 목업
  reviews/
    service-plan-review.md          (134줄) -- service-plan.md 검토 결과 (전건 반영 완료)
```

---

## 2. 문서별 역할 정의

| 문서 | 유형 | 역할 | 상태 |
|------|------|------|------|
| `mvp-service-design.md` | 설계 명세 | **프로젝트의 단일 진실 원천(SSOT)**. API 스펙, DB 스키마, 시퀀스 다이어그램, 보안 정책, 에러 코드 모두 포함 | 2차 검토 완료, 구현 단계 |
| `service-plan.md` | 상위 계획서 | 서비스 비전, 로드맵, 수익 모델, 경쟁 분석. 기술 스택은 설계서 반영 완료 | 검토 반영 완료 |
| `domain-analysis.md` | 분석 문서 | 목업 HTML에서 추출한 엔티티/속성/액션 매핑. API 엔드포인트 초안 포함 | 완료 (API 경로는 설계서 기준으로 갱신 필요) |
| `file-upload-storage-plan.md` | 계획 문서 | R2 파일 저장 전략. **주의: Next.js 풀스택 기준으로 작성됨** (아키텍처 변경 전) | 구버전 -- 설계서가 대체 |
| `tiiny-host-analysis.md` | 벤치마킹 | 경쟁사(tiiny.host) 기능/가격/포지셔닝 분석 | 완료, 참조용 |
| `service-plan-review.md` | 검토 결과 | service-plan.md 1차 검토 결과. 전건 반영 완료 | 아카이브 |
| `mockups/*.html` | UI 목업 | 5개 주요 페이지의 정적 HTML 목업 | 완료, domain-analysis의 근거 |

---

## 3. 문서 간 의존성 그래프

```
[의존 방향: 화살표는 "참조한다/근거로 삼는다" 방향]

tiiny-host-analysis.md
        |
        v
service-plan.md  <-------- service-plan-review.md
        |
        v
mvp-service-design.md  <-- file-upload-storage-plan.md (구버전, 부분 흡수됨)
        ^
        |
domain-analysis.md  <----- mockups/*.html
```

**핵심 의존성 요약:**
- `mvp-service-design.md`는 `service-plan.md`와 `file-upload-storage-plan.md`를 참조하여 작성됨
- `service-plan.md`는 `tiiny-host-analysis.md`를 벤치마킹 근거로 사용
- `domain-analysis.md`는 `mockups/*.html` 5개 파일에서 엔티티와 액션을 추출
- `service-plan-review.md`는 `service-plan.md`와 `mvp-service-design.md`를 교차 검증

---

## 4. 검토 작업별 최적 참조 경로

### 4.1 새 설계 문서 검토 시

```
1순위: mvp-service-design.md     -- 기존 확정 스펙과의 정합성 확인
2순위: service-plan.md           -- 상위 비전/로드맵과의 일관성 확인
3순위: domain-analysis.md        -- 엔티티/API 명세 교차 검증
불필요: tiiny-host-analysis.md, file-upload-storage-plan.md
```

### 4.2 계획서 업데이트 검토 시

```
1순위: mvp-service-design.md     -- 기술 스택/범위가 설계서와 일치하는지 확인
2순위: service-plan-review.md    -- 기존 검토 항목 재발 여부 확인
불필요: mockups/*.html, tiiny-host-analysis.md
```

### 4.3 구현 코드 리뷰 시 (문서 기준 확인)

```
1순위: mvp-service-design.md     -- API 스펙, DB 스키마, 에러 코드 참조
2순위: domain-analysis.md        -- UI 액션-API 매핑 확인
불필요: plans/ 디렉토리 전체
```

---

## 5. 컨텍스트 최적화 권장 사항

### 5.1 즉시 적용 가능

| # | 최적화 | 효과 |
|---|--------|------|
| O-1 | **`file-upload-storage-plan.md`에 deprecated 표시** | 구버전 문서와 현행 설계서 혼동 방지. 이 문서는 Next.js 풀스택 기준이며 `mvp-service-design.md`가 완전히 대체함 |
| O-2 | **`domain-analysis.md`의 API 경로를 설계서 기준으로 갱신** | 현재 `/api/documents`(도메인 분석)와 `/api/v1/documents`(설계서) 불일치. 검토 시 혼란 유발 |
| O-3 | **검토 결과를 reviews/ 디렉토리에 통합** | 현재 `mvp-service-design.md` 내부(1955줄~)에 자체 검토 섹션이 포함됨. 분리하면 설계서 본문만 2,400줄 -> ~1,950줄로 축소 |

### 5.2 문서 관리 체계

| # | 최적화 | 효과 |
|---|--------|------|
| O-4 | **각 문서 헤더에 `상태` 필드 추가** (draft / active / deprecated / archived) | DR1이 문서 읽기 전에 현행성 판단 가능 |
| O-5 | **`docs/README.md` 또는 인덱스 문서 생성** | 문서 전체 맵을 한 곳에서 파악. 이 문서(`context-optimization.md`)가 그 역할을 임시로 수행 |
| O-6 | **교차 참조 시 절대 경로 통일** | 일부 문서는 상대 경로(`../design/...`), 일부는 프로젝트 루트 기준(`docs/plans/...`) 사용. 하나로 통일 |

### 5.3 DR1 에이전트 워크플로우 최적화

**문서 검토 요청 수신 시 DR1이 따라야 할 순서:**

```
1. 이 파일(context-optimization.md)의 섹션 4에서 작업 유형별 참조 경로 확인
2. 대상 문서의 헤더에서 참조 문서 목록 확인
3. mvp-service-design.md는 2,407줄이므로 전체 로드 대신 관련 섹션만 선택적 읽기:
   - API 스펙 확인: 섹션 4-5 (약 200-600줄)
   - DB 스키마 확인: 섹션 7 (약 700-900줄)
   - 보안 확인: 섹션 6, 11 (약 600-700줄, 1400-1600줄)
   - 에러 코드 확인: 섹션 9 (약 1000-1200줄)
   - 자체 검토 결과: 섹션 14 이후 (약 1955줄~)
4. agent-memory의 project_mvp_design_review.md에서 이전 검토 이력 확인
5. 검토 결과는 docs/reviews/ 디렉토리에 저장
```

---

## 6. 알려진 문서 간 불일치 (2026-03-22 기준)

| 항목 | 문서 A | 문서 B | 불일치 내용 |
|------|--------|--------|------------|
| API 경로 prefix | domain-analysis.md (`/api/documents`) | mvp-service-design.md (`/api/v1/documents`) | 버전 prefix 유무 |
| 아키텍처 | file-upload-storage-plan.md (Next.js 풀스택) | mvp-service-design.md (Spring Boot 분리) | 전면 변경됨 |
| 에디터 | file-upload-storage-plan.md (Monaco Editor) | mvp-service-design.md (textarea 기반 에디터) | 설계서에서 단순화 가능성 |
