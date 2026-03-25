"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { oauthCallback } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { COOKIE_OAUTH_STATE, COOKIE_OAUTH_PROVIDER } from "@/lib/constants";
import { getCookie, deleteCookie } from "@/lib/utils/cookie";

function AuthCallbackContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { login } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) return;
    processedRef.current = true;

    async function processCallback() {
      const code = searchParams.get("code");
      const state = searchParams.get("state");

      if (!code || !state) {
        setError("잘못된 콜백 요청입니다.");
        return;
      }

      const savedState = getCookie(COOKIE_OAUTH_STATE);
      if (!savedState || savedState !== state) {
        deleteCookie(COOKIE_OAUTH_STATE);
        deleteCookie(COOKIE_OAUTH_PROVIDER);
        router.replace("/auth/error");
        return;
      }

      deleteCookie(COOKIE_OAUTH_STATE);

      try {
        const provider = getCookie(COOKIE_OAUTH_PROVIDER);
        deleteCookie(COOKIE_OAUTH_PROVIDER);

        if (provider !== "google" && provider !== "github") {
          setError("OAuth provider 정보를 확인할 수 없습니다.");
          return;
        }

        const result = await oauthCallback(provider, {
          code,
          redirectUri: `${window.location.origin}/auth/callback`,
          state,
        });

        login(result.user);
        router.replace("/dashboard");
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "인증 처리 중 오류가 발생했습니다.",
        );
      }
    }

    processCallback();
  }, [searchParams, router, login]);

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
        <h1 className="text-xl font-bold text-danger">인증 오류</h1>
        <p className="mt-2 text-sm text-text-secondary">{error}</p>
        <a
          href="/auth/login"
          className="mt-4 text-sm font-medium text-accent hover:underline"
        >
          다시 로그인하기
        </a>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary">
      <div className="text-center">
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-border-dark border-t-accent" />
        <p className="mt-4 text-sm text-text-muted">로그인 처리 중...</p>
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-bg-primary">
          <div className="text-center">
            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-border-dark border-t-accent" />
            <p className="mt-4 text-sm text-text-muted">로그인 처리 중...</p>
          </div>
        </div>
      }
    >
      <AuthCallbackContent />
    </Suspense>
  );
}
