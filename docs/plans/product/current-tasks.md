# 현재 작업 태스크

> 아직 배포되지 않은 작업 목록. 배포 시 변경 범위에 따라 버전(major/minor/patch)을 결정하고, 이 파일을 `vX.X.X-tasks.md`로 복사한 뒤 초기화한다.

## 완료

- [x] 게스트 문서 이관 기능 (v0.9.5)
- [x] PublishResultModal 로그인 CTA 강화 (v0.9.5)
- [x] 편집하기 버튼 추가 (v0.9.5)
- [x] 에디터 placeholder 개선 (v0.9.5)

## 진행 중

## 예정
- [ ] GSC 색인 재요청 (drafturl.com/ko, /en)
- [ ] SEO 블로그 콘텐츠 작성 (3~5개)
- [ ] Product Hunt 런칭 준비

## 아이디어 백로그

### 컬렉션 (문서 묶음 + 페이지 네비게이션)
관련 문서들을 하나의 URL로 묶어 페이지 넘김으로 볼 수 있는 기능.

**DB**
- `collections` 테이블: id, slug, title, owner_id, expires_at
- `collection_items` 테이블: collection_id, document_id, position

**URL 구조**
- 목차: `drafturl.com/c/{slug}`
- 페이지: `drafturl.com/c/{slug}/{n}`
- 기존 문서 URL 그대로 유지

**기능 범위**
- 대시보드에서 문서 선택 → 컬렉션 생성 + 순서 지정
- 목차 페이지 (문서 리스트 + "처음부터 읽기")
- 문서 뷰어에 컬렉션 네비게이션 바 (이전/다음, n/전체)
- 컬렉션 제목 수정 / 문서 추가·제거·순서 변경
- 비로그인 컬렉션은 24시간 만료

**API**
- `POST /api/v1/collections`
- `GET /api/v1/collections/{slug}`
- `PATCH /api/v1/collections/{slug}`
- `POST/DELETE /api/v1/collections/{slug}/items`

### 태그 기반 문서 관리
문서가 쌓일수록 대시보드 관리가 어려워지는 문제 해결.
- 문서에 태그 추가/삭제 (예: `#claude`, `#보고서`, `#팀공유`)
- 태그 필터링으로 대시보드 문서 좁히기
- 태그별 문서 목록 페이지 (`/dashboard?tag=보고서`)
- (옵션) 제목/내용 검색
- (옵션) 즐겨찾기/상단 고정
