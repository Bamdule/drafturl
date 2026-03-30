# Legal Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add legal pages (/terms, /privacy, /aup) that render Markdown files from docs/legal/ with i18n support, and wire up links from 4 locations.

**Architecture:** Each legal page is a Next.js Server Component that reads the corresponding `.ko.md` or `.en.md` file from `docs/legal/` at build time using `fs.readFileSync`, converts it to HTML via the existing remark/rehype pipeline, and renders it in a centered layout. The locale is obtained from the `x-locale` header (existing pattern from `layout.tsx`). Four link locations are updated: home footer, signup, login, and the publish button area.

**Tech Stack:** Next.js App Router (Server Components), unified/remark-parse/remark-gfm/remark-rehype/rehype-stringify (already in package.json), existing i18n system (DictProvider/useDict).

---

### Task 1: Rename existing md files to locale-suffixed names

**Files:**
- Rename: `docs/legal/terms-of-service.md` → `docs/legal/terms-of-service.ko.md`
- Rename: `docs/legal/privacy-policy.md` → `docs/legal/privacy-policy.ko.md`
- Rename: `docs/legal/acceptable-use-policy.md` → `docs/legal/acceptable-use-policy.ko.md`

- [ ] **Step 1: Rename files**

```bash
cd /Users/bam/my-ai-workspaces/drafturl
mv docs/legal/terms-of-service.md docs/legal/terms-of-service.ko.md
mv docs/legal/privacy-policy.md docs/legal/privacy-policy.ko.md
mv docs/legal/acceptable-use-policy.md docs/legal/acceptable-use-policy.ko.md
```

- [ ] **Step 2: Verify renamed files exist**

```bash
ls docs/legal/*.ko.md
```

Expected: 3 files listed.

- [ ] **Step 3: Commit**

```bash
git add docs/legal/
git commit -m "chore: rename legal md files with .ko locale suffix"
```

---

### Task 2: Create English translations of legal documents

**Files:**
- Create: `docs/legal/terms-of-service.en.md`
- Create: `docs/legal/privacy-policy.en.md`
- Create: `docs/legal/acceptable-use-policy.en.md`

- [ ] **Step 1: Create English Terms of Service**

Translate `docs/legal/terms-of-service.ko.md` into English. The document should:
- Keep the same structure (Article 1 through Article 17 + Addendum)
- Add translation notice at top: `> This is a translation provided for your convenience. In case of any discrepancy, the Korean version shall prevail.`
- Replace `> 최종 수정일: 2026-03-30` with `> Last updated: 2026-03-30`
- Replace all Korean headings with English equivalents (e.g. "제1조 (목적)" → "Article 1 (Purpose)")

- [ ] **Step 2: Create English Privacy Policy**

Translate `docs/legal/privacy-policy.ko.md` into English. Same conventions as Step 1.

- [ ] **Step 3: Create English Acceptable Use Policy**

Translate `docs/legal/acceptable-use-policy.ko.md` into English. Same conventions as Step 1.

- [ ] **Step 4: Verify all 6 files exist**

```bash
ls docs/legal/*.md
```

Expected: 6 files (3 ko + 3 en).

- [ ] **Step 5: Commit**

```bash
git add docs/legal/
git commit -m "feat: add English translations of legal documents"
```

---

### Task 3: Create Markdown rendering utility

**Files:**
- Create: `frontend/src/lib/legal/renderMarkdown.ts`

- [ ] **Step 1: Create the markdown renderer**

```typescript
// frontend/src/lib/legal/renderMarkdown.ts
import fs from "fs";
import path from "path";
import { unified } from "unified";
import remarkParse from "remark-parse";
import remarkGfm from "remark-gfm";
import remarkRehype from "remark-rehype";
import rehypeStringify from "rehype-stringify";
import type { Locale } from "@/dictionaries/types";

const LEGAL_DIR = path.join(process.cwd(), "..", "docs", "legal");

type LegalDoc = "terms-of-service" | "privacy-policy" | "acceptable-use-policy";

export async function renderLegalMarkdown(
  doc: LegalDoc,
  locale: Locale,
): Promise<string> {
  const filename = `${doc}.${locale}.md`;
  const filePath = path.join(LEGAL_DIR, filename);
  const markdown = fs.readFileSync(filePath, "utf-8");

  const result = await unified()
    .use(remarkParse)
    .use(remarkGfm)
    .use(remarkRehype, { allowDangerousHtml: true })
    .use(rehypeStringify, { allowDangerousHtml: true })
    .process(markdown);

  return String(result);
}
```

- [ ] **Step 2: Verify build compiles**

```bash
cd frontend && npx eslint src/lib/legal/renderMarkdown.ts
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/legal/
git commit -m "feat: add legal markdown rendering utility"
```

---

### Task 4: Create the /terms page

**Files:**
- Create: `frontend/src/app/terms/page.tsx`

- [ ] **Step 1: Create the terms page**

```tsx
// frontend/src/app/terms/page.tsx
import type { Metadata } from "next";
import { headers } from "next/headers";
import Link from "next/link";
import type { Locale } from "@/dictionaries/types";
import { renderLegalMarkdown } from "@/lib/legal/renderMarkdown";

export const metadata: Metadata = {
  title: "Terms of Service",
  description: "DraftURL Terms of Service",
};

export default async function TermsPage() {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;
  const html = await renderLegalMarkdown("terms-of-service", locale);

  return (
    <main className="min-h-screen bg-bg-primary">
      <div className="max-w-3xl mx-auto px-6 py-12">
        {locale === "en" && (
          <div className="mb-6 p-4 rounded-lg bg-bg-tertiary text-sm text-text-muted border border-border-dark">
            This is a translation provided for your convenience. In case of any
            discrepancy, the{" "}
            <Link href="/ko/terms" className="text-accent hover:underline">
              Korean version
            </Link>{" "}
            shall prevail.
          </div>
        )}
        <article
          className="prose prose-invert prose-sm max-w-none
            prose-headings:text-text-primary prose-p:text-text-secondary
            prose-a:text-accent prose-strong:text-text-primary
            prose-table:text-text-secondary prose-th:text-text-primary
            prose-td:border-border-dark prose-th:border-border-dark
            prose-hr:border-border-dark"
          dangerouslySetInnerHTML={{ __html: html }}
        />
        <nav className="mt-12 pt-6 border-t border-border-dark flex gap-4 text-sm text-text-muted">
          <Link href="/privacy" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "개인정보처리방침" : "Privacy Policy"}
          </Link>
          <Link href="/aup" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "이용제한정책" : "Acceptable Use Policy"}
          </Link>
        </nav>
      </div>
    </main>
  );
}
```

- [ ] **Step 2: Verify build**

```bash
cd frontend && npx eslint src/app/terms/page.tsx
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/terms/
git commit -m "feat: add /terms page rendering legal markdown"
```

---

### Task 5: Create the /privacy page

**Files:**
- Create: `frontend/src/app/privacy/page.tsx`

- [ ] **Step 1: Create the privacy page**

```tsx
// frontend/src/app/privacy/page.tsx
import type { Metadata } from "next";
import { headers } from "next/headers";
import Link from "next/link";
import type { Locale } from "@/dictionaries/types";
import { renderLegalMarkdown } from "@/lib/legal/renderMarkdown";

export const metadata: Metadata = {
  title: "Privacy Policy",
  description: "DraftURL Privacy Policy",
};

export default async function PrivacyPage() {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;
  const html = await renderLegalMarkdown("privacy-policy", locale);

  return (
    <main className="min-h-screen bg-bg-primary">
      <div className="max-w-3xl mx-auto px-6 py-12">
        {locale === "en" && (
          <div className="mb-6 p-4 rounded-lg bg-bg-tertiary text-sm text-text-muted border border-border-dark">
            This is a translation provided for your convenience. In case of any
            discrepancy, the{" "}
            <Link href="/ko/privacy" className="text-accent hover:underline">
              Korean version
            </Link>{" "}
            shall prevail.
          </div>
        )}
        <article
          className="prose prose-invert prose-sm max-w-none
            prose-headings:text-text-primary prose-p:text-text-secondary
            prose-a:text-accent prose-strong:text-text-primary
            prose-table:text-text-secondary prose-th:text-text-primary
            prose-td:border-border-dark prose-th:border-border-dark
            prose-hr:border-border-dark"
          dangerouslySetInnerHTML={{ __html: html }}
        />
        <nav className="mt-12 pt-6 border-t border-border-dark flex gap-4 text-sm text-text-muted">
          <Link href="/terms" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "이용약관" : "Terms of Service"}
          </Link>
          <Link href="/aup" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "이용제한정책" : "Acceptable Use Policy"}
          </Link>
        </nav>
      </div>
    </main>
  );
}
```

- [ ] **Step 2: Verify build**

```bash
cd frontend && npx eslint src/app/privacy/page.tsx
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/privacy/
git commit -m "feat: add /privacy page rendering legal markdown"
```

---

### Task 6: Create the /aup page

**Files:**
- Create: `frontend/src/app/aup/page.tsx`

- [ ] **Step 1: Create the AUP page**

```tsx
// frontend/src/app/aup/page.tsx
import type { Metadata } from "next";
import { headers } from "next/headers";
import Link from "next/link";
import type { Locale } from "@/dictionaries/types";
import { renderLegalMarkdown } from "@/lib/legal/renderMarkdown";

export const metadata: Metadata = {
  title: "Acceptable Use Policy",
  description: "DraftURL Acceptable Use Policy",
};

export default async function AupPage() {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;
  const html = await renderLegalMarkdown("acceptable-use-policy", locale);

  return (
    <main className="min-h-screen bg-bg-primary">
      <div className="max-w-3xl mx-auto px-6 py-12">
        {locale === "en" && (
          <div className="mb-6 p-4 rounded-lg bg-bg-tertiary text-sm text-text-muted border border-border-dark">
            This is a translation provided for your convenience. In case of any
            discrepancy, the{" "}
            <Link href="/ko/aup" className="text-accent hover:underline">
              Korean version
            </Link>{" "}
            shall prevail.
          </div>
        )}
        <article
          className="prose prose-invert prose-sm max-w-none
            prose-headings:text-text-primary prose-p:text-text-secondary
            prose-a:text-accent prose-strong:text-text-primary
            prose-table:text-text-secondary prose-th:text-text-primary
            prose-td:border-border-dark prose-th:border-border-dark
            prose-hr:border-border-dark"
          dangerouslySetInnerHTML={{ __html: html }}
        />
        <nav className="mt-12 pt-6 border-t border-border-dark flex gap-4 text-sm text-text-muted">
          <Link href="/terms" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "이용약관" : "Terms of Service"}
          </Link>
          <Link href="/privacy" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "개인정보처리방침" : "Privacy Policy"}
          </Link>
        </nav>
      </div>
    </main>
  );
}
```

- [ ] **Step 2: Verify build**

```bash
cd frontend && npx eslint src/app/aup/page.tsx
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/aup/
git commit -m "feat: add /aup page rendering legal markdown"
```

---

### Task 7: Update link locations (home footer, signup, login)

**Files:**
- Modify: `frontend/src/components/home/HomePage.tsx:197-198`
- Modify: `frontend/src/app/auth/signup/page.tsx:277-283`
- Modify: `frontend/src/app/auth/login/page.tsx:278-284`

- [ ] **Step 1: Update home footer links**

In `frontend/src/components/home/HomePage.tsx`, replace lines 197-198:

```tsx
// Before:
<a href="#" className="hover:text-text-muted transition-colors">{dict.home.footer.terms}</a>
<a href="#" className="hover:text-text-muted transition-colors">{dict.home.footer.privacy}</a>

// After:
<a href="/terms" className="hover:text-text-muted transition-colors">{dict.home.footer.terms}</a>
<a href="/privacy" className="hover:text-text-muted transition-colors">{dict.home.footer.privacy}</a>
```

- [ ] **Step 2: Update signup page links**

In `frontend/src/app/auth/signup/page.tsx`, replace the two `href="#"` in the agreement section (lines 277, 281):

```tsx
// Before:
<a href="#" className="hover:text-text-muted transition-colors">
  {dict.auth.signup.terms}
</a>
...
<a href="#" className="hover:text-text-muted transition-colors">
  {dict.auth.signup.privacy}
</a>

// After:
<a href="/terms" className="hover:text-text-muted transition-colors">
  {dict.auth.signup.terms}
</a>
...
<a href="/privacy" className="hover:text-text-muted transition-colors">
  {dict.auth.signup.privacy}
</a>
```

- [ ] **Step 3: Update login page links**

In `frontend/src/app/auth/login/page.tsx`, replace the two `href="#"` in the agreement section (lines 278, 282):

```tsx
// Before:
<a href="#" className="hover:text-text-muted transition-colors">
  {dict.auth.login.terms}
</a>
...
<a href="#" className="hover:text-text-muted transition-colors">
  {dict.auth.login.privacy}
</a>

// After:
<a href="/terms" className="hover:text-text-muted transition-colors">
  {dict.auth.login.terms}
</a>
...
<a href="/privacy" className="hover:text-text-muted transition-colors">
  {dict.auth.login.privacy}
</a>
```

- [ ] **Step 4: Verify no href="#" remains**

```bash
cd frontend && grep -rn 'href="#"' src/components/home/HomePage.tsx src/app/auth/signup/page.tsx src/app/auth/login/page.tsx
```

Expected: No matches.

- [ ] **Step 5: Verify eslint passes**

```bash
cd frontend && npx eslint src/components/home/HomePage.tsx src/app/auth/signup/page.tsx src/app/auth/login/page.tsx
```

Expected: No errors.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/home/HomePage.tsx frontend/src/app/auth/signup/page.tsx frontend/src/app/auth/login/page.tsx
git commit -m "feat: wire up legal page links in footer, signup, login"
```

---

### Task 8: Add terms agreement text to PublishButton

**Files:**
- Modify: `frontend/src/components/common/PublishButton.tsx:161-177`
- Modify: `frontend/src/dictionaries/ko.json`
- Modify: `frontend/src/dictionaries/en.json`

- [ ] **Step 1: Add dictionary keys**

In `frontend/src/dictionaries/ko.json`, add to the `"publish"` object:

```json
"termsAgreement": "공유 시 ",
"termsLink": "이용약관",
"termsAgreementSuffix": "에 동의합니다."
```

In `frontend/src/dictionaries/en.json`, add to the `"publish"` object:

```json
"termsAgreement": "By sharing, you agree to our ",
"termsLink": "Terms of Service",
"termsAgreementSuffix": "."
```

- [ ] **Step 2: Add agreement text below publish buttons**

In `frontend/src/components/common/PublishButton.tsx`, add the following right after the closing `</div>` of the buttons flex container (after line 177, before `</>`) inside the `<>...</>` fragment that contains the buttons:

```tsx
{/* Terms agreement */}
<p className="text-xs text-text-muted/60">
  {dict.publish.termsAgreement}
  <a href="/terms" className="hover:text-text-muted transition-colors underline">
    {dict.publish.termsLink}
  </a>
  {dict.publish.termsAgreementSuffix}
</p>
```

This should appear below the "미리보기 + 공유하기" buttons and above the expiry notice for non-authenticated users.

- [ ] **Step 3: Verify eslint passes**

```bash
cd frontend && npx eslint src/components/common/PublishButton.tsx
```

Expected: No errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/common/PublishButton.tsx frontend/src/dictionaries/ko.json frontend/src/dictionaries/en.json
git commit -m "feat: add terms agreement text to publish button area"
```

---

### Task 9: Update dictionary types (if needed) and final verification

**Files:**
- Possibly modify: `frontend/src/dictionaries/types.ts`

- [ ] **Step 1: Check if Dict type needs updating**

```bash
cd frontend && cat src/dictionaries/types.ts
```

If the type is inferred from the JSON (e.g. `typeof import("./ko.json")`), the new keys will be auto-included. If it's manually defined, add the new `publish.termsAgreement`, `publish.termsLink`, `publish.termsAgreementSuffix` fields.

- [ ] **Step 2: Run full eslint check**

```bash
cd frontend && npx eslint src/
```

Expected: No errors.

- [ ] **Step 3: Run build to verify pages render**

```bash
cd frontend && npx next build 2>&1 | head -50
```

Expected: Build succeeds. Routes `/terms`, `/privacy`, `/aup` appear in the build output.

- [ ] **Step 4: Commit any remaining changes**

If type updates were needed:

```bash
git add frontend/src/dictionaries/
git commit -m "chore: update dictionary types for legal page keys"
```
