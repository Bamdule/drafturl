import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "문서 만료 - DraftURL",
  description: "이 문서는 만료 기간이 지나 더 이상 볼 수 없습니다.",
  robots: "noindex, nofollow",
};

interface ExpiredPageProps {
  searchParams: Promise<{ slug?: string }>;
}

export default async function ExpiredPage({
  searchParams,
}: ExpiredPageProps) {
  const { slug } = await searchParams;

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <h1 className="text-4xl font-bold text-text-primary">
          문서가 만료되었습니다
        </h1>
        <p className="mt-4 text-text-secondary">
          이 문서는 만료 기간이 지나 더 이상 볼 수 없습니다.
        </p>
        {slug && (
          <p className="mt-2 text-sm text-text-muted">문서 ID: {slug}</p>
        )}
        <p className="mt-4 text-sm text-text-secondary">
          로그인하면 문서를 영구 보관할 수 있습니다.
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Link
            href="/"
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
          >
            새 문서 만들기
          </Link>
          <Link
            href="/auth/login"
            className="rounded-md border border-border-dark px-4 py-2 text-sm font-medium text-text-primary hover:bg-bg-tertiary transition-colors"
          >
            로그인
          </Link>
        </div>
      </div>
    </div>
  );
}
