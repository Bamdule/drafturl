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
