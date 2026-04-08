# DraftURL 문서 지도

## 디렉토리 구조

```
docs/
├── features/    ← 기능 기획+설계 (임시 — 배포 후 삭제)
├── product/     ← 백로그, 제품 방향, 브랜딩 (영구)
├── infra/       ← 인프라/배포/보안 설계 (영구)
├── market/      ← 시장 분석, 경쟁사 (영구)
├── qa/          ← QA 시나리오 (영구)
├── releases/    ← 릴리즈 노트, 버전별 누적 (영구)
└── content/     ← 사이트 서빙 콘텐츠 (탐색 제외)
    ├── legal/   ← 약관, 개인정보처리방침
    └── guide/   ← MCP 가이드
```

## 핵심 문서

| 문서 | 역할 |
|------|------|
| [product/backlog.md](product/backlog.md) | 다음 작업 후보, 보류 항목 |
| [product/product.md](product/product.md) | 서비스 비전, 기능 목록, 로드맵 |
| [infra/deployment.md](infra/deployment.md) | CI/CD, GitHub Actions 배포 워크플로우 |
| [infra/dev-environment.md](infra/dev-environment.md) | 로컬 개발환경 (Docker Compose, Cloudflare Tunnel) |
| [market/market.md](market/market.md) | 경쟁사 분석, 핵심 해자, 방어 전략 |

## 문서 관리 원칙

- `features/`는 기능 개발 시 생성, 배포 완료 후 삭제
- `product/`, `infra/`, `market/`, `qa/`는 항상 현재 상태를 반영
- `releases/`는 버전별로 누적
