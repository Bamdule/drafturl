# 경쟁사 분석: PageDrop

> 작성일: 2026-03-29
> 대상: https://pagedrop.io (서비스), https://pagedrop.dev (SEO 랜딩)

---

## 1. 서비스 개요

PageDrop은 HTML을 붙여넣으면 1초 이내에 공유 URL을 발급하는 무료 인스턴트 호스팅 서비스.
DraftURL의 가장 직접적인 경쟁자.

- **슬로건**: "Drop Your HTML. Get a Live Link. Instantly."
- **런칭**: Product Hunt 런칭 완료
- **인프라**: Vercel 호스팅 추정 (보안 체크포인트 기반)

---

## 2. 비즈니스 모델

### 현재: 무료 기반 + Creator Pass 마이크로 결제

| 구분 | 내용 |
|------|------|
| **기본 가격** | 완전 무료 (가입 없음, API 키 없음, 신용카드 없음) |
| **수익원** | Creator Pass (TTL 연장권) |
| **Creator Pass** | Starter (7일), Pro (14일), Ultimate (30일) TTL 연장 |
| **특징** | 스택 가능, 선물 가능, 만료 없음 |

### TTL 기반 수익 구조

```
무료 사용자:
  - 비공개 페이지: 기본 TTL 3일
  - Explore 공개 페이지: 기본 TTL 14일
  - 좋아요 보너스: 10 likes → +1일 (최대 24일)

유료 전환 동기:
  - 영구 보관이 필요한 사용자 → Creator Pass 구매
  - Pass로 TTL을 7/14/30일 연장
```

### 비즈니스 모델 분석

- **프리미엄 전환 동기**: 콘텐츠가 만료되기 전에 연장하려면 결제 필요
- **바이럴 요소**: Explore 공개 시 TTL 보너스 → 공유 유도
- **진입 장벽 제거**: 가입/API키 완전 불필요 → 마찰 최소화
- **약점**: 가입 시스템 없음 → 사용자 식별/리텐션 데이터 부족

---

## 3. 전체 기능 목록

### 3-1. 핵심 기능

| 기능 | 상세 |
|------|------|
| **즉시 배포** | HTML 붙여넣기 → 1초 이내 URL 발급 |
| **다중 포맷** | HTML, Markdown, PDF, ZIP 지원 |
| **Markdown 렌더링** | 마크다운 자동 → 스타일된 HTML 페이지 변환 |
| **ZIP 지원** | 루트에 index.html 포함 필수, 최대 10MB |
| **PDF 호스팅** | PDF 파일 직접 업로드 → URL 발급 |

### 3-2. 공유 & 커뮤니티

| 기능 | 상세 |
|------|------|
| **Explore (탐색)** | 공개 페이지 갤러리, 커뮤니티 발견 기능 |
| **좋아요 시스템** | 10 likes → +1일 TTL 보너스 (최대 24일) |
| **포크 & 리믹스** | 공개 페이지를 복제하여 독립적으로 수정 |
| **서브도메인 URL** | `my-project.pagedrop.io` 형태 (이전 URL 자동 리다이렉트) |

### 3-3. 보안 & 관리

| 기능 | 상세 |
|------|------|
| **비밀번호 보호** | 생성 시 비밀번호 설정, 방문자 인증 24시간 유지 |
| **TTL (만료 설정)** | 영구 기본 / 7일·14일·30일 선택 가능 |
| **삭제 토큰** | 생성 시 반환되는 토큰으로 언제든 삭제 가능 |
| **자동 보안 검사** | 모든 업로드 자동 스캐닝, 악성 콘텐츠 제거 |
| **투명성 페이지** | `/transparency` — 차단된 콘텐츠 공개 |

### 3-4. 개발자 도구

| 기능 | 상세 |
|------|------|
| **REST API** | 인증 불필요, AI 에이전트/자동화 최적화 |
| **MCP 서버** | `npx pagedrop-mcp`로 Claude/Cursor/VS Code 연동 |
| **배치 배포** | 단일 API 호출로 최대 20페이지 동시 배포 |
| **iframe 임베드** | `/embed/:siteId` 경로로 다른 사이트에 삽입 |
| **CORS 지원** | 교차 출처 헤더 자동 포함 |

### 3-5. API 스펙

```
POST   /api/v1/sites          — 사이트 생성 (인증 불필요)
PUT    /api/v1/sites/:siteId  — 사이트 업데이트
DELETE /api/v1/sites/:siteId  — 사이트 삭제 (X-Delete-Token 헤더 필요)

제한:
  - HTML: 최대 1MB
  - PDF/ZIP: 최대 10MB
  - ZIP: 루트에 index.html 필수
  - 배치: 최대 20페이지/요청
```

### 3-6. MCP 서버 설정

```json
{
  "mcpServers": {
    "pagedrop": {
      "command": "npx",
      "args": ["-y", "pagedrop-mcp"]
    }
  }
}
```

Claude, VS Code, Cursor 등 MCP 호환 클라이언트에서 바로 사용 가능.

---

## 4. SEO & 마케팅 전략

### 멀티 도메인 전략

| 도메인 | 역할 |
|--------|------|
| `pagedrop.io` | 메인 서비스 (앱) |
| `pagedrop.dev` | SEO 랜딩 페이지 (개발자 타겟 키워드) |
| `pagedrop.app` | 추가 랜딩 (링크 페이지 생성) |
| `pagedrop.pro` | 로컬 비즈니스 원페이지 제작 서비스 ($297) |

### SEO 기법

- **콘텐츠 마케팅**: `/articles/` 경로에 경쟁사 비교 글 게시
  - "PageDrop vs HTML Hosting Providers"
  - 검색 키워드 선점 목적
- **JSON-LD**: WebApplication + FAQ + HowTo 스키마 적용
- **키워드 전략**: "free HTML hosting", "instant deploy", "no signup" 반복
- **Product Hunt 런칭**: 초기 트래픽 + 백링크 확보

### Explore = SEO 자산

사용자가 만든 공개 콘텐츠가 Google에 인덱싱됨 → 사용자 콘텐츠가 곧 SEO 자산.

---

## 5. 주요 타임라인

| 시기 | 이벤트 |
|------|--------|
| 2025.11 | Explore (커뮤니티 갤러리) 런칭 |
| 2026.02 | Creator Pass 도입 (마이크로 결제 수익 모델) |
| 2026.Q1 | 서브도메인 URL 지원 (`project.pagedrop.io`) |
| 2026.Q1 | MCP 서버 제공 |

---

## 6. DraftURL과의 기능 비교

| 기능 | DraftURL | PageDrop | 비고 |
|------|----------|----------|------|
| HTML 공유 | O | O | 동일 |
| Markdown | O | O | 동일 |
| PDF | X | O | **PageDrop 우세** |
| ZIP (멀티파일) | X | O | **PageDrop 우세** |
| 가입 없이 사용 | O (24시간 만료) | O (TTL 기반) | 유사 |
| 가입 시 영구 보관 | O | X (Pass 구매 필요) | **DraftURL 우세** |
| 문서 편집 | O (대시보드) | X (삭제 후 재생성) | **DraftURL 우세** |
| 실시간 미리보기 | O | X | **DraftURL 우세** |
| 비밀번호 보호 | O | O | 동일 |
| API | O (인증 필요) | O (인증 불필요) | PageDrop 더 개방적 |
| MCP 서버 | 개발 중 | O (이미 제공) | **PageDrop 우세** |
| 포크/리믹스 | X | O | **PageDrop 우세** |
| 배치 배포 | X | O (20개) | **PageDrop 우세** |
| iframe 임베드 | X | O | **PageDrop 우세** |
| 커뮤니티 갤러리 | X | O (Explore) | **PageDrop 우세** |
| 다국어 | O (한/영) | X (영어만) | **DraftURL 우세** |
| 대시보드 | O | X | **DraftURL 우세** |
| OAuth 로그인 | O (Google/GitHub) | X | **DraftURL 우세** |
| 콘텐츠 마케팅 | X | O (블로그) | **PageDrop 우세** |
| Product Hunt | X | O | **PageDrop 우세** |

---

## 7. DraftURL 전략 시사점

### 즉시 대응 (v0.3.0)

1. **MCP 서버 출시 최우선** — PageDrop이 이미 제공 중. AI 에이전트 연동이 핵심 차별점인데 뒤처지고 있음
2. **Google Search Console 등록** — SEO 설정만 하고 등록 안 하면 무의미

### 단기 대응 (v0.4.0)

3. **콘텐츠 마케팅 시작** — "DraftURL vs PageDrop" 비교 글, 사용법 가이드
4. **Product Hunt 런칭** — 초기 백링크 + 트래픽 확보
5. **iframe 임베드 지원** — 다른 사이트에서 문서를 삽입할 수 있도록

### 중기 차별화

6. **편집 기능 강화** — PageDrop은 수정 불가 (삭제 후 재생성). DraftURL의 에디터 + 대시보드가 핵심 차별점
7. **버전 히스토리** — PageDrop에 없는 기능. 문서 이력 관리가 프로 사용자 전환 동기
8. **AI 수정 기능** — "표를 보기 좋게 정리해줘" 같은 프롬프트 편집. PageDrop에는 없는 완전히 새로운 가치

### PageDrop과 다른 방향으로 포지셔닝

```
PageDrop: "붙여넣고 잊어버리는" 일회성 공유 도구
  → 가입 없음, 관리 기능 없음, 편집 불가

DraftURL: "붙여넣고 계속 발전시키는" 살아있는 문서 플랫폼
  → 대시보드, 편집, 버전 관리, AI 수정 → 문서의 생명주기 관리
```

---

## Sources

- [PageDrop 홈](https://pagedrop.io/)
- [PageDrop About](https://pagedrop.io/about)
- [PageDrop SEO 랜딩](https://pagedrop.dev/)
- [PageDrop Changelog](https://pagedrop.io/changelog)
- [PageDrop on Product Hunt](https://www.producthunt.com/products/pagedrop)
- [PageDrop vs Handoff vs VibeShare](https://handoff.host/blog/pagedrop-vs-handoff-vs-vibeshare/)
- [PageDrop vs HTML Hosting Providers](https://pagedrop.io/articles/pagedrop-vs-html-hosting-providers)
