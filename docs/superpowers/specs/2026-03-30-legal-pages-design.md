# 약관 페이지 및 노출 위치 설계

> 작성일: 2026-03-30
> 상태: approved

## 요약

DraftURL 서비스에 법적 약관 페이지(이용약관, 개인정보처리방침, 이용제한정책)를 추가하고, 4곳에 노출 링크를 배치한다. 약관 콘텐츠는 `docs/legal/` 디렉토리의 Markdown 파일을 빌드 타임에 읽어 렌더링하며, 한국어/영어 다국어를 지원한다.

## 배경

- 현재 서비스는 이메일 회원가입, 비회원 문서 생성, Umami 분석, Sentry 에러 추적 등 개인정보를 수집하고 있으나 약관 페이지가 없음
- 홈페이지 Footer, 회원가입, 로그인 페이지에 약관 링크가 `href="#"`으로 미연결 상태
- 비회원 문서 생성 시 약관 동의 고지가 전혀 없음

## 결정 사항

| 항목 | 결정 |
|------|------|
| 필요 문서 | 이용약관(ToS), 개인정보처리방침(Privacy), 이용제한정책(AUP) |
| 노출 위치 | 홈 Footer, 회원가입, 로그인, 비회원 문서 생성 (4곳) |
| 구현 방식 | `docs/legal/*.md` → 빌드 타임에 읽어 렌더링 |
| 페이지 구조 | 개별 페이지 (`/terms`, `/privacy`, `/aup`) |
| 다국어 | 한국어/영어 — locale별 md 파일 |

## 1. 파일 구조

### Markdown 소스

```
docs/legal/
├── terms-of-service.ko.md
├── terms-of-service.en.md
├── privacy-policy.ko.md
├── privacy-policy.en.md
├── acceptable-use-policy.ko.md
└── acceptable-use-policy.en.md
```

기존 파일 리네임:
- `terms-of-service.md` → `terms-of-service.ko.md`
- `privacy-policy.md` → `privacy-policy.ko.md`
- `acceptable-use-policy.md` → `acceptable-use-policy.ko.md`

영문 버전(`*.en.md`)은 한국어 원본을 기반으로 새로 작성.

### 프론트엔드 라우트

```
frontend/src/app/
├── terms/page.tsx       → /terms
├── privacy/page.tsx     → /privacy
└── aup/page.tsx         → /aup
```

## 2. 약관 페이지 구현

### 렌더링 방식

- 빌드 타임에 `fs.readFileSync`로 md 파일을 읽음
- `remark` + `rehype`로 Markdown → HTML 변환
- 현재 locale(`ko`/`en`)에 따라 해당 언어 파일을 선택

### 페이지 레이아웃

- 홈페이지와 동일한 헤더 사용
- 중앙 정렬 콘텐츠 영역 (max-width: 720px)
- 최종 수정일 표시
- 하단에 다른 약관 페이지로의 네비게이션 링크
- Footer에 이용약관/개인정보처리방침 링크

### 다국어 처리

- 현재 locale 감지: 기존 DraftURL의 locale 메커니즘 활용
- 한국어 페이지: 별도 안내 없음 (준거법 기준 원본)
- 영어 페이지: 상단에 고지문 표시
  > "This is a translation provided for your convenience. In case of any discrepancy, the Korean version shall prevail."

### 메타데이터

각 페이지에 적절한 title/description:
- `/terms`: "이용약관 | DraftURL" / "Terms of Service | DraftURL"
- `/privacy`: "개인정보처리방침 | DraftURL" / "Privacy Policy | DraftURL"
- `/aup`: "이용제한정책 | DraftURL" / "Acceptable Use Policy | DraftURL"

## 3. 노출 위치별 변경

### 3-1. 홈페이지 Footer

**파일**: `frontend/src/components/home/HomePage.tsx` (라인 197-198)

변경: `href="#"` → `/terms`, `/privacy`

### 3-2. 회원가입 페이지

**파일**: `frontend/src/app/auth/signup/page.tsx` (라인 277-283)

변경: `href="#"` → `/terms`, `/privacy`

### 3-3. 로그인 페이지

**파일**: `frontend/src/app/auth/login/page.tsx` (라인 277-285)

변경: `href="#"` → `/terms`, `/privacy`

### 3-4. 비회원 문서 생성 (신규)

**파일**: `frontend/src/components/common/PublishButton.tsx`

변경: "공유하기" 버튼 하단 또는 근처에 다음 텍스트 추가:
- 한국어: "공유 시 [이용약관]에 동의합니다"
- 영어: "By sharing, you agree to our [Terms of Service]"

링크는 `/terms`로 연결. 약관 동의 체크박스는 불필요 (클릭 시 묵시적 동의 방식).

## 4. 다국어 사전 업데이트

`dictionaries/ko.json`, `dictionaries/en.json`에 추가 필요한 키:

```json
{
  "legal": {
    "translationNotice": "This is a translation provided for your convenience. In case of any discrepancy, the Korean version shall prevail.",
    "lastUpdated": "최종 수정일",
    "aup": "이용제한정책"
  },
  "publish": {
    "agreement": "공유 시 {terms}에 동의합니다",
    "terms": "이용약관"
  }
}
```

기존 `home.footer`에 AUP 링크 추가 여부: Footer에는 이용약관 + 개인정보처리방침만 표시 (AUP는 이용약관에서 링크로 참조).

## 5. SEO

- 약관 페이지는 `robots.ts`에서 차단하지 않음 (공개 페이지)
- `sitemap.ts`에는 추가하지 않음 (검색 유입 불필요)

## 6. 범위 외

- 약관 동의 체크박스 (현재 "계속 진행하면 동의" 묵시적 동의 방식 유지)
- 약관 변경 이력 페이지
- 약관 동의 버전 추적 (DB에 동의 일시/버전 저장)
- 쿠키 동의 배너 (Umami는 쿠키 미사용)
