# DraftURL 문서 인덱스

## 구조

```
docs/
  plans/                          -- 기획 (왜 만드는가, 무엇을 만들지)
    market/                       -- 시장 분석
      market.md                   -- 경쟁사, 전략, 리스크
    product/                      -- 제품 기획
      product.md                  -- 비전, 기능, 플로우, 수익 모델
  design/                         -- 설계 (어떻게 만들지)
    domain.md                     -- 도메인 모델 (엔티티, 관계, 비즈니스 규칙, ERD, DDL)
    backend/
      README.md                   -- 요약, 기술 스택, 패키지 구조, 의존성 규칙, 설계 결정, 구현 주의사항
      api.md                      -- API 공통 규격, 엔드포인트 목록, 각 API 상세 명세, 에러 코드
      auth.md                     -- 인증/인가 (OAuth2, JWT, Refresh Token Rotation, Security 필터 체인)
      logic.md                    -- 핵심 로직 (2단계 커밋, 서빙, 수정, 삭제, 스케줄러, 상태 전이)
      security.md                 -- 보안 (입력 검증, HTML 서빙 보안, 인증 보안, HTML 처리 정책, 리스크)
    frontend/
      README.md                   -- 요약, 기술 스택, 역할 정의, 디렉토리 구조, 구현 주의사항
      pages.md                    -- 페이지/라우트 구조, 주요 페이지 상세
      client.md                   -- API 클라이언트 레이어, 에러 처리, 토큰 관리, Zustand 상태관리
    infrastructure/
      README.md                   -- 요약, 인프라 기술 스택, 아키텍처 다이어그램, 확장성 계획, 설계 결정
      dev-environment.md          -- 개발 환경 (Docker Compose, 실행 가이드, Cloudflare Tunnel)
      deployment.md               -- 배포 (CI/CD 워크플로우, Dockerfile, 환경변수, 호스팅 비교)
      operations.md               -- 운영 (비용 추산, 성능 목표, 캐싱 전략)
  tasks/                          -- 구현 태스크
  reviews/                        -- 검토 결과
  mockups/                        -- UI 목업 HTML (5개 페이지)
```

## 핵심 문서

| 문서 | 역할 | 상태 |
|------|------|------|
| [plans/product/](plans/product/product.md) | 서비스 비전, 기능 목록, 로드맵, 수익 모델 | active |
| [plans/market/](plans/market/market.md) | 경쟁사 분석, 핵심 해자, 방어 전략, 실행 전략 | active |
| [design/domain.md](design/domain.md) | 도메인 모델, ERD, DDL, 도메인 규칙 | active |
| [design/backend/](design/backend/README.md) | API 명세, 인증/인가, 보안, 핵심 로직, 패키지 구조 | active |
| [design/frontend/](design/frontend/README.md) | 페이지 구조, 컴포넌트, 토큰 관리, 상태관리 | active |
| [design/infrastructure/](design/infrastructure/README.md) | 배포, CI/CD, 비용 추산, 개발 환경, 설계 결정 기록 | active |

## 의존성 방향

```
plans/market/ ──> plans/product/ ──> design/
                                            ├── domain.md
                                            ├── backend/    (API, 보안, 로직)
                                            ├── frontend/   (UI, 토큰 관리)
                                            └── infrastructure/  (배포, 비용)

mockups/ ──> design/frontend/ (UI 참조)
reviews/ ──> plans/, design/ (검토 대상)
```

## 상태 정의

- **active** -- 현행 문서, 의사결정 근거로 사용 가능
- **draft** -- 작성 중, 미확정
- **archived** -- 이력 참고용, 현행 기준 아님
