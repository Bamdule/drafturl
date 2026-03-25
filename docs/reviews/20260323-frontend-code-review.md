# 프론트엔드 코드 리뷰

> 검토일: 2026-03-23
> 검토 대상: `frontend/src/` 하위 TypeScript/React 소스 코드
> 파일 수: 41개 (.ts/.tsx)
> 기술 스택: Next.js 16, React 19, TypeScript, Tailwind CSS 4, Zustand, Monaco Editor

## 요약

전반적으로 구조가 깔끔하고 설계 문서와의 일치도가 높다. 다만 **iframe sandbox에 `allow-scripts`를 허용하면서 `allow-same-origin`도 함께 부여하여 XSS 공격에 노출되는 보안 취약점**, **OAuth provider 판별 로직의 신뢰성 부재**, **토큰을 non-httpOnly 쿠키에 저장하여 XSS 시 탈취 가능한 점** 등 보안 관련 이슈가 존재한다. 성능 측면에서는 매 키 입력마다 Blob을 생성하는 바이트 카운트, 에디터 글로벌 스토어 공유로 인한 페이지 간 상태 오염 등의 개선 여지가 있다.

## 조치 우선순위 테이블

| Severity | ID | 분류 | 제목 | 위치 |
|----------|-----|------|------|------|
| CRITICAL | C1 | 보안 | iframe sandbox에서 allow-scripts + allow-same-origin 동시 허용 (XSS) | `HtmlViewer.tsx:14`, `PreviewPanel.tsx:73` |
| CRITICAL | C2 | 보안 | JWT 토큰이 non-httpOnly 쿠키에 저장되어 XSS 시 탈취 가능 | `token.ts` 전체 |
| MAJOR | M1 | 정확성 | OAuth provider 판별을 인가 코드 prefix로 추측 | `auth/callback/page.tsx:51` |
| MAJOR | M2 | 보안 | getCookie 정규식이 쿠키 이름에 특수문자 포함 시 ReDoS/오동작 가능 | `token.ts:17`, `auth/callback/page.tsx:11` |
| MAJOR | M3 | 상태관리 | 에디터 글로벌 스토어가 메인/편집 페이지 간 상태 오염 유발 | `useEditorStore.ts`, `EditDocumentPage` |
| MAJOR | M4 | 정확성 | 서버 컴포넌트에서 클라이언트 fetch로 API 호출 시 쿠키 미전달 | `app/[slug]/page.tsx:51` |
| MAJOR | M5 | UX/정확성 | clipboard 실패 시에도 성공 토스트 표시 | `DocumentViewPage.tsx:30` |
| MAJOR | M6 | 보안 | dangerouslySetInnerHTML 사용 | `page.tsx:124` (HomePage FeatureCard) |
| MINOR | m1 | 성능 | 매 키 입력마다 Blob 생성으로 바이트 카운트 계산 | `EditorPanel.tsx:16-29` |
| MINOR | m2 | 정확성 | DocumentCard에서 URL 파싱 실패 가능성 | `DocumentCard.tsx:78` |
| MINOR | m3 | 코드 품질 | getCookie/deleteCookie 함수 중복 정의 | `token.ts`, `auth/callback/page.tsx` |
| MINOR | m4 | UX | Login 페이지 OAuth 에러 시 사용자에게 에러 메시지 미표시 | `auth/login/page.tsx:36-38` |
| MINOR | m5 | 상태관리 | Auth store 새로고침 시 초기화되어 인증 상태 유실 | `useAuthStore.ts` |
| MINOR | m6 | 성능 | MarkdownViewer에서 렌더링 Promise 에러 미처리 | `MarkdownViewer.tsx:18` |
| SUGGESTION | S1 | 성능 | 미리보기 Markdown 스타일 중복 제거 | `PreviewPanel.tsx`, `MarkdownViewer.tsx` |
| SUGGESTION | S2 | UX | 편집 페이지에서 unsaved changes 경고 없음 | `dashboard/[slug]/edit/page.tsx` |
| SUGGESTION | S3 | 보안 | 토큰 쿠키에 Secure 플래그 미설정 | `token.ts:54-62` |
| SUGGESTION | S4 | 접근성 | Dialog에 aria-labelledby/aria-describedby 미설정 | `dialog.tsx` |

---

## 이슈 상세

### C1. iframe sandbox에서 allow-scripts + allow-same-origin 동시 허용 (XSS)

**위치**: `frontend/src/components/viewer/HtmlViewer.tsx:14`, `frontend/src/components/editor/PreviewPanel.tsx:73`

**현재 코드**:
```tsx
// HtmlViewer.tsx
<iframe
  srcDoc={content}
  sandbox="allow-same-origin allow-scripts"
  title={title ?? "문서"}
/>

// PreviewPanel.tsx
<iframe
  srcDoc={wrappedHtml}
  sandbox="allow-same-origin allow-scripts"
  title="미리보기"
/>
```

**문제점**: `sandbox` 속성에서 `allow-scripts`와 `allow-same-origin`을 동시에 허용하면, iframe 내 스크립트가 부모 페이지의 DOM과 쿠키(non-httpOnly)에 접근할 수 있다. 사용자가 악성 HTML을 붙여넣기하면:
1. `document.cookie`로 JWT 토큰 탈취 가능
2. `parent.document`로 부모 페이지 DOM 조작 가능
3. `fetch()`로 외부 서버에 토큰 전송 가능

**권장 수정**:
- **HtmlViewer** (서빙용): `sandbox="allow-scripts"` (allow-same-origin 제거). 스크립트 실행이 필요하지만 부모 도메인 접근은 차단.
- **PreviewPanel** (미리보기): `sandbox="allow-scripts"` (allow-same-origin 제거). 혹은 별도 도메인(예: `preview.drafturl.com`)에서 렌더링.
- 장기적으로는 CSP 헤더와 별도 도메인 iframe 서빙이 최선.

---

### C2. JWT 토큰이 non-httpOnly 쿠키에 저장

**위치**: `frontend/src/lib/auth/token.ts` 전체

**현재 코드**:
```ts
// token.ts 주석
// 설계 문서에서는 httpOnly 쿠키를 권장하지만,
// 클라이언트 컴포넌트에서 API 호출 시 Authorization 헤더를 직접 설정해야 하므로
// MVP에서는 non-httpOnly 쿠키로 관리한다.
```

**문제점**: 설계 문서(`client.md`)에서는 httpOnly 쿠키를 명시적으로 권장하고 있으나, 구현에서는 `document.cookie`로 직접 접근 가능한 non-httpOnly 쿠키를 사용한다. C1의 XSS 취약점과 결합되면 토큰 탈취가 즉시 가능하다.

**권장 수정**:
- Next.js API Route를 프록시로 사용하여 서버 사이드에서 httpOnly 쿠키를 읽고 Spring Boot API에 전달하는 구조로 변경
- 또는 BFF(Backend For Frontend) 패턴 적용
- MVP 단계에서 당장 변경이 어렵다면, 최소한 C1의 sandbox 수정으로 XSS 공격 표면을 축소해야 함

---

### M1. OAuth provider 판별을 인가 코드 prefix로 추측

**위치**: `frontend/src/app/auth/callback/page.tsx:51`

**현재 코드**:
```tsx
const provider = code.startsWith("4/") ? "google" : "github";
```

**문제점**: OAuth 인가 코드의 형식은 provider가 언제든 변경할 수 있으며, 다른 provider도 `4/`로 시작할 수 있다. 이 추측은 신뢰할 수 없고, 잘못된 provider로 콜백을 보내면 인증이 실패한다.

**권장 수정**:
- 로그인 시 `oauth_state` 쿠키에 provider 정보를 함께 저장하거나, state 파라미터에 provider를 인코딩
- 또는 콜백 URL을 provider별로 분리: `/auth/callback/google`, `/auth/callback/github`

---

### M2. getCookie 정규식이 쿠키 이름에 특수문자 포함 시 오동작

**위치**: `frontend/src/lib/auth/token.ts:17`, `frontend/src/app/auth/callback/page.tsx:11`

**현재 코드**:
```ts
const match = document.cookie.match(
  new RegExp(`(^| )${name}=([^;]+)`),
);
```

**문제점**: `name`에 정규식 특수문자가 포함되면 예기치 않은 동작이 발생한다. 현재 쿠키 이름(`access_token`, `refresh_token`, `oauth_state`)에는 `_`만 포함되어 당장 문제는 없으나, 쿠키 이름이 변경되거나 `.`을 포함하게 되면 버그가 된다.

**권장 수정**:
```ts
function getCookie(name: string): string | null {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = document.cookie.match(new RegExp(`(?:^|; )${escaped}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}
```

---

### M3. 에디터 글로벌 스토어가 메인/편집 페이지 간 상태 오염 유발

**위치**: `frontend/src/lib/store/useEditorStore.ts`, `frontend/src/app/dashboard/[slug]/edit/page.tsx`

**문제점**: `useEditorStore`가 단일 글로벌 인스턴스이므로:
1. 메인 페이지에서 내용을 입력한 후 편집 페이지로 이동하면, `loadDocument()`가 호출되기 전 순간에 이전 내용이 보임
2. 편집 페이지에서 뒤로 가기로 메인 페이지로 돌아오면 편집 중이던 문서 내용이 메인 에디터에 남아 있음
3. 편집 페이지에서 `useEffect` cleanup이 없어 스토어 상태가 정리되지 않음

**권장 수정**:
- 편집 페이지의 `useEffect`에 cleanup에서 `reset()` 호출 추가
- 또는 편집 페이지에서 별도의 로컬 상태를 사용하고 `EditorPanel`에 props로 전달하는 구조로 변경
- 장기적으로는 각 페이지별 context 또는 store slice 분리

---

### M4. 서버 컴포넌트에서 클라이언트 fetch로 API 호출 시 쿠키 미전달

**위치**: `frontend/src/app/[slug]/page.tsx:51`

**현재 코드**:
```tsx
// Server Component
export default async function SlugPage({ params }: SlugPageProps) {
  const { slug } = await params;
  const doc = await getDocumentView(slug);  // apiFetch 사용
  ...
}
```

**문제점**: `apiFetch`는 `document.cookie`에서 토큰을 읽는 클라이언트 전용 함수이다. 서버 컴포넌트에서 호출 시 `document`가 undefined이므로 토큰이 전달되지 않는다. `getDocumentView`는 `auth: false`이므로 현재는 동작하지만, 향후 인증이 필요한 API를 서버 컴포넌트에서 호출하면 실패한다. 또한 서버 컴포넌트에서의 `fetch`에는 Next.js의 캐싱/revalidation 설정이 없어 매 요청마다 API를 호출하게 된다.

**권장 수정**:
- 서버 컴포넌트용 별도 API 클라이언트를 만들어 Next.js `cookies()` API로 토큰을 읽도록 분리
- fetch에 `next: { revalidate: 60 }` 등의 캐시 옵션 추가 고려

---

### M5. clipboard 실패 시에도 성공 토스트 표시

**위치**: `frontend/src/app/[slug]/DocumentViewPage.tsx:29-31`

**현재 코드**:
```tsx
const handleCopyUrl = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href);
    showToast("URL이 복사되었습니다!");
  } catch {
    showToast("URL이 복사되었습니다!");  // 실패해도 동일 메시지
  }
};
```

**문제점**: Clipboard API가 실패했음에도(예: 비HTTPS 환경, 권한 미부여) 사용자에게 성공 메시지를 보여준다. 사용자는 복사되었다고 믿고 공유했으나 실제로는 클립보드에 아무것도 없을 수 있다.

**권장 수정**:
```tsx
catch {
  showToast("URL 복사에 실패했습니다. 직접 복사해주세요.");
}
```

---

### M6. dangerouslySetInnerHTML 사용

**위치**: `frontend/src/app/page.tsx:124`

**현재 코드**:
```tsx
function FeatureCard({ icon, title, children }) {
  return (
    <div>
      <span dangerouslySetInnerHTML={{ __html: icon }} />
    </div>
  );
}
```

**문제점**: `dangerouslySetInnerHTML`을 사용하고 있다. 현재 호출부에서 `icon`에 HTML 엔티티(`&#9889;` 등)만 전달하므로 당장 XSS 위험은 낮지만, 향후 `icon` 값이 동적으로 변경되면 취약점이 될 수 있다. 불필요한 공격 표면이다.

**권장 수정**:
- HTML 엔티티 대신 유니코드 문자를 직접 사용: `icon="⚡"`, `icon="👀"` 등
- 또는 `icon` prop을 `ReactNode` 타입으로 변경하여 JSX를 직접 전달

---

### m1. 매 키 입력마다 Blob 생성으로 바이트 카운트 계산

**위치**: `frontend/src/components/editor/EditorPanel.tsx:16-29`

**현재 코드**:
```tsx
function useByteCount(content: string) {
  const [byteCount, setByteCount] = useState("0 bytes");
  useEffect(() => {
    const bytes = new Blob([content]).size;
    ...
  }, [content]);
  return byteCount;
}
```

**문제점**: `content`가 변경될 때마다 (즉, 매 키 입력마다) Blob 객체가 생성된다. 대용량 문서에서 성능 저하가 발생할 수 있다.

**권장 수정**:
- `TextEncoder`를 사용하여 `new TextEncoder().encode(content).byteLength`로 변경 (Blob 생성 오버헤드 제거)
- 또는 debounce 적용

---

### m2. DocumentCard에서 URL 파싱 실패 가능성

**위치**: `frontend/src/components/document/DocumentCard.tsx:78`

**현재 코드**:
```tsx
<span className="font-mono text-xs text-accent opacity-70">
  {new URL(doc.url).host}/{doc.slug}
</span>
```

**문제점**: `doc.url`이 유효하지 않은 URL 문자열일 경우 `new URL()`이 예외를 throw하여 전체 컴포넌트가 crash한다.

**권장 수정**:
```tsx
const displayUrl = (() => {
  try { return `${new URL(doc.url).host}/${doc.slug}`; }
  catch { return doc.slug; }
})();
```

---

### m3. getCookie/deleteCookie 함수 중복 정의

**위치**: `frontend/src/lib/auth/token.ts`, `frontend/src/app/auth/callback/page.tsx:10-19`

**문제점**: 동일한 `getCookie`/`deleteCookie` 함수가 두 파일에 중복 구현되어 있다. 하나를 수정할 때 다른 하나를 놓치기 쉽다.

**권장 수정**: `token.ts`의 함수를 export하여 `callback/page.tsx`에서 import해서 사용

---

### m4. Login 페이지 OAuth 에러 시 에러 메시지 미표시

**위치**: `frontend/src/app/auth/login/page.tsx:36-38`

**현재 코드**:
```tsx
} catch {
  setIsLoading(false);
}
```

**문제점**: OAuth state 발급 실패 시 사용자에게 아무런 피드백 없이 로딩 상태만 해제된다. 네트워크 에러 등으로 실패하면 사용자는 왜 동작하지 않는지 알 수 없다.

**권장 수정**: `catch` 블록에서 에러 상태를 설정하고 UI에 에러 메시지를 표시

---

### m5. Auth store 새로고침 시 초기화

**위치**: `frontend/src/lib/store/useAuthStore.ts`

**문제점**: Zustand 스토어는 메모리에만 존재하므로, 페이지 새로고침 시 `isAuthenticated`가 `false`로 초기화된다. Header에서 `isAuthenticated`를 기반으로 UI를 분기하므로, 새로고침 직후 잠깐 비로그인 UI가 표시된 후 API 호출로 복구되는 깜빡임(flash)이 발생한다.

**권장 수정**:
- 쿠키 존재 여부를 기반으로 초기 `isAuthenticated` 값을 설정
- 또는 `zustand/middleware`의 `persist`를 사용하여 `localStorage`에 저장
- Header 컴포넌트에서 초기 로딩 상태를 고려한 스켈레톤 UI 적용

---

### m6. MarkdownViewer에서 렌더링 Promise 에러 미처리

**위치**: `frontend/src/components/viewer/MarkdownViewer.tsx:18`

**현재 코드**:
```tsx
useEffect(() => {
  renderMarkdown(content).then(setHtml);
}, [content]);
```

**문제점**: `renderMarkdown`이 실패할 경우 unhandled promise rejection이 발생한다.

**권장 수정**:
```tsx
useEffect(() => {
  renderMarkdown(content).then(setHtml).catch(() => setHtml('<p>렌더링 실패</p>'));
}, [content]);
```

---

### S1. 미리보기 Markdown 스타일 중복 제거

**위치**: `frontend/src/components/editor/PreviewPanel.tsx:37-56`, `frontend/src/components/viewer/MarkdownViewer.tsx:22-39`

**문제점**: 거의 동일한 Markdown 렌더링용 CSS가 두 파일에 하드코딩되어 있다. 스타일 변경 시 양쪽 모두 수정해야 한다.

**권장 수정**: 공통 유틸리티 함수로 추출 (예: `lib/markdown.ts`에 `wrapMarkdownHtml(html: string): string` 함수 추가)

---

### S2. 편집 페이지에서 unsaved changes 경고 없음

**위치**: `frontend/src/app/dashboard/[slug]/edit/page.tsx`

**문제점**: 문서를 수정 중 저장하지 않고 페이지를 떠나면 변경사항이 유실되지만 아무런 경고가 없다.

**권장 수정**: `beforeunload` 이벤트 리스너를 추가하여 미저장 변경사항이 있을 때 확인 대화상자를 표시

---

### S3. 토큰 쿠키에 Secure 플래그 미설정

**위치**: `frontend/src/lib/auth/token.ts:54-62`

**현재 코드**:
```ts
setCookie(COOKIE_ACCESS_TOKEN, accessToken, {
  maxAge: 3600,
  sameSite: "Lax",
  // secure 미설정 (기본값 false)
});
```

**문제점**: 프로덕션 환경(HTTPS)에서도 `Secure` 플래그 없이 전송되므로, 중간자 공격 시 쿠키가 HTTP로 전송될 수 있다.

**권장 수정**: 환경변수로 프로덕션 여부를 판단하여 `secure: process.env.NODE_ENV === 'production'` 설정

---

### S4. Dialog에 aria-labelledby/aria-describedby 미설정

**위치**: `frontend/src/components/ui/dialog.tsx`

**문제점**: `DialogContent`에 `role="dialog"`와 `aria-modal="true"`는 설정되어 있으나, `aria-labelledby`/`aria-describedby`가 없어 스크린 리더가 다이얼로그의 제목과 설명을 파악하기 어렵다.

**권장 수정**: `DialogTitle`과 `DialogDescription`에 고유 id를 부여하고 `DialogContent`에서 `aria-labelledby`, `aria-describedby`로 연결

---

## 잘된 점

1. **설계 문서와의 높은 일치도**: 페이지 구조, API 클라이언트 설계, 상태관리 구조가 설계 문서(`pages.md`, `client.md`)와 대체로 잘 일치한다.

2. **토큰 갱신 중복 방지**: `client.ts`의 `refreshPromise` 패턴으로 동시 다발적인 401 응답 시 토큰 갱신 요청이 중복되지 않도록 잘 처리되어 있다.

3. **Markdown sanitize 처리**: `rehype-sanitize`를 사용하여 Markdown 렌더링 시 XSS를 방지하고 있다.

4. **타입 안전성**: API 타입 정의(`types.ts`)가 체계적이며, 전반적으로 TypeScript를 잘 활용하고 있다.

5. **Dialog 포커스 트랩**: 커스텀 Dialog 컴포넌트에서 Tab 키 포커스 트랩과 ESC 키 핸들링, 이전 포커스 복원까지 직접 구현한 것은 접근성 면에서 좋다.

6. **에러 바운더리**: `error.tsx`에서 Next.js 16의 `unstable_retry`를 사용한 글로벌 에러 바운더리가 잘 구성되어 있다.

7. **OAuth state 검증**: 콜백 페이지에서 쿠키에 저장된 state와 URL의 state를 비교 검증하여 CSRF를 방지하고 있다.

8. **컴포넌트 분리**: 에디터, 뷰어, 문서 목록 등이 적절한 단위로 분리되어 있으며, UI 컴포넌트(`button`, `dialog`, `card` 등)도 재사용 가능하게 구성되어 있다.
