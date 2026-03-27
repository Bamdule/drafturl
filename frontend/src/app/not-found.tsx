import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "페이지를 찾을 수 없습니다 - DraftURL",
  description: "요청하신 페이지가 존재하지 않거나 URL이 잘못되었습니다.",
};

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <div className="text-5xl mb-4">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="mx-auto text-text-muted">
            <circle cx="12" cy="12" r="10" />
            <path d="M8 15h8M9 9h.01M15 9h.01" />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-text-primary">
          페이지를 찾을 수 없습니다
        </h1>
        <p className="mt-4 text-text-secondary leading-relaxed">
          요청하신 페이지가 존재하지 않거나, URL이 잘못되었을 수 있습니다.
        </p>
        <div className="mt-6 flex items-center justify-center gap-3">
          <Link
            href="/"
            className="inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
          >
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  );
}
