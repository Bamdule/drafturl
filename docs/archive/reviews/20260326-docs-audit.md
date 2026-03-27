# docs/ 전체 문서 검토 결과

> 검토일: 2026-03-26
> 기준: v0.1.0 MVP 배포 완료 (Galaxy Book 개발서버, CDN 직접 서빙, R2 전환 완료)

---

## 요약

총 **48개 파일** 검토. 핵심 설계 문서(design/)는 전반적으로 최신 상태이나, **개발 환경 문서의 MinIO 참조**, **QA 문서의 outdated 인증 방식**, **완료된 태스크/리뷰 문서 18건**이 정리 대상이다. 문서 조회 방식이 CDN 직접 서빙으로 변경되었으나 일부 설계 문서에 반영이 미흡하다.

| 상태 | 개수 | 설명 |
|------|------|------|
| 최신 | 20 | 현행 기준에 맞음 |
| 부분 outdated | 12 | 일부 내용이 현재 코드/인프라와 불일치 |
| 삭제/아카이브 후보 | 16 | 완료된 태스크, 반영된 리뷰, 이력 참고용 |

---

## 상세 검토 결과

### 1. docs/README.md

| 항목 | 내용 |
|------|------|
| 목적 | 문서 인덱스, 구조 안내, 핵심 문서 링크, 상태 정의 |
| 상태 | **부분 outdated** |
| 문제점 | tasks/, reviews/, qa/, plans/, releases/ 디렉토리가 구조도에 누락. mockups/만 표시됨. 실제 docs 구조와 불일치 |
| 조치 | 구조도에 plans/, tasks/, qa/, releases/ 추가 필요 |

---

### 2. design/backend/ (4개 파일)

#### design/backend/README.md
| 항목 | 내용 |
|------|------|
| 목적 | 백엔드 기술 스택, 패키지 구조, 설정, 로깅, 설계 결정 |
| 상태 | **최신** |
| 비고 | R2 참조 정확. Port-Adapter 패턴 설명 현행과 일치. "초기 스캐폴딩 단계"라는 참고 문구는 이제 삭제해도 됨 (구현 완료) |

#### design/backend/api.md
| 항목 | 내용 |
|------|------|
| 목적 | API 공통 규격, 12개 엔드포인트 상세 명세, 에러 코드 |
| 상태 | **부분 outdated** |
| 문제점 | (1) `GET /api/v1/documents/{slug}/view` 응답에 `content` 필드로 설명되어 있으나, 실제로는 `contentUrl`로 CDN URL을 반환하는 방식으로 변경됨. (2) 이메일 회원가입 API가 구현되었으나 명세에 없음 (`POST /api/v1/auth/signup`, `POST /api/v1/auth/login`) |

#### design/backend/auth.md
| 항목 | 내용 |
|------|------|
| 목적 | OAuth2 + JWT 인증 플로우, Refresh Token Rotation, Security 필터 체인 |
| 상태 | **부분 outdated** |
| 문제점 | 이메일 회원가입/로그인이 실제로 구현되었으나 이 문서에는 OAuth2만 기술. BFF 프록시 패턴(Next.js API Route 경유)이 릴리즈 문서에는 언급되나 여기에는 미반영 |

#### design/backend/logic.md
| 항목 | 내용 |
|------|------|
| 목적 | 문서 생성 2단계 커밋, 서빙, 수정, 삭제, 스케줄러, 상태 전이 |
| 상태 | **부분 outdated** |
| 문제점 | 문서 서빙 흐름도에서 "R2에서 파일 로드" -> "content + metadata 반환"으로 설명. 실제로는 메타데이터 + contentUrl만 반환하고, 프론트엔드가 CDN에서 직접 로드하는 방식으로 변경됨 |

#### design/backend/security.md
| 항목 | 내용 |
|------|------|
| 목적 | 입력 검증, HTML 서빙 보안, 인증 보안, HTML 처리 정책 |
| 상태 | **부분 outdated** |
| 문제점 | sandbox 정책이 `allow-scripts`만으로 설명되어 있으나, 실제 코드에서는 `allow-same-origin allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox`로 구현됨. 설계와 구현이 괴리 |

---

### 3. design/domain.md

| 항목 | 내용 |
|------|------|
| 목적 | 4개 핵심 엔티티, ERD, DDL, 도메인 규칙, R2 버킷 구조 |
| 상태 | **최신** |
| 비고 | R2 참조 정확. DDL은 Flyway 마이그레이션과 일치. 도메인 규칙 10개 현행 유효 |

---

### 4. design/frontend/ (3개 파일)

#### design/frontend/README.md
| 항목 | 내용 |
|------|------|
| 목적 | 기술 스택, 역할 정의, 디렉토리 구조, 구현 주의사항 |
| 상태 | **부분 outdated** |
| 문제점 | (1) "Next.js 15"로 기술되어 있으나 릴리즈 문서에는 "Next.js 16"으로 명시. (2) "직접 R2 접근 X"라고 되어있지만, CDN 서빙 방식에서 프론트엔드가 files.drafturl.com에서 직접 파일을 로드함 (백엔드 API를 거치지 않음). 역할 정의 수정 필요 |

#### design/frontend/pages.md
| 항목 | 내용 |
|------|------|
| 목적 | App Router 페이지/라우트 구조, 주요 페이지 상세 |
| 상태 | **부분 outdated** |
| 문제점 | (1) `app/auth/signup/` 라우트가 실제 존재하나 문서에 없음. (2) OAuth2 콜백 페이지 설명에서 "Spring Boot GET /api/v1/auth/oauth2/state" 호출이 기술되어 있으나 실제 state 생성 방식 확인 필요 |

#### design/frontend/client.md
| 항목 | 내용 |
|------|------|
| 목적 | API 클라이언트, 에러 처리, 토큰 관리, Zustand Store |
| 상태 | **최신** |
| 비고 | httpOnly 쿠키 + BFF 프록시 패턴은 릴리즈 문서에서 확인된 실제 구현과 일치 |

---

### 5. design/infrastructure/ (4개 파일)

#### design/infrastructure/README.md
| 항목 | 내용 |
|------|------|
| 목적 | 인프라 기술 스택, 아키텍처 다이어그램, 확장성 계획 |
| 상태 | **부분 outdated** |
| 문제점 | Railway/Vercel/Supabase 기반 아키텍처로 설명되어 있으나, 실제 배포는 Galaxy Book 개발서버 + Docker Compose + Cloudflare Tunnel. 아키텍처 다이어그램이 현재 환경과 불일치. 이 문서의 내용은 "향후 운영서버 전환 시" 계획으로 리포지셔닝해야 함 |

#### design/infrastructure/deployment.md
| 항목 | 내용 |
|------|------|
| 목적 | CI/CD 워크플로우, Dockerfile, 환경변수 관리, 호스팅 비교 |
| 상태 | **부분 outdated** |
| 문제점 | Railway/Vercel 기반 배포 설명. 실제로는 Self-hosted Runner + Docker Compose 배포. dev-server-plan.md의 내용이 현행 |

#### design/infrastructure/dev-environment.md
| 항목 | 내용 |
|------|------|
| 목적 | Docker Compose 로컬 개발 환경, Cloudflare Tunnel |
| 상태 | **outdated** |
| 문제점 | **MinIO를 R2 대체로 사용한다고 기술**되어 있으나, 커밋 `4b19d7e`에서 MinIO -> R2로 전환 완료됨. docker-compose 파일에 MinIO 없음 확인. 다이어그램, docker-compose 구성 표, 로컬 환경변수 모두 MinIO 참조를 R2로 갱신 필요 |

#### design/infrastructure/operations.md
| 항목 | 내용 |
|------|------|
| 목적 | 비용 추산, 성능 목표, 캐싱 전략 |
| 상태 | **부분 outdated** |
| 문제점 | Railway $5/월 기준 비용 추산. 현재는 Galaxy Book 로컬 서버(비용 $0)로 운영. 현 환경 기준 비용은 도메인비 $10/년 + R2 무료 정도 |

---

### 6. plans/ (6개 파일)

#### plans/market/market.md
| 항목 | 내용 |
|------|------|
| 목적 | tiiny.host 경쟁사 분석, 핵심 해자, 방어 전략, 실행 전략 |
| 상태 | **최신** |
| 비고 | 전략/방향성 문서로서 유효. Phase 관련 내용도 현행과 일치 |

#### plans/product/product.md
| 항목 | 내용 |
|------|------|
| 목적 | 서비스 비전, 기능 목록, 사용자 플로우, 로드맵, 수익 모델 |
| 상태 | **최신** |
| 비고 | Phase 1 MVP 범위, 만료 정책, 가격 체계 모두 유효. 주기적 업데이트 대상 |

#### plans/product/branding.md
| 항목 | 내용 |
|------|------|
| 목적 | 브랜드 정체성, 메시징, 비주얼 가이드라인, 마이크로카피 |
| 상태 | **최신** |
| 비고 | 디자인 토큰, 카피, 페르소나 모두 현행 유효 |

#### plans/product/backlog.md
| 항목 | 내용 |
|------|------|
| 목적 | 버전 범위 밖 작업 후보 목록 |
| 상태 | **최신** |
| 비고 | 2026-03-27 최종 수정. v0.3.0 후보 항목 적절 |

#### plans/product/v0.2.0-tasks.md
| 항목 | 내용 |
|------|------|
| 목적 | v0.2.0 작업 목록 (CI/CD, 사용성, SEO, 모바일) |
| 상태 | **최신** |
| 비고 | 확정 상태, 현재 진행 예정 작업 |

#### plans/infra/dev-server-plan.md
| 항목 | 내용 |
|------|------|
| 목적 | Galaxy Book 개발서버 구축 계획 (OS, Docker, Tunnel, 배포, R2, 운영서버 전략) |
| 상태 | **부분 outdated** |
| 문제점 | (1) 환경변수 관리 섹션에서 `R2_ACCESS_KEY`/`R2_ENDPOINT`가 "dev=MinIO, prod=R2"로 설명 (6-1절). 실제로는 모든 환경에서 R2 사용. (2) docker-compose 구조에 MinIO 언급 (4절). (3) 마이그레이션 체크리스트의 "6개 컨테이너" 표현은 MinIO 포함 수치 (5개로 수정 필요). (4) 미결사항의 "MinIO 데이터" 참조 (10절). 한편 5절의 R2 직접 사용 결정과 CDN 구성은 현행과 일치하여 정확함 |

#### plans/infra/release-guide.md
| 항목 | 내용 |
|------|------|
| 목적 | 릴리즈 관리 가이드 (버전 체계, 브랜치 전략, 태그, 릴리즈 문서) |
| 상태 | **최신** |
| 비고 | 2026-03-27 작성, 현행 적용 중 |

---

### 7. qa/ (8개 파일)

#### qa/README.md
| 항목 | 내용 |
|------|------|
| 목적 | QA 체크리스트 요약, 실행 환경, 실행 방법 |
| 상태 | **부분 outdated** |
| 문제점 | 실행 환경에 "MinIO 콘솔: http://localhost:9001" 기재. MinIO 제거됨 |

#### qa/auth.md
| 항목 | 내용 |
|------|------|
| 목적 | 인증 관련 12개 TC |
| 상태 | **outdated** |
| 문제점 | **이메일 회원가입/로그인 TC(TC-AUTH-01~07)가 존재**. 원래 설계(OAuth2 only)와도 다르고, 실제 구현에도 이메일 가입이 있다면 최신이지만, 설계 문서와의 정합성이 없음. OAuth2 TC(TC-AUTH-12)는 현재 "이메일 로그인만 구현된 경우 SKIP"이라고 메모. **설계 문서와 QA 문서의 인증 방식 정합성을 맞춰야 함** |

#### qa/document-create.md, document-manage.md, document-view.md, editor.md
| 항목 | 내용 |
|------|------|
| 상태 | **최신** |
| 비고 | TC 내용이 실제 구현과 일치. document-view.md의 TC-VIEW-07에서 `sandbox="allow-same-origin allow-scripts"`로 기대 결과 명시 -- 코드와 일치 |

#### qa/infra.md
| 항목 | 내용 |
|------|------|
| 상태 | **부분 outdated** |
| 문제점 | TC-INFRA-07에서 Docker 컨테이너로 `drafturl-storage (MinIO): healthy` 기대. MinIO 제거됨 |

#### qa/results/ (2개 파일)
| 항목 | 내용 |
|------|------|
| 목적 | 2026-03-25 QA 실행 결과 (로컬 + 개발서버) |
| 상태 | **이력 참고용 -- 보관** |
| 비고 | MVP 배포 전 QA 이력. 삭제 불필요하나 최신 결과는 아님 |

---

### 8. releases/v0.1.0-mvp.md

| 항목 | 내용 |
|------|------|
| 목적 | v0.1.0 MVP 릴리즈 노트 |
| 상태 | **최신** |
| 비고 | 배포일 2026-03-27, 기술 스택/아키텍처 정확 (BFF 프록시, CDN 직접 서빙, R2) |

---

### 9. tasks/ (3개 파일)

#### tasks/backend.md
| 항목 | 내용 |
|------|------|
| 목적 | 백엔드 11개 태스크 체크리스트 |
| 상태 | **완료됨 -- 아카이브 후보** |
| 비고 | 11개 태스크 전부 완료(상태 표). 하위 세부 항목은 아직 `[ ]`로 표기되어 있으나 상단 상태는 모두 완료. v0.1.0 이력으로만 의미 있음 |

#### tasks/frontend.md
| 항목 | 내용 |
|------|------|
| 목적 | 프론트엔드 16개 태스크 체크리스트 |
| 상태 | **거의 완료 -- 아카이브 후보** |
| 비고 | 13개 완료, 14/15/16은 미완성 표시이나 이미 MVP 배포됨. 잔여 항목(반응형, SEO)은 v0.2.0-tasks.md로 이관됨 |

#### tasks/improvements.md
| 항목 | 내용 |
|------|------|
| 목적 | MVP 전체 개선점 분석 (UX, 성능, 보안, 코드 품질, 인프라, 비즈니스) |
| 상태 | **유효하지만 리포지셔닝 필요** |
| 비고 | P0 항목 중 일부(S-1 httpOnly 전환, I-2 프로덕션 배포)는 이미 해결됨 (BFF 프록시 구현, Galaxy Book 배포). 해결된 항목 표시 갱신 필요. 잔여 항목은 backlog.md나 v0.2.0-tasks.md와 중복 가능 |

---

### 10. reviews/ (18개 파일)

| 파일 | 목적 | 상태 |
|------|------|------|
| 20260322-0835-context-optimization.md | 컨텍스트 최적화 리뷰 | 아카이브 후보 |
| 20260322-0920-mvp-service-design-review.md | MVP 서비스 설계 리뷰 | 아카이브 후보 |
| 20260322-0923-restructure-review.md | 문서 구조 개편 리뷰 | 아카이브 후보 |
| 20260322-0948-product-review.md | product.md 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-0953-backend-review.md | 백엔드 설계 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-1000-frontend-review.md | 프론트엔드 설계 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-1000-infrastructure-review.md | 인프라 설계 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-1001-domain-review.md | 도메인 모델 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-1059-service-plan-review.md | 서비스 플랜 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-1103-split-review.md | 문서 분리 리뷰 | 아카이브 후보 (반영 완료) |
| 20260322-1235-backend-code-review.md | 백엔드 코드 리뷰 | 아카이브 후보 (수정 반영됨) |
| 20260322-1304-backend-structure-review.md | 백엔드 구조 리뷰 | 아카이브 후보 |
| 20260323-C3-storage-usage-negative-prevention-review.md | StorageUsage 음수 방지 수정 리뷰 | 아카이브 후보 (수정 완료) |
| 20260323-C4-jsoup-safelist-fix-review.md | Jsoup Safelist 수정 리뷰 | 아카이브 후보 (수정 완료) |
| 20260323-M2-M8-batch-optimization-review.md | 배치 최적화 리뷰 | 아카이브 후보 (수정 완료) |
| 20260323-c2-scheduler-transactional-review.md | 스케줄러 트랜잭션 리뷰 | 아카이브 후보 (수정 완료) |
| 20260323-frontend-code-review.md | 프론트엔드 코드 리뷰 | 아카이브 후보 (수정 완료) |
| 20260323-oauth-state-hmac-self-review.md | OAuth state HMAC 리뷰 | 아카이브 후보 (수정 완료) |

**18개 전부 아카이브 후보**. 설계 리뷰는 해당 설계 문서에 이미 반영되었고, 코드 리뷰는 수정 완료 확인됨 (improvements.md "해결 완료된 이슈" 섹션에서 명시).

---

## 핵심 이슈 요약

### MinIO -> R2 전환 미반영 문서 (5건)

| 문서 | 구체적 위치 |
|------|------------|
| design/infrastructure/dev-environment.md | 전체 (다이어그램, docker-compose 표, 환경변수) |
| plans/infra/dev-server-plan.md | 4절 docker-compose 구조, 6-1절 환경변수, 8절 체크리스트, 10절 미결사항 |
| qa/README.md | 실행 환경의 MinIO 콘솔 참조 |
| qa/infra.md | TC-INFRA-07 Docker 컨테이너 기대 결과 |
| docs/README.md | 구조도(간접) -- MinIO는 아니지만 구조 누락 |

### CDN 직접 서빙 방식 미반영 (3건)

| 문서 | 구체적 위치 |
|------|------------|
| design/backend/api.md | `/view` API 응답 스키마 (content -> contentUrl) |
| design/backend/logic.md | 문서 서빙 시퀀스 다이어그램 |
| design/frontend/README.md | "직접 R2 접근 X" 역할 정의 |

### 설계-구현 괴리 (2건)

| 문서 | 구체적 내용 |
|------|------------|
| design/backend/security.md | sandbox 정책: 문서=`allow-scripts`만, 코드=`allow-same-origin allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox` |
| design/backend/auth.md + api.md | 이메일 회원가입/로그인이 구현되었으나 설계 문서에 미기재 |

### 삭제/아카이브 후보 (21건)

| 분류 | 파일 수 | 설명 |
|------|---------|------|
| reviews/ | 18 | 전부 반영 완료된 리뷰 |
| tasks/backend.md | 1 | 전 태스크 완료 |
| tasks/frontend.md | 1 | 대부분 완료, 잔여는 v0.2.0으로 이관 |
| tasks/improvements.md | 1 | 일부 해결, 잔여는 backlog/v0.2.0과 중복 |

---

## 권장 조치

### 즉시 (문서 갱신)

1. **dev-environment.md**: MinIO 참조를 전부 제거하고 R2 직접 사용으로 갱신
2. **api.md**: `/view` API 응답 스키마를 `contentUrl` 방식으로 갱신
3. **logic.md**: 문서 서빙 시퀀스 다이어그램에 CDN 직접 서빙 반영
4. **security.md**: sandbox 속성을 실제 구현과 맞추거나, 의도적 차이라면 근거 기록
5. **auth.md + api.md**: 이메일 회원가입/로그인 API 명세 추가
6. **qa/README.md, qa/infra.md**: MinIO 참조 제거

### 단기 (정리)

7. **reviews/ 18개**: `reviews/archived/` 하위로 이동하거나 삭제
8. **tasks/backend.md, tasks/frontend.md**: `archived` 상태로 변경하거나 삭제
9. **tasks/improvements.md**: 해결된 항목 표시 갱신, 잔여 항목을 backlog.md로 통합 후 삭제
10. **docs/README.md**: 전체 디렉토리 구조도 갱신
11. **infrastructure/README.md, deployment.md, operations.md**: 현재 환경(Galaxy Book)과 목표 환경(클라우드)을 명확히 구분하는 문구 추가
