import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "페이지를 찾을 수 없습니다 - DraftURL",
  description: "요청하신 페이지가 존재하지 않거나 URL이 잘못되었습니다.",
};

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b border-border-dark bg-bg-primary/80 backdrop-blur-md">
        <div className="flex items-center h-14 px-4 md:px-6 max-w-screen-xl mx-auto">
          <Link href="/" className="flex items-center gap-2 text-text-primary font-semibold text-[15px] hover:opacity-80 transition-opacity">
            <span className="w-7 h-7 rounded-lg bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center text-xs font-bold text-white">
              D
            </span>
            DraftURL
          </Link>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 flex items-center justify-center px-4">
        <div className="max-w-sm text-center">
          <div className="w-16 h-16 rounded-2xl bg-bg-secondary border border-border-dark flex items-center justify-center mx-auto mb-5">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="text-text-muted">
              <circle cx="12" cy="12" r="10" />
              <path d="M8 15h8M9 9h.01M15 9h.01" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-text-primary mb-2">
            페이지를 찾을 수 없습니다
          </h1>
          <p className="text-sm text-text-secondary mb-6">
            요청하신 페이지가 존재하지 않거나, URL이 잘못되었을 수 있습니다.
          </p>
          <Link
            href="/"
            className="inline-block rounded-lg bg-accent px-5 py-2.5 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
          >
            홈으로 돌아가기
          </Link>
        </div>
      </main>
    </div>
  );
}
