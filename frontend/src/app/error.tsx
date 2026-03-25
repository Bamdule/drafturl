"use client";

import { useEffect } from "react";

/**
 * Next.js 16 global error boundary.
 * reset -> unstable_retry
 */
export default function Error({
  error,
  unstable_retry,
}: {
  error: Error & { digest?: string };
  unstable_retry: () => void;
}) {
  useEffect(() => {
    console.error("Unhandled error:", error);
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <h1 className="text-2xl font-bold text-text-primary">
          오류가 발생했습니다
        </h1>
        <p className="mt-4 text-text-secondary">
          예기치 않은 오류가 발생했습니다. 잠시 후 다시 시도해주세요.
        </p>
        {error.digest && (
          <p className="mt-2 text-xs text-text-muted">
            오류 코드: {error.digest}
          </p>
        )}
        <div className="mt-6 flex justify-center gap-3">
          <button
            onClick={() => unstable_retry()}
            className="px-4 py-2 rounded-md text-sm font-semibold text-white bg-accent hover:bg-accent-hover transition-colors cursor-pointer"
          >
            다시 시도
          </button>
          <button
            onClick={() => (window.location.href = "/")}
            className="px-4 py-2 rounded-md text-sm font-medium text-text-primary border border-border-dark hover:bg-bg-tertiary transition-colors cursor-pointer"
          >
            홈으로 돌아가기
          </button>
        </div>
      </div>
    </div>
  );
}
