# 문서 조회 QA

> 트리거: `[slug]/`, `HtmlViewer`, `MarkdownViewer`, `ViewDocumentUseCase`

---

## 정상 조회

### TC-VIEW-01: HTML 문서 상세 보기

**사전 조건**: HTML 문서 생성 완료, 공유 URL 확보
**테스트 단계**:
1. 공유 URL(`/{slug}`)로 접근
**기대 결과**:
- 상단 CTA 바 ("나도 만들어보기" 링크) 표시
- HTML 문서가 iframe 내에서 풀스크린 렌더링
- 로컬에서 .html 파일을 열었을 때와 동일한 렌더링 결과
**자동화**: 가능

### TC-VIEW-02: Markdown 문서 상세 보기

**사전 조건**: Markdown 문서 생성 완료, 공유 URL 확보
**테스트 단계**:
1. 공유 URL(`/{slug}`)로 접근
**기대 결과**:
- 상단 CTA 바 표시
- Markdown이 HTML로 변환되어 스타일링된 상태로 렌더링
- 제목(h1~h6), 코드 블록, 테이블, 인용문 등 올바르게 표시
**자동화**: 가능

---

## 에러 상태

### TC-VIEW-03: 존재하지 않는 문서 (404)

**사전 조건**: 없음
**테스트 단계**:
1. 존재하지 않는 slug로 접근 (예: `/nonexistent99`)
**기대 결과**: 404 페이지 표시, "홈으로 돌아가기" 링크 제공
**자동화**: 가능

### TC-VIEW-04: 삭제된 문서 (410)

**사전 조건**: 문서 생성 후 대시보드에서 삭제
**테스트 단계**:
1. 삭제된 문서의 공유 URL로 접근
**기대 결과**: "문서가 삭제되었습니다" 메시지 표시, "이 문서는 소유자에 의해 삭제되었습니다." 설명 표시, "나도 만들어보기" 링크 제공
**자동화**: 가능

### TC-VIEW-05: 만료된 문서 (410)

**사전 조건**: 비로그인으로 생성한 문서의 만료 시간 경과 (24시간 후)
**테스트 단계**:
1. 만료된 문서의 공유 URL로 접근
**기대 결과**: "문서가 만료되었습니다" 메시지 표시, "이 문서는 만료 기간이 지나 더 이상 볼 수 없습니다." 설명 표시, "나도 만들어보기" 링크 제공
**자동화**: 가능 (DB에서 expiresAt을 과거로 수정하여 테스트)

---

## 렌더링 검증

### TC-VIEW-06: 외부 스크립트/스타일 포함 HTML 렌더링

**사전 조건**: 외부 CDN 리소스를 포함하는 HTML 문서 생성
**테스트 단계**:
1. 아래와 같은 HTML로 문서 생성:
   ```html
   <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5/dist/css/bootstrap.min.css">
   <div class="container mt-5">
     <h1 class="text-primary">Bootstrap Test</h1>
   </div>
   ```
2. 공유 URL로 접근
**기대 결과**: 외부 CSS가 로드되어 Bootstrap 스타일이 적용된 상태로 렌더링
**자동화**: 부분 가능

### TC-VIEW-07: iframe sandbox 보안

**사전 조건**: HTML 문서 상세 보기 상태
**테스트 단계**:
1. 공유 URL로 HTML 문서 접근
2. DevTools > Elements에서 iframe의 sandbox 속성 확인
3. iframe 내부에서 `parent.document` 접근 시도 (Console에서)
**기대 결과**: iframe에 `sandbox="allow-same-origin allow-scripts"` 속성 설정, iframe 내 스크립트에서 부모 페이지 DOM 직접 조작 불가
**자동화**: 가능

---

## 메타데이터

### TC-VIEW-08: Open Graph 메타데이터

**사전 조건**: 제목이 있는 문서 생성 완료
**테스트 단계**:
1. 공유 URL의 HTML 소스 확인 (View Source 또는 curl)
**기대 결과**:
- `<title>` 태그에 문서 제목 포함
- `og:title`, `og:description`, `og:type`, `og:site_name` 태그 존재
- `og:type`이 "article"
- `og:site_name`이 "DraftURL"
**자동화**: 가능
