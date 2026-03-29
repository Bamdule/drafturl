## Project
DraftURL — HTML/Markdown 문서를 URL로 즉시 공유하는 웹 서비스.
모노레포: backend (Spring Boot 3.4, Java 21) + frontend (Next.js 16, React 19, TypeScript)

## Workflow
- 복잡한 작업은 Plan 모드에서 시작하고, 구현 전에 계획을 승인받는다
- 큰 변경사항은 리뷰 가능한 단위로 나누어 진행한다

## 검증 요구사항 (기능 수정 시 반드시 실행)
- 백엔드 변경 후: `cd backend && ./gradlew build -x test` 성공 확인
- 프론트엔드 변경 후: `cd frontend && npx eslint src/` 에러 없음 확인
- 양쪽 변경 시 두 검증 모두 실행
- 기능 변경 후: 관련 API를 curl로 호출하여 정상 응답 확인 (문서 생성/조회 등)
- 사용자에게 완료 보고 전에 테스트를 먼저 수행하고, 실패 시 직접 수정

## 큰 기능 완료 후 정리
- MEMORY.md 인덱스와 실제 메모리 파일의 일치 여부를 확인한다
- 더 이상 유효하지 않은 메모리(완료된 프로젝트, 변경된 규칙)를 정리한다
- 코드에서 유추 가능한 내용이 메모리에 중복 저장되어 있으면 제거한다

## 버전 관리 (배포 시 자동 수행)
- 현재 버전: 루트 `VERSION` 파일 (single source of truth)
- SemVer 규칙: Major(하위호환 깨짐), Minor(새 기능), Patch(버그/보안 수정)
- "배포해줘" 요청 시 자동으로 수행할 것:
  1. 변경 내용 분석 → major/minor/patch 판단 → `VERSION` 업데이트
  2. `docs/plans/product/vX.X.X-tasks.md` 체크박스 반영
  3. `CHANGELOG.md` 해당 버전 섹션 추가
  4. `docs/releases/vX.X.X.md` 릴리즈 노트 작성
  5. git commit → git tag vX.X.X → push (main + release)
- 배포 트리거: `release` 브랜치 push 시 GitHub Actions 자동 배포

## 문서 규칙
- `docs/archive/`는 완료된 문서 보관소이며, 탐색 대상에서 제외한다
- `docs/design/` — 설계 문서 (항상 현재 상태 반영)
- `docs/plans/` — 계획/태스크 문서
- `docs/releases/` — 릴리즈 노트
- `docs/qa/` — QA 시나리오
