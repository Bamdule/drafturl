"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { oauthCallback } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { useDict } from "@/components/i18n/DictProvider";
import { COOKIE_OAUTH_STATE, COOKIE_OAUTH_PROVIDER, OAUTH_PROVIDERS } from "@/lib/constants";
import type { OAuthProvider } from "@/lib/constants";
import { getCookie, deleteCookie } from "@/lib/utils/cookie";

function AuthCallbackContent() {
  const { dict } = useDict();
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
        setError(dict.auth.callback.invalidRequest);
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

        const validProviders = Object.keys(OAUTH_PROVIDERS);
        if (!provider || !validProviders.includes(provider)) {
          setError(dict.auth.callback.noProvider);
          return;
        }

        const result = await oauthCallback(provider as OAuthProvider, {
          code,
          redirectUri: `${window.location.origin}/auth/callback`,
          state,
        });

        login(result.user);
        window.umami?.track("login", { provider: provider as string });
        window.location.href = "/dashboard";
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : dict.auth.callback.error,
        );
      }
    }

    processCallback();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams, router, login]);

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
        <h1 className="text-xl font-bold text-danger">{dict.auth.callback.errorTitle}</h1>
        <p className="mt-2 text-sm text-text-secondary">{error}</p>
        <Link
          href="/auth/login"
          className="mt-4 text-sm font-medium text-accent hover:underline"
        >
          {dict.auth.callback.retry}
        </Link>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary">
      <div className="text-center">
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-border-dark border-t-accent" />
        <p className="mt-4 text-sm text-text-muted">{dict.auth.callback.loading}</p>
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  const { dict } = useDict();

  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-bg-primary">
          <div className="text-center">
            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-border-dark border-t-accent" />
            <p className="mt-4 text-sm text-text-muted">{dict.auth.callback.loading}</p>
          </div>
        </div>
      }
    >
      <AuthCallbackContent />
    </Suspense>
  );
}
