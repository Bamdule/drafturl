# MVP 계획서 — HTML/MD 빠른 호스팅 서비스

> 작성일: 2026-03-21
> 상위 문서: docs/plans/service-plan.md
> 이전 문서를 완전히 대체함

---

## 1. 서비스 목표 재정의

### 핵심 목표

> **"LLM이 만들어준 HTML/MD 파일을 붙여넣기 한 번으로 즉시 URL을 생성하여 공유한다."**

AI 수정 기능(Claude API 연동)은 Phase 2로 이동한다. MVP의 유일한 목표는 **입력에서 공유 URL 생성까지의 시간을 최소화**하는 것이다.

### MVP 범위 확정 사항

| 확정 결정 | 근거 |
|-----------|------|
| HTML과 MD 단일 파일만 지원 | LLM 출력물의 99%가 self-contained 단일 HTML 또는 단일 MD |
| ZIP, 이미지, CSS/JS 분리 파일 제외 | MVP 단순화, Phase 2에서 확장 |
| Presigned URL 불필요 | 단일 텍스트 파일은 수KB~수백KB, 서버 경유로 충분 |
| `document_files` 테이블 불필요 | 문서당 파일이 항상 1개 |
| AI 수정 기능 제외 | Phase 2로 이동 |

---

## 2. MVP 핵심 기능 목록

| 우선순위 | 기능 | 설명 |
|---------|------|------|
| P0 | **텍스트 붙여넣기 배포** | HTML/MD 텍스트를 에디터에 붙여넣으면 즉시 URL 생성 |
| P0 | **단일 파일 업로드 배포** | `.html` 또는 `.md` 파일 드래그 앤 드롭 업로드 → 즉시 URL 생성 |
| P0 | **실시간 미리보기** | 배포 전 렌더링 결과를 즉시 확인 (HTML은 iframe, MD는 렌더링) |
| P0 | **고유 URL 생성** | `{서비스도메인}/{slug}` 형태로 즉시 발급 |
| P0 | **정적 파일 서빙** | 생성된 URL로 접근 시 HTML 페이지 또는 렌더링된 MD 제공 |
| P0 | **비로그인 임시 배포** | 가입 없이 즉시 배포, 24시간 후 자동 만료 |
| P1 | **사용자 인증** | 이메일/소셜 로그인 (문서 관리, 영구 URL 목적) |
| P1 | **내 문서 목록** | 로그인 사용자의 문서 대시보드 |
| P1 | **문서 수정/재배포** | 기존 문서 내용을 수정하여 같은 URL에 업데이트 |
| P2 | **Markdown 테마 렌더링** | MD → 스타일링된 HTML 자동 변환 (기본 테마 1개) |

**MVP에서 명시적으로 제외된 기능:**
- AI 수정 기능 (Claude API 연동)
- 버전 히스토리 (Phase 2)
- 비밀번호 보호 (Phase 2)
- ZIP/멀티파일 지원 (Phase 2)
- 커스텀 도메인 (Phase 2)
- 팀 워크스페이스 (Phase 3)
- CLI/API (Phase 3)

---

## 3. 기술 아키텍처 (단순화)

### 기술 스택 (기존 계획 유지)

| 구성요소 | 선택 | 비고 |
|---------|------|------|
| 프레임워크 | **Next.js 15 (App Router)** | SSR + API Routes |
| 언어 | **TypeScript** | |
| 스타일링 | **Tailwind CSS + shadcn/ui** | |
| 에디터 | **Monaco Editor** | HTML/MD 코드 편집용 |
| 인증 | **NextAuth.js (Auth.js v5)** | 소셜 로그인 |
| 데이터베이스 | **PostgreSQL (Supabase)** | 메타데이터 |
| 파일 저장소 | **Cloudflare R2** | HTML/MD 파일 저장 + CDN 서빙 |
| ORM | **Drizzle ORM** | 타입 안전 |
| MD 렌더링 | **unified (remark + rehype)** | MD → HTML 변환 |

### 아키텍처 개요 (MVP)

```
사용자 브라우저
    │
    ├─ 문서 작성 ──→ Next.js App (Vercel)
    │                   ├─ Auth.js (인증)
    │                   ├─ PostgreSQL (메타데이터)
    │                   └─ R2 (HTML/MD 파일)
    │
    └─ 공유 URL 접속 ──→ Next.js Route → R2에서 파일 로드 → 응답
                          (+ Cloudflare CDN 캐싱)
```

기존 계획 대비 제거된 요소: Claude API 연동, Presigned URL 플로우, ZIP 처리 파이프라인.

---

## 4. 파일 업로드/저장/서빙 플로우 (HTML/MD 전용)

### 4.1 텍스트 붙여넣기 플로우

```
클라이언트 (브라우저)
    │
    │  사용자가 에디터에 HTML 또는 MD 텍스트 붙여넣기
    │  → [실시간 미리보기] 확인
    │  → "공유하기" 클릭
    │
    │  POST /api/documents/create
    │  Body: {
    │    content: "<html>...</html>",
    │    type: "html" | "markdown",
    │    title: "My Document"     (선택)
    │  }
    │
    ▼
Next.js API Route (/api/documents/create)
    │
    │  1. 입력 검증
    │     - content 길이 체크 (최대 5MB)
    │     - type이 "html" 또는 "markdown"인지 확인
    │  2. 인증 확인 (선택적 — 비로그인 허용)
    │  3. slug 생성 (nanoid 8자리)
    │  4. HTML인 경우: 기본 새니타이징
    │  5. R2에 파일 저장
    │     - 키: documents/{document_id}/content.html 또는 content.md
    │  6. PostgreSQL에 메타데이터 저장
    │  7. 응답: { url, slug, expiresAt }
    │
    ▼
응답: { url: "https://share.서비스.com/xK9mP2nQ", expiresAt: "2026-03-22T..." }
```

### 4.2 파일 업로드 플로우

```
클라이언트 (브라우저)
    │
    │  .html 또는 .md 파일 드래그 앤 드롭 또는 파일 선택
    │  → 클라이언트 사전 검증:
    │     - 확장자가 .html, .htm, .md, .markdown 인지 확인
    │     - 파일 크기 5MB 이하 확인
    │  → 파일 내용 읽기 (FileReader)
    │  → [실시간 미리보기] 확인
    │  → "공유하기" 클릭
    │
    │  POST /api/documents/create
    │  Body: {
    │    content: "<파일 텍스트 내용>",
    │    type: "html" | "markdown",
    │    title: "filename.html"
    │  }
    │
    ▼
(이후 텍스트 붙여넣기 플로우와 동일)
```

핵심: 파일 업로드도 결국 텍스트 내용을 읽어서 같은 API로 전송한다. HTML/MD는 텍스트 파일이므로 `FileReader.readAsText()`로 읽어서 텍스트 붙여넣기와 동일한 경로로 처리한다. **별도의 바이너리 업로드 엔드포인트가 필요 없다.**

### 4.3 정적 파일 서빙 플로우

```
사용자 브라우저
    │
    │  GET https://share.서비스.com/{slug}
    │
    ▼
Cloudflare CDN
    │  Cache HIT → 즉시 응답
    │  Cache MISS ↓
    ▼
Next.js Route Handler (또는 Page)
    │
    │  1. slug로 PostgreSQL 조회 → document 메타데이터 획득
    │  2. 만료 체크 (expires_at < now → 404 또는 만료 안내 페이지)
    │  3. R2에서 파일 로드 (documents/{document_id}/content.html|md)
    │  4. HTML인 경우:
    │     → sandbox iframe 내에서 렌더링하는 래퍼 페이지 반환
    │  5. MD인 경우:
    │     → unified로 HTML 변환 → 스타일 적용 → 렌더링된 페이지 반환
    │  6. Cache-Control 헤더 설정
    │
    ▼
응답: 렌더링된 HTML 페이지
```

### 4.4 문서 수정 플로우 (로그인 사용자)

```
1. 내 문서 목록에서 문서 선택
2. 에디터에서 내용 수정
3. [실시간 미리보기] 확인
4. "업데이트" 클릭
5. PUT /api/documents/{slug}
   Body: { content: "...", type: "html"|"markdown" }
6. R2 파일 덮어쓰기 (같은 키)
7. CDN 캐시 무효화 (Cloudflare Cache Purge API)
8. 같은 URL 유지
```

---

## 5. DB 스키마 (단순화)

MVP에서는 3개 테이블만 필요하다.

```sql
-- 사용자 테이블 (NextAuth.js 기본 + 확장)
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          TEXT,
  email         TEXT UNIQUE,
  email_verified TIMESTAMPTZ,
  image         TEXT,
  plan          TEXT DEFAULT 'free',        -- 'free', 'starter', 'pro'
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- 문서 테이블 (핵심)
CREATE TABLE documents (
  id            TEXT PRIMARY KEY,            -- nanoid 8자리 (slug와 동일)
  slug          TEXT UNIQUE NOT NULL,        -- URL 경로 (= id)
  user_id       UUID REFERENCES users(id),   -- NULL = 비로그인 임시 문서
  title         TEXT,
  doc_type      TEXT NOT NULL,               -- 'html' | 'markdown'
  r2_key        TEXT NOT NULL,               -- R2 저장 경로
  content_size  INTEGER NOT NULL,            -- 바이트
  status        TEXT DEFAULT 'active',       -- 'active' | 'expired' | 'deleted'
  expires_at    TIMESTAMPTZ,                 -- 비로그인: 생성 후 24시간
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- 사용량 추적 테이블
CREATE TABLE storage_usage (
  user_id         UUID PRIMARY KEY REFERENCES users(id),
  total_bytes     BIGINT DEFAULT 0,
  document_count  INTEGER DEFAULT 0,
  updated_at      TIMESTAMPTZ DEFAULT NOW()
);
```

**기존 계획 대비 제거된 테이블:**
- `document_versions` — MVP에서는 버전 관리 없음 (Phase 2에서 추가)
- `document_files` — 문서당 파일이 항상 1개이므로 불필요

**`documents` 테이블 단순화 포인트:**
- `id`와 `slug`를 동일하게 사용 (nanoid 8자리)
- `r2_key` 필드에 R2 경로를 직접 저장 (별도 version/file 테이블 불필요)
- `content_size`로 용량 추적 (파일 1개이므로 이것이 곧 문서 크기)
- `user_id`가 NULL이면 비로그인 임시 문서

---

## 6. R2 버킷 구조 (단순화)

### 6.1 버킷 구조

```
r2-bucket: docshare-files
│
└── documents/
    ├── {document_id}/
    │   └── content.html          ← HTML 문서
    ├── {document_id}/
    │   └── content.md            ← Markdown 문서
    └── ...
```

기존 계획의 `uploads/` (임시 업로드 영역), `archived/` (아카이브), user_id별 디렉토리, version_id별 디렉토리를 모두 제거했다. 서버 경유 업로드이므로 임시 영역이 불필요하고, 버전 관리가 없으므로 version 디렉토리도 불필요하다.

### 6.2 키 네이밍 규칙

```
documents/{document_id}/content.{ext}
```

- `document_id`: nanoid 8자리 (예: `xK9mP2nQ`)
- `ext`: `html` 또는 `md`
- 예시: `documents/xK9mP2nQ/content.html`

### 6.3 R2 접근 방식

MVP에서는 R2를 퍼블릭 버킷으로 설정하지 않는다. Next.js API를 통해서만 파일을 읽고 쓴다 (서버 경유).

이유:
- 만료 체크, 접근 제어 등 비즈니스 로직을 서버에서 처리해야 함
- 퍼블릭 버킷 설정 시 만료된 문서도 직접 접근 가능한 보안 이슈
- MVP 단계에서 트래픽이 적으므로 서버 경유 비용이 무시할 수 있음

Phase 2에서 트래픽 증가 시 Cloudflare Workers + R2 바인딩으로 엣지 서빙 최적화 가능.

---

## 7. 보안 (HTML/MD 전용으로 축소)

### 7.1 입력 검증

| 항목 | 검증 내용 |
|------|----------|
| **파일 확장자** | `.html`, `.htm`, `.md`, `.markdown`만 허용 |
| **MIME 타입** | `text/html`, `text/markdown`, `text/plain`만 허용 |
| **콘텐츠 크기** | 최대 5MB (텍스트 파일이므로 충분) |
| **Rate Limiting** | IP 기반 분당 10회, 비로그인 사용자 일일 20개 문서 |

ZIP, 바이너리, 이미지 관련 보안 검증은 MVP에서 불필요 (해당 파일 유형 미지원).

### 7.2 HTML 서빙 보안

| 위협 | 대응책 |
|------|--------|
| **XSS (사용자 HTML 내 스크립트)** | 서빙 도메인을 메인 앱과 분리하여 sandbox iframe으로 격리 |
| **쿠키 탈취** | 서빙 도메인(`view.서비스.com`)과 앱 도메인(`서비스.com`) 분리 |
| **외부 리소스 로드** | Phase 2에서 CSP 강화 검토 (MVP에서는 LLM 출력물 호환성 우선) |

### 7.3 HTML 새니타이징 (최소한)

LLM이 생성한 HTML의 기능을 최대한 보존하되, 치명적 위험만 제거:

```
원본 HTML
  → <iframe>, <object>, <embed> 태그 제거
  → javascript: URL 스킴 제거
  → 나머지는 그대로 유지 (<script>, <style>, 인라인 이벤트 허용)
  → sandbox iframe에서 격리 서빙으로 안전성 확보
```

### 7.4 비로그인 문서 보안

- 24시간 자동 만료 (expires_at)
- slug가 nanoid 8자리 (경우의 수: 2.8조 개)로 URL 추측 불가
- CRON으로 만료 문서 주기적 삭제 (R2 파일 + DB 레코드)

---

## 8. 비용 추산

### 8.1 가정 (MVP 출시 후 1~2개월)

| 항목 | 수치 |
|------|------|
| 총 문서 생성 | 일 100개 |
| 활성 문서 (만료 전) | ~500개 |
| 평균 문서 크기 | 50KB (self-contained HTML/MD) |
| 월간 문서 조회 수 | 10,000회 |
| 등록 사용자 | 100명 |

### 8.2 저장소 사용량

```
활성 문서: 500개 x 50KB = 25MB
월간 총 생성: 3,000개 x 50KB = 150MB (비로그인은 24시간 후 삭제)
R2 실질 저장량: ~50MB (만료 삭제 후)
```

### 8.3 월간 비용

| 항목 | 비용 | 비고 |
|------|------|------|
| **Cloudflare R2 저장** | **$0** | 50MB << 10GB 무료 티어 |
| **R2 쓰기 (Class A)** | **$0** | 월 ~3,000회 << 1M 무료 |
| **R2 읽기 (Class B)** | **$0** | 월 ~10,000회 << 10M 무료 |
| **R2 이그레스** | **$0** | 항상 무료 |
| **Supabase PostgreSQL** | **$0** | Free 티어 (500MB DB, 50K 행) |
| **Vercel 호스팅** | **$0** | Hobby 플랜 (개인 프로젝트) |
| **도메인** | **~$10/년** | .com 도메인 |
| **총합** | **~$0/월** | 모든 서비스 무료 티어 내 |

### 8.4 스케일 시 (사용자 1,000명, 프로덕션)

| 항목 | 비용 |
|------|------|
| Supabase Pro | $25/월 |
| Vercel Pro | $20/월 |
| R2 | $0 (10GB 미만) |
| **총합** | **~$45/월** |

---

## 9. 구현 단계 (압축 일정)

### Week 1: 핵심 인프라 + 배포 기능

**Day 1-2: 프로젝트 설정**
- Next.js 15 프로젝트 생성 (App Router, TypeScript)
- Tailwind CSS + shadcn/ui 설정
- Drizzle ORM + Supabase PostgreSQL 연결
- DB 마이그레이션 (users, documents, storage_usage)
- R2 클라이언트 설정 (`@aws-sdk/client-s3`)
- 환경변수 설정 (R2, Supabase, NextAuth)

**Day 3-4: 문서 생성 API + 에디터 UI**
- `POST /api/documents/create` API Route 구현
  - 입력 검증 (크기, 타입)
  - nanoid slug 생성
  - R2에 파일 저장
  - DB 메타데이터 기록
  - 비로그인 시 expires_at = now + 24h
- 메인 페이지 에디터 UI
  - Monaco Editor (HTML/MD 탭 전환)
  - 파일 드래그 앤 드롭 영역
  - "공유하기" 버튼
- 실시간 미리보기 (iframe for HTML, unified for MD)

**Day 5: 정적 서빙**
- `/[slug]` 동적 라우트 구현
  - DB에서 문서 조회
  - 만료 체크
  - R2에서 파일 로드
  - HTML: sandbox iframe 래퍼 페이지 서빙
  - MD: remark/rehype로 렌더링 후 서빙
- Cache-Control 헤더 설정
- URL 복사 기능

### Week 2: 인증 + 문서 관리 + 마무리

**Day 1-2: 사용자 인증**
- NextAuth.js 설정 (Google, GitHub 소셜 로그인)
- 로그인/로그아웃 UI
- 로그인 사용자의 문서는 expires_at = NULL (영구)
- 비로그인 → 로그인 시 임시 문서 소유권 이전 (세션 쿠키 기반)

**Day 3-4: 문서 관리**
- 내 문서 목록 페이지 (대시보드)
- 문서 수정 기능 (`PUT /api/documents/{slug}`)
  - R2 파일 덮어쓰기
  - updated_at 갱신
- 문서 삭제 기능 (`DELETE /api/documents/{slug}`)
  - R2 파일 삭제 + DB soft delete
- storage_usage 업데이트 로직

**Day 5: 배포 준비 + 정리**
- 만료 문서 정리 CRON (Vercel Cron)
  - `expires_at < now`인 문서의 R2 파일 삭제 + status = 'expired'
- Rate Limiting 미들웨어 적용
- HTML 기본 새니타이징 적용
- Vercel 배포 설정
- 기본 에러 페이지 (404, 만료 안내)

### Week 3 (Buffer): 테스트 + 버그 수정 + 런칭

- E2E 테스트 (핵심 플로우)
- 모바일 반응형 점검
- 성능 최적화 (서빙 응답 시간)
- 랜딩 페이지 다듬기
- 프로덕션 환경 설정 (Supabase Pro, 도메인)
- 소프트 런칭

---

## 10. Phase 2 로드맵 (MVP 이후)

### Phase 2-A: AI 수정 기능 (2주)

| 작업 | 설명 |
|------|------|
| Claude API 연동 | 문서 내용을 컨텍스트로 전달, 수정 요청 처리 |
| AI 채팅 패널 UI | 우측 패널에서 프롬프트 입력 → 수정 결과 미리보기 |
| 버전 히스토리 | `document_versions` 테이블 추가, 수정 시 이전 버전 보존 |
| Diff 미리보기 | 수정 전/후 비교 UI |

### Phase 2-B: 기능 확장 (2주)

| 작업 | 설명 |
|------|------|
| 비밀번호 보호 | 공유 링크 접근 시 비밀번호 입력 |
| Markdown 테마 | 여러 렌더링 테마 선택 가능 |
| 커스텀 slug | 사용자가 URL 경로 지정 가능 |
| 플랜 체계 도입 | Free/Starter/Pro 구분, 결제 연동 (Stripe) |

### Phase 2-C: 파일 유형 확장 (2주)

| 작업 | 설명 |
|------|------|
| ZIP 업로드 지원 | 멀티파일 프로젝트 (HTML+CSS+JS+이미지) |
| `document_files` 테이블 추가 | ZIP 내 개별 파일 관리 |
| Presigned URL 방식 | 대용량 파일 업로드를 위한 직접 R2 업로드 |
| 이미지/폰트 지원 | 문서 내 삽입 리소스 |

### Phase 3: 플랫폼 확장

| 작업 | 설명 |
|------|------|
| CLI/API | 터미널에서 `docshare deploy ./file.html` |
| Claude Code MCP 연동 | Claude Code에서 직접 배포 |
| 팀 워크스페이스 | 다중 사용자 협업 |
| 커스텀 도메인 | 사용자 소유 도메인 연결 + SSL |
| 분석 대시보드 | 방문자 통계 |
| QR 코드 | 공유 편의 기능 |

---

## 11. 기술 의사결정 요약

| 결정 | 선택 | 대안 | 이유 |
|------|------|------|------|
| 저장소 | Cloudflare R2 | Supabase Storage, S3 | 이그레스 무료, 넉넉한 무료 티어 |
| 업로드 방식 | 서버 경유 전용 | Presigned URL | HTML/MD 텍스트는 수십KB~수백KB, 서버 경유로 충분 |
| R2 접근 | 서버 경유 (비공개 버킷) | 퍼블릭 버킷 + CDN | 만료 체크 등 비즈니스 로직 서버 처리 필요 |
| DB 스키마 | documents 단일 테이블 | versions + files 분리 | 문서당 파일 1개, 버전 관리 없음 (MVP) |
| 보안 격리 | 별도 서브도메인 + sandbox iframe | 같은 도메인 CSP | Same-Origin Policy로 세션 격리 |
| HTML 새니타이징 | 최소한 (iframe/object 제거만) | DOMPurify 전면 적용 | LLM 출력물 호환성 우선, sandbox로 안전성 확보 |
| ID/slug | nanoid 8자리 (ID = slug) | UUID + 별도 slug | 단순화, URL이 곧 ID |

---

## Sources

- [Cloudflare R2 Pricing](https://developers.cloudflare.com/r2/pricing/)
- [Supabase Pricing](https://supabase.com/pricing)
- [nanoid](https://github.com/ai/nanoid)
- [unified (remark/rehype)](https://unifiedjs.com/)
