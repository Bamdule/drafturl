# DraftURL 도메인 분석 — 목업 기반 비즈니스 도메인 정리

> 작성일: 2026-03-21
> 기반: docs/design/mockups/*.html (5개 페이지)

---

## 1. 엔티티 및 속성

### 1.1 Document (문서)

대시보드(`dashboard.html`) 문서 카드와 공유 완료 페이지(`shared.html`) 메타 정보에서 도출:

| 속성 | 출처 페이지 | UI 표현 예시 |
|------|------------|-------------|
| `title` | dashboard (doc-title) | "주간 보고서 - 3월 3주차" |
| `doc_type` | dashboard (doc-type-badge) | HTML / MD (뱃지) |
| `slug` | dashboard (doc-url), shared (url-main) | `drafturl.com/xK9mP2nQ` |
| `content_size` | dashboard (doc-meta), shared (url-meta) | "12.4 KB" |
| `status` | dashboard (status-dot) | 활성(녹색) / 만료(회색) |
| `created_at` | shared (url-meta) | "방금 생성" |
| `updated_at` | dashboard (doc-meta) | "3시간 전 수정" |
| `expires_at` | shared (countdown) | "23시간 59분 42초 후 만료" |
| `user_id` | 암시적 (로그인 사용자 문서 vs 비로그인) | NULL = 비로그인 임시 문서 |
| `content` | index (editor textarea) | 실제 HTML/MD 텍스트 |
| `r2_key` | UI에 노출되지 않음 (내부 속성) | -- |

### 1.2 User (사용자)

대시보드(`dashboard.html`) 헤더의 사용자 메뉴와 로그인 페이지(`login.html`)에서 도출:

| 속성 | 출처 페이지 | UI 표현 예시 |
|------|------------|-------------|
| `name` | dashboard (user-name) | "김개발" |
| `avatar` (이니셜) | dashboard (user-avatar) | "K" |
| `plan` | dashboard (stat-card) | "Free" |
| `auth_provider` | login (social buttons) | Google / GitHub |

### 1.3 StorageUsage (사용량)

대시보드(`dashboard.html`) 통계 카드 영역에서 도출:

| 속성 | 출처 UI | 표현 예시 |
|------|---------|----------|
| `total_document_count` | stat-card "전체 문서" | "7" |
| `active_document_count` | stat-sub | "활성 5" |
| `expired_document_count` | stat-sub | "만료 2" |
| `total_bytes` | stat-card "저장 용량" | "124 KB" |
| `storage_limit` | stat-sub | "5 MB 중 사용" |
| `storage_percentage` | storage-bar-inner | 2.4% (프로그레스 바) |

### 1.4 Plan (플랜)

대시보드 stat-card와 로그인 페이지 benefits 영역에서 도출:

| 속성 | 출처 UI | 표현 예시 |
|------|---------|----------|
| `name` | stat-card "플랜" | "Free" |
| `document_limit` | stat-sub | "문서 3개" |
| `ai_edit_limit` | stat-sub | "AI 수정 월 10회" |
| `storage_limit` | stat-sub (저장 용량 카드) | "5 MB" |

---

## 2. 사용자 액션 및 유스케이스

### 2.1 index.html (랜딩 + 에디터 페이지)

| 액션 | UI 요소 | 제안 API 매핑 |
|------|---------|--------------|
| HTML/MD 텍스트 붙여넣기 | editor textarea | (클라이언트 전용) |
| HTML/MD 탭 전환 | editor-tab 버튼 ("HTML", "Markdown") | (클라이언트 전용) |
| 실시간 미리보기 확인 | preview-iframe | (클라이언트 전용) |
| 파일 드래그 앤 드롭 / 파일 선택 | drop-zone, fileInput (.html/.htm/.md/.markdown) | (클라이언트 전용 - FileReader) |
| 문서 공유하기 | "공유하기" 버튼 (publishBtn) | `POST /api/documents` |
| 공유 URL 복사 | modal 내 "복사" 버튼 | (클라이언트 전용 - clipboard) |
| 공유 상세 보기로 이동 | modal 내 "상세 보기" 링크 | 네비게이션: shared.html |
| 대시보드 이동 | 헤더 "내 문서" 링크 | 네비게이션: dashboard.html |
| 로그인 페이지 이동 | 헤더 "로그인" 버튼 | 네비게이션: login.html |

**키보드 단축키:** Cmd/Ctrl+Enter = 공유하기 실행

### 2.2 shared.html (공유 완료 페이지)

| 액션 | UI 요소 | 제안 API 매핑 |
|------|---------|--------------|
| 공유 URL 복사 | "복사" 버튼 | (클라이언트 전용 - clipboard) |
| Twitter/X 공유 | share-btn (X 아이콘) | (외부 URL 오픈) |
| 이메일 공유 | share-btn (이메일 아이콘) | (mailto: 링크) |
| Slack 공유 | share-btn (Slack 아이콘) | (외부 URL 오픈) |
| 문서 뷰어 보기 | "문서 보기" 버튼 | 네비게이션: viewer.html |
| 새 문서 만들기 | "새 문서 만들기" 버튼 | 네비게이션: index.html |
| 무료 가입하기 | CTA 배너 내 "무료 가입하기" 버튼 | 네비게이션: login.html |

### 2.3 viewer.html (문서 뷰어 페이지)

| 액션 | UI 요소 | 제안 API 매핑 |
|------|---------|--------------|
| URL 복사 | "URL 복사" 버튼 | (클라이언트 전용 - clipboard) |
| DraftURL 홈으로 이동 | 로고 링크 | 네비게이션: index.html |
| 나도 문서 공유하기 | 하단 CTA "나도 문서 공유하기" | 네비게이션: index.html |

**서버 측 액션:** slug로 문서 조회 + R2에서 콘텐츠 로드 = `GET /api/documents/{slug}`

### 2.4 dashboard.html (대시보드 페이지)

| 액션 | UI 요소 | 제안 API 매핑 |
|------|---------|--------------|
| 내 문서 목록 조회 | 페이지 로드 시 | `GET /api/documents` (인증 필요) |
| 사용량 통계 조회 | stats-bar | `GET /api/users/me/usage` |
| URL 복사 | 문서별 링크 아이콘 버튼 | (클라이언트 전용) |
| 문서 보기 | 문서별 눈 아이콘 버튼 | 네비게이션: viewer.html |
| 문서 편집 | 문서별 연필 아이콘 버튼 | 네비게이션: index.html (편집 모드) |
| 문서 삭제 | 문서별 휴지통 아이콘 버튼 -> 확인 모달 | `DELETE /api/documents/{slug}` |
| 새 문서 만들기 | 헤더 "새 문서" 버튼, 페이지 내 "새 문서 만들기" 버튼 | 네비게이션: index.html |

### 2.5 login.html (로그인 페이지)

| 액션 | UI 요소 | 제안 API 매핑 |
|------|---------|--------------|
| Google 로그인 | "Google로 계속하기" 버튼 | `GET /api/auth/google` (OAuth2 리다이렉트) |
| GitHub 로그인 | "GitHub로 계속하기" 버튼 | `GET /api/auth/github` (OAuth2 리다이렉트) |
| 로그인 없이 사용하기 | 하단 "로그인 없이 사용하기" 링크 | 네비게이션: index.html |

### 2.6 통합 API 엔드포인트 목록

| 메서드 | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| `POST` | `/api/documents` | 문서 생성 (공유하기) | 선택적 |
| `GET` | `/api/documents/{slug}` | 문서 조회 (뷰어 서빙) | 불필요 |
| `PUT` | `/api/documents/{slug}` | 문서 수정/재배포 | 필수 |
| `DELETE` | `/api/documents/{slug}` | 문서 삭제 | 필수 |
| `GET` | `/api/documents` | 내 문서 목록 | 필수 |
| `GET` | `/api/users/me/usage` | 사용량 통계 | 필수 |
| `GET` | `/api/auth/google` | Google OAuth2 시작 | -- |
| `GET` | `/api/auth/github` | GitHub OAuth2 시작 | -- |
| `GET` | `/api/auth/callback` | OAuth2 콜백 | -- |

---

## 3. 페이지 간 네비게이션 플로우

```
                    +-----------+
                    | login.html|
                    | (로그인)  |
                    +-----+-----+
                      ^   |
                      |   | (로그인 성공)
                      |   v
+----------+    +-----+------+    +------------+
|index.html| <->|dashboard   | -->| viewer.html|
|(에디터)  |    |.html       |    | (뷰어)     |
+----+-----+    |(대시보드)  |    +------+-----+
     |          +-----+------+           |
     | (공유하기)      ^                  |
     v                 |                  |
+----+-----+           |                  |
|shared.html+-----------+                  |
|(공유완료) +------------------------------+
+-----------+
```

| 출발 페이지 | 목적 페이지 | 트리거 | 조건 |
|------------|-----------|--------|------|
| index.html | shared.html | "공유하기" 성공 후 모달에서 "상세 보기" | 없음 |
| index.html | dashboard.html | 헤더 "내 문서" 클릭 | 로그인 권장 |
| index.html | login.html | 헤더 "로그인" 클릭 또는 만료 안내 링크 | 비로그인 상태 |
| shared.html | viewer.html | "문서 보기" 클릭 | 없음 |
| shared.html | index.html | "새 문서 만들기" 클릭 | 없음 |
| shared.html | login.html | "무료 가입하기" CTA 클릭 | 비로그인 상태 |
| shared.html | dashboard.html | 헤더 "내 문서" 클릭 | 로그인 권장 |
| viewer.html | index.html | 로고 클릭 또는 하단 CTA 클릭 | 없음 |
| dashboard.html | index.html | "새 문서" / 편집 아이콘 | 로그인 상태 |
| dashboard.html | viewer.html | 보기 아이콘 | 로그인 상태 |
| login.html | dashboard.html | 로그인 성공 | -- |
| login.html | index.html | "로그인 없이 사용하기" | -- |

### 인증 상태에 따른 접근 제어

| 페이지 | 비로그인 | 로그인 | 차이점 |
|--------|---------|--------|--------|
| index.html | 접근 가능 | 접근 가능 | 헤더에 "로그인" vs "사용자 메뉴" 표시 |
| shared.html | 접근 가능 | 접근 가능 | 비로그인 시 만료 카운트다운 + 가입 CTA 표시 |
| viewer.html | 접근 가능 | 접근 가능 | 변화 없음 (공개 뷰어) |
| dashboard.html | 접근 불가 (로그인 필요) | 접근 가능 | 인증 필수 |
| login.html | 접근 가능 | 리다이렉트 (대시보드로) | -- |

---

## 4. 상태 및 전이

### 4.1 Document 상태

```
            [생성]
              |
              v
          +--------+
          | active |  (활성 - 녹색 점)
          +---+----+
              |
     +--------+--------+
     |                  |
 (24h 경과,          (사용자가
  비로그인)           삭제 클릭)
     |                  |
     v                  v
 +---------+      +---------+
 | expired |      | deleted |
 | (만료)  |      | (삭제됨)|
 +---------+      +---------+
```

- **active**: 대시보드에서 녹색 점 + "활성" 텍스트
- **expired**: 대시보드 stat-sub에 "만료 2"로 카운트 (회색 점)
- **deleted**: 삭제 모달 확인 후 전이 ("문서가 영구적으로 삭제됩니다. 공유 URL도 더 이상 작동하지 않습니다.")

### 4.2 만료 카운트다운

shared.html에서 비로그인 문서에 대해 실시간 카운트다운:
- 형식: `{시간}시간 {분}분 {초}초 후 만료`
- 초기값: 약 24시간
- CTA: "가입하면 영구 보관!"

### 4.3 인증 상태 전이

```
[비로그인] ---(Google/GitHub 로그인)---> [로그인]
[로그인]   ---(로그아웃)-------------> [비로그인]
```

---

## 5. 기존 설계와의 차이점

### 5.1 플랜 관련 수치 불일치

| 항목 | service-plan.md | file-upload-storage-plan.md | 목업 |
|------|----------------|---------------------------|------|
| Free 문서 만료 | 7일 | 24시간 | 24시간 |
| Free 문서 수 | 3개 | 3개 | 대시보드에 5개 활성 표시 (불일치) |

### 5.2 MVP 범위 외 기능이 목업에 포함됨

- **AI 수정 월 10회** — 대시보드 플랜 카드에 표시되나 MVP에서 제외된 기능
- **QR 코드** — shared.html에 "Coming Soon" 플레이스홀더

### 5.3 설계에 없는 새로운 기능이 목업에 존재

- **소셜 공유 버튼** (Twitter/X, 이메일, Slack) — shared.html에 있으나 설계 미반영
- **뷰어 하단 CTA** ("이 문서는 DraftURL로 만들어졌습니다") — 바이럴 성장 UI
- **에디터 bytes 카운터** — 5MB 제한 관련 UX

### 5.4 문서 제목 입력 UI 부재

설계에서 `title`은 선택적 파라미터이나, index.html에 제목 입력 필드가 없음. HTML `<title>` 태그 자동 추출 또는 파일명 사용 로직 필요.

### 5.5 아키텍처 불일치

mvp-service-design.md는 Spring Boot 분리 아키텍처이나, file-upload-storage-plan.md는 여전히 Next.js 풀스택 기준. 두 문서 간 정합성 업데이트 필요.

---

## 6. 추가 관찰 사항

### 6.1 비로그인 가입 유도 터치포인트

1. index.html: 하단 만료 안내 "로그인하면 영구 보관!"
2. shared.html: 만료 카운트다운 CTA + "무료 가입하기" 버튼
3. viewer.html: 하단 CTA "나도 문서 공유하기"

### 6.2 서비스 이용약관 / 개인정보 처리방침

login.html에 동의 문구가 있으나 링크가 `#`(플레이스홀더). 런칭 전 실제 문서 필요.

---

## 검토 결과

> 검토일: 2026-03-21
> 검토자: doc-reviewer 에이전트
> 검토 기준: docs/design/mockups/*.html (5개), docs/design/mvp-service-design.md, docs/plans/file-storage/file-upload-storage-plan.md 직접 대조

---

### 정확한 항목 (확인됨)

아래 항목은 목업 HTML과 대조 결과 올바르게 도출되었다.

- **엔티티 속성 대부분 정확**: `title`, `doc_type`, `slug`, `content_size`, `status`, `created_at`, `updated_at`, `expires_at`, `user_id`, `content`, `r2_key` 모두 목업에서 확인 가능한 범위 내에서 정확하게 도출됨.
- **User 엔티티**: `name("김개발")`, `avatar("K")`, `plan("Free")`, `auth_provider(Google/GitHub)` 모두 login.html과 dashboard.html에서 확인됨.
- **StorageUsage 수치**: "전체 문서 7", "활성 5 / 만료 2", "124 KB", "5 MB 중 사용", "2.4%" 모두 dashboard.html의 stats-bar와 일치함.
- **Plan 속성**: "Free", "문서 3개", "AI 수정 월 10회" 모두 dashboard.html stat-card에서 확인됨.
- **액션 목록 전반**: 5개 페이지의 버튼/링크/단축키가 빠짐없이 도출됨. Cmd/Ctrl+Enter 단축키도 index.html JS에서 확인됨.
- **네비게이션 플로우 테이블**: 출발-목적지-트리거 조합이 HTML href 속성과 JS redirect와 일치함.
- **Document 상태 전이 다이어그램**: active/expired/deleted 전이와 삭제 모달 문구("공유 URL도 더 이상 작동하지 않습니다")가 dashboard.html과 일치함.
- **만료 카운트다운 형식**: shared.html JS에서 `23시간 59분 42초` 초기값과 1초 감소 로직 확인됨.
- **5.1 ~ 5.5 불일치 항목**: 실제 목업과 설계 문서 대조 결과 모두 사실로 확인됨.

---

### 오류 및 수정 필요 항목

#### O-1 (Minor) `shared.html` 헤더에 "내 문서" 링크 누락

**현황**: 섹션 2.2에서 shared.html 액션 목록에 `shared.html → dashboard.html` 경로(헤더 "내 문서" 클릭)가 기재되어 있고, 섹션 3 네비게이션 테이블에도 동일 행이 존재한다.

**실제 HTML**: shared.html 헤더를 확인하면 `<a href="dashboard.html" class="btn btn-ghost">내 문서</a>` 링크가 실제로 존재한다. 따라서 테이블 자체는 정확하다.

**단, 섹션 2.2 액션 테이블에는 이 헤더 링크가 없고** 섹션 3 네비게이션 테이블에서만 다룬다. shared.html 헤더의 "내 문서" 링크와 "로그인" 버튼은 2.2 액션 테이블에서 명시적으로 누락되어 있다.

**수정 제안**: 섹션 2.2 shared.html 액션 테이블에 다음 두 행을 추가한다.

```
| 대시보드 이동  | 헤더 "내 문서" 링크 | 네비게이션: dashboard.html |
| 로그인 페이지 이동 | 헤더 "로그인" 버튼 | 네비게이션: login.html |
```

---

#### O-2 (Minor) `viewer.html` 액션 테이블에서 "내 문서" 경로 미언급

**현황**: viewer.html에는 top bar가 존재하며, `<a href="index.html">DraftURL</a>` 로고 링크만 있다. "내 문서" 또는 "로그인" 버튼은 없다. 따라서 섹션 2.3 뷰어 액션 테이블은 구조적으로 정확하다.

**단**, 섹션 3 네비게이션 테이블에 `viewer.html → dashboard.html` 경로가 없다. viewer.html에서 대시보드로 직접 가는 링크는 실제로도 없으므로 테이블은 올바르다. 추가 이슈 없음.

---

#### O-3 (Major) `Document.status` 값 집합 불일치

**현황**: 섹션 1.1과 섹션 4.1 상태 다이어그램에서 `active`, `expired`, `deleted` 세 가지 값만 언급한다.

**실제 설계**: mvp-service-design.md 섹션 3.2에서 `DocumentStatus`는 `PENDING | ACTIVE | EXPIRED | DELETED` 네 가지이며, 섹션 3.6 DR-9에서 "PENDING 문서는 5분 이내 ACTIVE 전이가 없으면 스케줄러가 정리"라는 도메인 규칙도 명시되어 있다.

**영향**: `PENDING` 상태를 누락하면 2단계 커밋 패턴(R2 업로드 전 DB에 먼저 INSERT)이 도메인 분석에 반영되지 않아, 향후 API 설계나 구현 시 이 상태를 놓칠 수 있다.

**수정 제안**: 섹션 1.1 status 행 설명을 "활성(녹색) / 만료(회색) / 삭제됨 / 대기중(pending)"으로 갱신하고, 섹션 4.1 상태 다이어그램에 `pending` → `active` 전이를 추가한다.

```
[생성 요청]
    |
    v
+----------+
| pending  |  (DB INSERT, R2 업로드 전)
+----+-----+
     |
 (R2 업로드 완료)
     |
     v
+--------+
| active |
+---+----+
    ...
```

---

#### O-4 (Minor) `User` 엔티티에서 `email` 속성 누락

**현황**: 섹션 1.2 User 엔티티 표에는 `name`, `avatar`, `plan`, `auth_provider` 네 가지만 있다.

**실제 설계**: mvp-service-design.md 섹션 3.2에서 User는 `email(unique, not null)`을 핵심 식별자로 사용하며, `provider`+`providerId` 조합도 포함한다. `profileImage`(이미지 URL), `providerId`(OAuth2 ID)도 별도 속성이다.

**목업 관련**: login.html에는 email을 직접 입력하는 UI가 없으므로 목업만으로는 도출되지 않는 속성이다. 그러나 OAuth2 플로우에서 필수로 전달되는 속성이므로 도메인 분석 완성도를 위해 포함해야 한다.

**수정 제안**: 섹션 1.2에 다음 속성을 추가한다.

| 속성 | 출처 페이지 | UI 표현 예시 |
|------|-----------|-------------|
| `email` | OAuth2 콜백 (UI 미노출) | -- |
| `provider_id` | OAuth2 콜백 (UI 미노출) | -- |
| `profile_image` | OAuth2 콜백 (UI 미노출) | -- |

---

#### O-5 (Minor) `StorageUsage.document_count`가 전체 수인지 활성 수인지 모호

**현황**: 섹션 1.3 `total_document_count`와 `active_document_count`를 별도 속성으로 기재하고 있으나, mvp-service-design.md의 `StorageUsage` 엔티티는 `documentCount` 단일 필드만 갖는다.

**실제 설계**: `storage_usage.document_count`는 하나의 정수 필드이며, 설계 문서에서는 이것이 활성 문서 수인지 전체 문서 수인지 명확히 정의하지 않는다.

**영향**: `total_document_count`와 `active_document_count`를 별도 DB 컬럼으로 추적하는 것처럼 읽히는데, 실제 스키마와 다르다.

**수정 제안**: 섹션 1.3을 `StorageUsage` 엔티티의 실제 필드 구조(`document_count`, `total_bytes`)에 맞게 재작성하고, "활성 5 / 만료 2" 표시는 대시보드 조회 시 집계 쿼리로 계산함을 명시한다.

---

#### O-6 (Minor) `API 엔드포인트` 경로 불일치

**현황**: 섹션 2.6 통합 API 목록에 문서 생성 엔드포인트를 `POST /api/documents`로 기재하고 있다.

**실제 설계**: file-upload-storage-plan.md 섹션 4.1에서는 `POST /api/documents/create`이며, mvp-service-design.md 섹션 4에서는 `/api/v1/documents` (Spring Boot REST API, base URL이 `api.{서비스도메인}`)로 정의되어 있다.

**영향**: 엔드포인트 경로가 세 문서에서 세 가지로 다르다. 도메인 분석 문서에 기재된 `/api/documents`는 mvp-service-design.md와도 file-upload-storage-plan.md와도 정확히 일치하지 않는다.

**수정 제안**: 섹션 2.6 상단에 "API 경로는 최신 설계 문서(mvp-service-design.md)를 기준으로 하며, 문서 간 불일치는 섹션 5.5 참조" 주석을 추가하거나, mvp-service-design.md 기준 경로로 통일한다.

---

### 누락된 관찰 사항

#### N-1 `index.html` 피처 카드의 "AI 수정 (Coming Soon)"

**현황**: 섹션 5.2에서 대시보드 플랜 카드의 "AI 수정 월 10회"와 shared.html의 QR 코드만 Coming Soon으로 언급하고 있다.

**실제**: index.html 하단 피처 카드(`.features` 영역)에 "AI 수정 (Coming Soon)" 카드가 별도로 존재한다(`<h3>AI 수정 (Coming Soon)</h3>`). login.html benefits 목록에도 "AI 수정 기능 (Coming Soon)" 항목이 있다. Coming Soon 터치포인트가 3곳(index.html, shared.html, login.html)에 있음을 추가해야 한다.

---

#### N-2 `dashboard.html` 헤더에 "로그아웃" 버튼 부재

**현황**: 섹션 4.3 인증 상태 전이에서 `[로그인] → (로그아웃) → [비로그인]` 전이가 명시되어 있으나, dashboard.html 헤더의 user-menu(`김개발`)를 클릭해도 드롭다운이나 로그아웃 버튼이 목업에 구현되어 있지 않다.

**의미**: 로그아웃 액션의 트리거 UI가 목업에 정의되어 있지 않다. 섹션 2.4 dashboard.html 액션 테이블에 "로그아웃" 행이 없는 것은 목업의 현재 상태를 정확히 반영한 것이지만, 이 공백이 의도적 누락인지 미구현인지 명시해야 한다.

**수정 제안**: 섹션 2.4나 섹션 6에 "user-menu 클릭 시 드롭다운(로그아웃 포함)은 목업에 미구현 — 실제 구현 시 추가 필요"라는 관찰 사항을 추가한다.

---

#### N-3 `dashboard.html` 문서 목록의 "내 문서 5개" vs. 통계 카드 "전체 7개" 불일치

**현황**: 섹션 5.1에서 "Free 문서 수 3개 제한인데 대시보드에 5개 활성 표시"라고 언급하고 있다. 이는 올바른 지적이다.

**추가 관찰**: 이와 별개로 dashboard.html에는 두 가지 숫자가 동시에 표시된다. stats-bar의 "전체 문서: 7(활성 5/만료 2)"과 page-header의 "내 문서 5개"다. 두 숫자가 다르다는 것(7 vs 5)은 문서 목록이 활성 문서만 표시하고 만료 문서는 숨긴다는 설계 의도를 암시하지만, 도메인 분석 문서 어디에도 "목록에 표시되는 문서의 필터 기준"이 언급되지 않는다.

**수정 제안**: 섹션 2.4 "내 문서 목록 조회" 행에 "(활성 문서만 표시, 만료 문서는 제외)"를 추가하거나, 섹션 6에 별도 관찰 사항으로 추가한다.

---

#### N-4 `viewer.html` top bar에서 문서 제목 표시

**현황**: 섹션 2.3 뷰어 액션 테이블이나 엔티티 분석 어디에도 뷰어의 top bar에 문서 제목이 표시됨을 언급하지 않는다.

**실제**: viewer.html top bar(`viewer-bar-left`)에 `<span>주간 보고서</span>`가 표시된다. 즉 뷰어는 문서 메타데이터(`title`)를 조회하여 branding bar에 렌더링한다. 이는 `GET /api/documents/{slug}` 응답에 `title`이 포함되어야 함을 의미하며, `title`이 null인 경우의 fallback 처리도 필요하다.

**수정 제안**: 섹션 2.3 뷰어 서버 측 액션 설명에 "응답에 title 포함, null 시 fallback(slug 또는 '제목 없음') 필요"를 추가한다.

---

#### N-5 `shared.html`의 `url-meta`에서 `doc_type` 노출 방식

**현황**: 섹션 1.1에서 `doc_type`의 UI 표현 예시를 "HTML / MD (뱃지)"로 기재하고 있는데, 이는 dashboard.html의 `doc-type-badge`를 기준으로 한 것이다.

**추가 관찰**: shared.html의 `url-meta` 영역에서는 뱃지가 아닌 텍스트 아이콘(`HTML 문서`)으로 표시된다(`<span>📄 HTML 문서</span>`). 즉 같은 `doc_type` 속성이 페이지마다 다른 UI 패턴으로 렌더링된다. 이 차이는 향후 프론트엔드 컴포넌트 설계 시 참고해야 할 사항이다.

---

### 검토 요약

| 구분 | 건수 | 내용 |
|------|------|------|
| 정확히 도출된 항목 | - | 엔티티/속성, 대부분의 액션, 네비게이션 테이블, 불일치 5개 항목 모두 목업과 일치 |
| Major 수정 필요 | 1건 | O-3: Document.status에서 `pending` 상태 누락 |
| Minor 수정 필요 | 5건 | O-1, O-2, O-4, O-5, O-6 |
| 추가 관찰 사항 | 5건 | N-1 ~ N-5 |

**전반적 평가**: 도메인 분석의 정확도는 높다. 5개 목업 HTML을 빠짐없이 반영했고 불일치 항목도 정확히 식별했다. 주요 보완 포인트는 `pending` 상태 누락(O-3)과 대시보드 문서 목록의 표시 필터 기준 미명시(N-3)이다.
