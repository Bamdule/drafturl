import type { Metadata } from "next";
import { headers } from "next/headers";
import Link from "next/link";
import type { Locale } from "@/dictionaries/types";
import { renderGuideHtml } from "@/lib/guide/renderGuide";
import "../guide.css";

export const metadata: Metadata = {
  title: "MCP Connection Guide — DraftURL",
  description:
    "Connect Claude Desktop, Claude Code, or Cursor to DraftURL via MCP (Model Context Protocol).",
};

export default async function McpGuidePage() {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;
  const html = renderGuideHtml("mcp", locale);

  return (
    <main className="min-h-screen bg-bg-primary">
      <div className="max-w-3xl mx-auto px-6 py-12">
        <nav className="mb-6 text-sm text-text-muted">
          <Link href="/guide" className="hover:text-text-primary transition-colors">
            {locale === "ko" ? "가이드" : "Guide"}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-text-secondary">
            {locale === "ko" ? "MCP 연결" : "MCP Connection"}
          </span>
        </nav>

        <article
          className="guide-article"
          dangerouslySetInnerHTML={{ __html: html }}
        />

        <nav className="mt-12 pt-6 border-t border-border-dark flex gap-4 text-sm text-text-muted">
          <Link
            href="/guide"
            className="hover:text-text-primary transition-colors"
          >
            {locale === "ko" ? "← 가이드 목록" : "← All Guides"}
          </Link>
        </nav>
      </div>
    </main>
  );
}
