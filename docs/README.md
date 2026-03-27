# DraftURL 문서 지도

## 디렉토리 구조

```
docs/
├── design/                ← 설계 문서 (항상 최신 유지)
│   ├── backend/           ← API 명세, 인증/인가, 보안, 비즈니스 로직
│   ├── frontend/          ← 페이지 구조, 클라이언트, 컴포넌트, 상태관리
│   ├── infrastructure/    ← 배포, CI/CD, 개발환경, 운영
│   └── domain.md          ← 도메인 모델 & 데이터 설계 (ERD, DDL, 규칙)
├── plans/                 ← 계획 문서
│   ├── infra/             ← 인프라 계획 (개발서버, 릴리즈 가이드)
│   ├── market/            ← 시장 분석 (경쟁사, 전략, 리스크)
│   └── product/           ← 서비스 기능, 브랜딩, 버전별 태스크, 백로그
├── qa/                    ← QA 시나리오 (현행 테스트 케이스)
├── releases/              ← 릴리즈 노트 (버전별 누적)
├── mockups/               ← UI 목업 HTML
└── archive/               ← 완료된 문서 보관
    ├── reviews/           ← 반영 완료된 코드/문서 리뷰
    ├── tasks/             ← 완료된 개발 태스크
    └── qa-results/        ← 지난 QA 실행 결과
```

## 핵심 문서

| 문서 | 역할 |
|------|------|
| [design/domain.md](design/domain.md) | 도메인 모델, ERD, DDL, 도메인 규칙 |
| [design/backend/](design/backend/README.md) | API 명세, 인증/인가, 보안, 핵심 로직, 패키지 구조 |
| [design/frontend/](design/frontend/README.md) | 페이지 구조, 컴포넌트, 토큰 관리, 상태관리 |
| [design/infrastructure/](design/infrastructure/README.md) | 배포, CI/CD, 비용 추산, 개발 환경 |
| [plans/product/](plans/product/product.md) | 서비스 비전, 기능 목록, 로드맵, 수익 모델 |
| [plans/market/](plans/market/market.md) | 경쟁사 분석, 핵심 해자, 방어 전략 |

## 의존성 방향

```
plans/market/ ──> plans/product/ ──> design/
                                        ├── domain.md
                                        ├── backend/
                                        ├── frontend/
                                        └── infrastructure/
```

## 문서 관리 원칙

- **design/**, **plans/**, **qa/**는 항상 현재 상태를 반영한다
- 역할이 끝난 문서(완료된 리뷰, 태스크, QA 결과)는 **archive/**로 이동한다
- **releases/**는 버전별로 누적한다
