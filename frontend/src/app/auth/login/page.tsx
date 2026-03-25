"use client";

import { useCallback, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { generateOAuthState, emailLogin } from "@/lib/api/auth";
import { OAUTH_PROVIDERS, COOKIE_OAUTH_STATE, COOKIE_OAUTH_PROVIDER } from "@/lib/constants";
import { setCookie } from "@/lib/utils/cookie";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { ApiError } from "@/lib/api/types";
import type { OAuthProvider } from "@/lib/constants";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();
  const [isOAuthLoading, setIsOAuthLoading] = useState(false);

  // Email login state
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoginLoading, setIsLoginLoading] = useState(false);

  const handleOAuthLogin = useCallback(async (provider: OAuthProvider) => {
    if (isOAuthLoading) return;
    setIsOAuthLoading(true);

    try {
      const { state } = await generateOAuthState();
      setCookie(COOKIE_OAUTH_STATE, state, { maxAge: 300, sameSite: "Lax" });
      setCookie(COOKIE_OAUTH_PROVIDER, provider, { maxAge: 300, sameSite: "Lax" });

      const config = OAUTH_PROVIDERS[provider];
      const clientId =
        provider === "google"
          ? process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
          : process.env.NEXT_PUBLIC_GITHUB_CLIENT_ID;
      const redirectUri = `${window.location.origin}/auth/callback`;

      const params = new URLSearchParams({
        client_id: clientId ?? "",
        redirect_uri: redirectUri,
        response_type: "code",
        scope: config.scope,
        state,
      });

      window.location.href = `${config.authUrl}?${params.toString()}`;
    } catch {
      setIsOAuthLoading(false);
    }
  }, [isOAuthLoading]);

  const handleEmailLogin = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setServerError(null);
      setEmailError(null);

      if (!email.trim()) {
        setEmailError("이메일을 입력해주세요.");
        return;
      }
      if (!password) {
        setEmailError("비밀번호를 입력해주세요.");
        return;
      }

      if (isLoginLoading) return;
      setIsLoginLoading(true);

      try {
        const result = await emailLogin({
          email: email.trim(),
          password,
        });

        login(result.user);
        router.replace("/dashboard");
      } catch (err) {
        if (err instanceof ApiError) {
          setServerError(err.message);
        } else {
          setServerError("로그인 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
      } finally {
        setIsLoginLoading(false);
      }
    },
    [email, password, isLoginLoading, login, router],
  );

  const isLoading = isOAuthLoading || isLoginLoading;

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      {/* Header - centered logo */}
      <header className="flex items-center justify-center px-6 h-14 border-b border-border-dark">
        <Link
          href="/"
          className="flex items-center gap-2 text-lg font-bold text-text-primary no-underline"
        >
          <div className="w-7 h-7 bg-gradient-to-br from-accent to-[#a78bfa] rounded-lg flex items-center justify-center text-sm text-white font-bold">
            D
          </div>
          DraftURL
        </Link>
      </header>

      {/* Main */}
      <main className="flex-1 flex items-center justify-center px-6">
        <div className="w-full max-w-[400px] text-center">
          {/* Login Icon */}
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center mx-auto mb-6 text-3xl text-white shadow-[0_8px_32px_rgba(124,92,252,0.25)]">
            D
          </div>

          <h1 className="text-2xl font-bold text-text-primary mb-2">
            DraftURL에 로그인
          </h1>
          <p className="text-[15px] text-text-secondary mb-8">
            문서를 영구 보관하고, 대시보드에서 관리하세요.
          </p>

          {/* Email Login Form */}
          <form onSubmit={handleEmailLogin} className="flex flex-col gap-4 text-left mb-6">
            {/* Email */}
            <div>
              <label
                htmlFor="email"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                이메일
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setEmailError(null);
                }}
                placeholder="you@example.com"
                className="flex h-11 w-full rounded-lg border border-border-dark bg-bg-secondary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
              />
            </div>

            {/* Password */}
            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                비밀번호
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setEmailError(null);
                  }}
                  placeholder="비밀번호를 입력하세요"
                  className="flex h-11 w-full rounded-lg border border-border-dark bg-bg-secondary px-3 pr-10 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            {/* Errors */}
            {emailError && (
              <p className="text-xs text-danger">{emailError}</p>
            )}
            {serverError && (
              <div className="rounded-lg bg-danger/10 border border-danger/20 px-4 py-3 text-sm text-danger">
                {serverError}
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="h-11 w-full rounded-lg bg-gradient-to-r from-accent to-[#6a48e8] text-white text-[15px] font-medium hover:opacity-90 transition-opacity disabled:opacity-70 disabled:pointer-events-none cursor-pointer"
            >
              {isLoginLoading ? "로그인 중..." : "이메일로 로그인"}
            </button>
          </form>

          {/* Signup Link */}
          <p className="text-sm text-text-muted mb-6">
            계정이 없으신가요?{" "}
            <Link
              href="/auth/signup"
              className="text-accent hover:underline font-medium"
            >
              회원가입
            </Link>
          </p>

          {/* Benefits */}
          <div className="flex items-center gap-3 mb-6 text-text-muted text-xs">
            <span className="flex-1 h-px bg-border-dark" />
            가입하면 이런 기능을 사용할 수 있어요
            <span className="flex-1 h-px bg-border-dark" />
          </div>

          <div className="bg-bg-secondary border border-border-dark rounded-xl p-5 text-left">
            <BenefitItem>문서 영구 보관 (만료 없음)</BenefitItem>
            <BenefitItem>대시보드에서 문서 관리</BenefitItem>
            <BenefitItem>문서 수정 및 재배포</BenefitItem>
            <BenefitItem>AI 수정 기능 (Coming Soon)</BenefitItem>
          </div>

          <Link
            href="/"
            className="inline-flex items-center gap-1 mt-6 text-sm text-text-muted hover:text-text-secondary transition-colors"
          >
            &larr; 로그인 없이 사용하기
          </Link>
        </div>
      </main>

      {/* Footer */}
      <div className="text-center py-5 px-6 text-xs text-text-muted">
        계속 진행하면{" "}
        <a href="#" className="text-text-muted hover:text-text-secondary">
          서비스 이용약관
        </a>{" "}
        및{" "}
        <a href="#" className="text-text-muted hover:text-text-secondary">
          개인정보 처리방침
        </a>
        에 동의하게 됩니다.
      </div>
    </div>
  );
}

function BenefitItem({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-2.5 py-1.5 text-sm text-text-secondary">
      <span className="w-[18px] h-[18px] rounded-full bg-success/10 flex items-center justify-center text-[10px] text-success shrink-0">
        &#10003;
      </span>
      <span>{children}</span>
    </div>
  );
}

function EyeIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
      />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3.98 8.223A10.477 10.477 0 001.934 12.07a1.012 1.012 0 000 .639C3.423 16.49 7.36 19.5 12 19.5c1.658 0 3.222-.394 4.601-1.092M6.228 6.228A10.45 10.45 0 0112 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639a10.496 10.496 0 01-3.752 5.014M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 01-4.243-4.243m4.242 4.242L9.88 9.88"
      />
    </svg>
  );
}
