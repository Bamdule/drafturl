## Project
DraftURL — HTML/Markdown 문서를 URL로 즉시 공유하는 웹 서비스.
모노레포: backend (Spring Boot 3.4, Java 21) + frontend (Next.js 16, React 19, TypeScript)

## Workflow
- 파일 3개 이상 수정 or 새 API/DB 변경 → Plan 모드 먼저, 구현 전 승인
- 단순 버그 수정, 1~2파일 → 바로 구현
- 큰 변경은 리뷰 가능한 단위로 나눠 진행

## 작업 규칙
- git commit/push는 사용자 명시적 요청 시에만
- 다이어그램은 Mermaid 문법

## 검증 (기능 수정 시 필수)
- 백엔드: `cd backend && ./gradlew build -x test`
- 프론트엔드: `cd frontend && npx eslint src/`
- 완료 보고 전 테스트 먼저, 실패 시 직접 수정

## 배포 ("배포해줘" 요청 시)
- 버전: `VERSION` 파일 (SemVer). Major/Minor/Patch 자동 판단
- 절차: VERSION 업데이트 → CHANGELOG 추가 → 릴리즈 노트 작성 → backlog 갱신 → commit → tag → push (main + release) → **플랜 파일 + 기능 설계 문서 삭제**
- 트리거: `release` 브랜치 push → GitHub Actions 자동 배포
- 플랜 삭제: `~/.claude/plans/<plan-name>.md` + `docs/features/<기능명>.md` (서브에이전트 파일 포함)

## 토큰 효율성
- snapshot > screenshot (시각적 확인 꼭 필요할 때만 screenshot)
- **고비용 작업(fullPage 스크린샷, 대규모 코드 생성, 광범위 탐색) 전 사용자에게 사전 고지**
- 프로젝트 질문에 claude-api 같은 대형 외부 스킬 로드하지 않음

## 문서
- `docs/features/` — 기능 기획+설계 (임시, 배포 후 삭제)
- `docs/product/` — 백로그, 제품 방향 (영구)
- `docs/infra/` — 인프라/배포 설계 (영구)
- `docs/market/` — 시장/경쟁 분석 (영구)
- `docs/qa/` — QA 시나리오 (영구)
- `docs/releases/` — 릴리즈 노트 (영구)
- `docs/content/` — 탐색 제외 (사이트 서빙 콘텐츠)
