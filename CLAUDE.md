## Project
DraftURL — HTML/Markdown 문서를 URL로 즉시 공유하는 웹 서비스.
모노레포: backend (Spring Boot 3.4, Java 21) + frontend (Next.js 16, React 19, TypeScript)

## Workflow
- 복잡한 작업은 Plan 모드에서 시작, 구현 전 계획 승인
- 큰 변경은 리뷰 가능한 단위로 나눠 진행

## 검증 (기능 수정 시 필수)
- 백엔드: `cd backend && ./gradlew build -x test`
- 프론트엔드: `cd frontend && npx eslint src/`
- 완료 보고 전 테스트 먼저, 실패 시 직접 수정

## 배포 ("배포해줘" 요청 시)
- 버전: `VERSION` 파일 (SemVer). Major/Minor/Patch 자동 판단
- 절차: VERSION 업데이트 → CHANGELOG 추가 → 릴리즈 노트 작성 → backlog 갱신 → commit → tag → push (main + release)
- 트리거: `release` 브랜치 push → GitHub Actions 자동 배포

## 토큰 효율성
- snapshot > screenshot (시각적 확인 꼭 필요할 때만 screenshot)
- 파일은 한 번만 읽기. offset/limit 활용
- 에이전트에 "핵심만 간결하게" 지시
- **고비용 작업(fullPage 스크린샷, 대규모 코드 생성, 광범위 탐색) 전 사용자에게 사전 고지**
- 프로젝트 질문에 claude-api 같은 대형 외부 스킬 로드하지 않음

## 문서
- `docs/archive/` — 탐색 제외
- `docs/design/` — 설계 (현재 상태 반영)
- `docs/plans/` — 계획/태스크
- `docs/releases/` — 릴리즈 노트
- `docs/qa/` — QA 시나리오
