# DraftURL QA 가이드

> 최종 수정일: 2026-03-25

---

## QA 체크리스트 요약

| 영역 | 문서 | TC 수 | 상태 |
|------|------|-------|------|
| 인증 | [auth.md](auth.md) | 12 | - |
| 문서 생성 | [document-create.md](document-create.md) | 10 | - |
| 문서 관리 | [document-manage.md](document-manage.md) | 11 | - |
| 문서 조회 | [document-view.md](document-view.md) | 8 | - |
| 에디터 | [editor.md](editor.md) | 12 | - |
| 인프라 | [infra.md](infra.md) | 8 | - |

**전체 테스트 케이스: 61개**

---

## QA 실행 환경

### 로컬 환경

```bash
# 1. 인프라 (DB) 실행
docker compose up -d

# 2. 백엔드 실행
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 프론트엔드 실행
cd frontend && npm run dev
```

- 프론트엔드: `http://localhost:3000`
- 백엔드 API: `http://localhost:8080`
- 파일 저장소: Cloudflare R2 (외부 서비스, Cloudflare 대시보드에서 관리)

### 개발 서버

- 프론트엔드: `https://drafturl.com`
- 백엔드 API: `https://api.drafturl.com`
- Cloudflare Tunnel 경유

---

## QA 실행 방법

### 전체 QA

모든 기능 영역의 TC를 순서대로 수행한다. 권장 순서:

1. **인프라** (infra.md) -- 서버 접근 가능 여부 확인
2. **인증** (auth.md) -- 로그인/회원가입 정상 동작 확인
3. **에디터** (editor.md) -- 에디터 입력/파일 드롭 확인
4. **문서 생성** (document-create.md) -- 공유 URL 발급 확인
5. **문서 조회** (document-view.md) -- 공유 URL 렌더링 확인
6. **문서 관리** (document-manage.md) -- 대시보드 CRUD 확인

### 부분 QA (변경 영향 범위별)

코드 변경 시 영향받는 영역만 선택적으로 수행한다.

| 변경 파일/컴포넌트 | 실행할 QA |
|-------------------|-----------|
| `auth/`, `token.ts`, `cookies.ts`, `SecurityConfig` | auth.md |
| `PublishButton`, `PublishResultModal`, `CreateDocumentUseCase` | document-create.md |
| `dashboard/`, `DocumentCard`, `DocumentList`, `StorageUsageBar` | document-manage.md |
| `[slug]/`, `HtmlViewer`, `MarkdownViewer`, `ViewDocumentUseCase` | document-view.md |
| `EditorPanel`, `PreviewPanel`, `FileDropZone`, `TypewriterOverlay` | editor.md |
| `docker-compose*`, `SecurityConfig`, `RateLimitFilter` | infra.md |

### TC 상태 표기

각 TC 수행 후 결과를 표기한다:

- **PASS** -- 기대 결과와 일치
- **FAIL** -- 기대 결과와 불일치 (버그 리포트 작성)
- **SKIP** -- 환경 제약으로 미수행
- **BLOCK** -- 선행 TC 실패로 수행 불가
