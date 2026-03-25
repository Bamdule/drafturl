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
        <h1 className="text-6xl font-bold text-text-primary">404</h1>
        <p className="mt-4 text-lg text-text-secondary">
          페이지를 찾을 수 없습니다.
        </p>
        <p className="mt-2 text-sm text-text-muted">
          요청하신 페이지가 존재하지 않거나, URL이 잘못되었을 수 있습니다.
        </p>
        <Link
          href="/"
          className="mt-6 inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
        >
          홈으로 돌아가기
        </Link>
      </div>
    </div>
  );
}
