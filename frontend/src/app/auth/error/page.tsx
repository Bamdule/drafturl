import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "인증 오류 - DraftURL",
  description: "인증 과정에서 문제가 발생했습니다.",
  robots: "noindex, nofollow",
};

export default function AuthErrorPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <h1 className="text-2xl font-bold text-text-primary">인증 오류</h1>
        <p className="mt-4 text-text-secondary">
          인증 과정에서 문제가 발생했습니다. 보안을 위해 로그인을 다시
          시도해주세요.
        </p>
        <p className="mt-2 text-sm text-text-muted">
          문제가 계속되면 브라우저 쿠키를 삭제한 후 다시 시도하거나, 다른
          로그인 방법을 사용해주세요.
        </p>
        <Link
          href="/auth/login"
          className="mt-6 inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
        >
          다시 로그인하기
        </Link>
      </div>
    </div>
  );
}
