import type { Metadata } from "next";
import { headers } from "next/headers";
import Link from "next/link";
import type { Locale } from "@/dictionaries/types";

export const metadata: Metadata = {
  title: "Guide — DraftURL",
  description: "DraftURL usage guides and tutorials",
};

const content = {
  en: {
    heading: "Guide",
    subtitle: "Learn how to use DraftURL effectively.",
    guides: [
      {
        href: "/guide/mcp",
        title: "MCP Connection",
        description:
          "Connect Claude Desktop, Claude Code, or Cursor to DraftURL via MCP.",
      },
    ],
  },
  ko: {
    heading: "가이드",
    subtitle: "DraftURL 사용 방법을 안내합니다.",
    guides: [
      {
        href: "/guide/mcp",
        title: "MCP 연결",
        description:
          "Claude Desktop, Claude Code, Cursor에서 MCP로 DraftURL에 연결하는 방법.",
      },
    ],
  },
};

/* MCP 아이콘 — 플러그/연결 */
const guideIcons = [
  <svg key="plug" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22v-5" /><path d="M9 8V2" /><path d="M15 8V2" /><path d="M18 8v5a6 6 0 0 1-12 0V8z" /></svg>,
];

export default async function GuidePage() {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;
  const t = content[locale];

  return (
    <main className="min-h-screen bg-bg-primary">
      <div className="max-w-3xl mx-auto px-6 py-12">
        <h1 className="text-2xl font-bold text-text-primary mb-2">
          {t.heading}
        </h1>
        <p className="text-sm text-text-secondary mb-8">{t.subtitle}</p>

        <div className="grid gap-4">
          {t.guides.map((guide, index) => (
            <Link
              key={guide.href}
              href={guide.href}
              className="flex items-start gap-4 p-5 bg-bg-secondary border border-border-dark rounded-xl hover:border-border-dark-hover transition-colors"
            >
              <div className="w-10 h-10 rounded-lg bg-accent/15 text-accent flex items-center justify-center shrink-0">
                {guideIcons[index]}
              </div>
              <div>
                <h2 className="text-[15px] font-semibold text-text-primary mb-1">
                  {guide.title}
                </h2>
                <p className="text-[13px] text-text-secondary leading-relaxed">
                  {guide.description}
                </p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </main>
  );
}
