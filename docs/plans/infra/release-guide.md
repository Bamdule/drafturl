# DraftURL — 릴리즈 관리 가이드

> 작성일: 2026-03-27
> 상태: active

---

## 1. 버전 체계

Semantic Versioning (SemVer) 사용.

```
v{MAJOR}.{MINOR}.{PATCH}
```

| 구분 | 올리는 시점 | 예시 |
|------|------------|------|
| MAJOR | 정식 출시, 큰 전환점 (호환성 변경) | v1.0.0 |
| MINOR | 기능 추가 (Phase 단위 또는 주요 기능) | v0.2.0 |
| PATCH | 버그 수정, 소소한 개선 | v0.1.1 |

현재 버전: **v0.1.0** (MVP)

---

## 2. 브랜치 전략

**Feature Branch → main** 방식.

```
feature/기능명  →  PR/병합  →  main  →  git tag  →  배포
```

### 규칙

- **main**: 항상 배포 가능한 상태 유지
- **feature/\***: 기능 개발은 반드시 feature 브랜치에서 진행
- 브랜치 네이밍: `feature/기능명` (예: `feature/ai-edit`, `feature/version-history`)
- 버그 수정: `fix/버그명` (예: `fix/login-redirect`)
- 기능 완성 후 main에 병합, 태그 찍고 배포

### 배포 흐름

```
1. git checkout -b feature/새기능
2. 개발 및 테스트
3. main에 병합 (merge 또는 squash merge)
4. git tag v0.x.0
5. 갤럭시 북 개발서버에 배포
6. docs/releases/에 릴리즈 문서 작성
7. CHANGELOG.md 업데이트
```

---

## 3. Git 태그

배포마다 태그를 찍는다.

```bash
git tag v0.1.0
git push origin v0.1.0
```

- 태그는 main 브랜치의 배포 커밋에 찍는다
- 나중에 GitHub Releases 연동 또는 CI/CD 자동 배포 트리거로 활용 가능

---

## 4. 릴리즈 문서

`docs/releases/` 디렉토리에 버전별 문서를 작성한다.

- 파일명: `v{버전}.md` (예: `v0.1.0-mvp.md`, `v0.2.0.md`)
- 내용: 개요, 주요 기능/변경사항, 커밋 이력, 알려진 제한사항

---

## 5. CHANGELOG

프로젝트 루트의 `CHANGELOG.md`에 전체 버전 변경 이력을 역순으로 관리한다.

- 릴리즈 문서는 상세 내용, CHANGELOG는 요약본
- 형식: [Keep a Changelog](https://keepachangelog.com) 기반

---

## 6. 향후 확장

- 팀원 추가 시: PR 리뷰 필수화, branch protection 설정
- 운영 서버 이전 시: release 브랜치 도입 검토, CI/CD 자동 배포
- GitHub Releases: 태그 기반 자동 릴리즈 노트 생성
