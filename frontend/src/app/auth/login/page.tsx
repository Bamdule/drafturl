"use client";

import { useCallback, useState } from "react";
import Link from "next/link";
import { generateOAuthState, emailLogin } from "@/lib/api/auth";
import { OAUTH_PROVIDERS, COOKIE_OAUTH_STATE, COOKIE_OAUTH_PROVIDER } from "@/lib/constants";
import { setCookie } from "@/lib/utils/cookie";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { ApiError } from "@/lib/api/types";
import type { OAuthProvider } from "@/lib/constants";
import Header from "@/components/layout/Header";

/** 각 프로바이더의 Client ID 환경변수 매핑 */
const OAUTH_CLIENT_IDS: Record<OAuthProvider, string | undefined> = {
  google: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID,
  github: process.env.NEXT_PUBLIC_GITHUB_CLIENT_ID,
  naver: process.env.NEXT_PUBLIC_NAVER_CLIENT_ID,
  kakao: process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID,
};

export default function LoginPage() {
  const { login } = useAuthStore();
  const [isOAuthLoading, setIsOAuthLoading] = useState(false);

  // Email login state
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoginLoading, setIsLoginLoading] = useState(false);

  const handleOAuthLogin = useCallback(
    async (provider: OAuthProvider) => {
      if (isOAuthLoading) return;
      setIsOAuthLoading(true);

      try {
        const { state } = await generateOAuthState();
        setCookie(COOKIE_OAUTH_STATE, state, { maxAge: 300, sameSite: "Lax" });
        setCookie(COOKIE_OAUTH_PROVIDER, provider, {
          maxAge: 300,
          sameSite: "Lax",
        });

        const config = OAUTH_PROVIDERS[provider];
        const clientId = OAUTH_CLIENT_IDS[provider];
        const redirectUri = `${window.location.origin}/auth/callback`;

        const params = new URLSearchParams({
          client_id: clientId ?? "",
          redirect_uri: redirectUri,
          response_type: "code",
          state,
        });

        if (config.scope) {
          params.set("scope", config.scope);
        }

        window.location.href = `${config.authUrl}?${params.toString()}`;
      } catch {
        setIsOAuthLoading(false);
      }
    },
    [isOAuthLoading],
  );

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
        window.umami?.track("login", { provider: "email" });
        window.location.href = "/dashboard";
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
    [email, password, isLoginLoading, login],
  );

  const isLoading = isOAuthLoading || isLoginLoading;

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      <Header />

      <main className="flex-1 flex items-center justify-center px-4 py-8">
        <div className="w-full max-w-[420px]">
          {/* Card */}
          <div className="bg-bg-secondary border border-border-dark rounded-2xl p-8 shadow-lg shadow-black/20">
            {/* Header */}
            <div className="text-center mb-6">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center mx-auto mb-4 text-xl font-bold text-white">
                D
              </div>
              <h1 className="text-xl font-bold text-text-primary">
                로그인
              </h1>
            </div>

            {/* Email Login Form */}
            <form
              onSubmit={handleEmailLogin}
              className="flex flex-col gap-3.5 text-left"
            >
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
                  className="flex h-10 w-full rounded-lg border border-border-dark bg-bg-primary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
                />
              </div>

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
                    className="flex h-10 w-full rounded-lg border border-border-dark bg-bg-primary px-3 pr-10 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
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

              {emailError && (
                <p className="text-xs text-danger">{emailError}</p>
              )}
              {serverError && (
                <div className="rounded-lg bg-danger/10 border border-danger/20 px-3 py-2.5 text-sm text-danger">
                  {serverError}
                </div>
              )}

              <button
                type="submit"
                disabled={isLoading}
                className="h-10 w-full rounded-lg bg-gradient-to-r from-accent to-[#6a48e8] text-white text-sm font-medium hover:opacity-90 transition-opacity disabled:opacity-70 disabled:pointer-events-none cursor-pointer mt-1"
              >
                {isLoginLoading ? "로그인 중..." : "로그인"}
              </button>
            </form>

            {/* Divider */}
            <div className="flex items-center gap-3 my-5 text-text-muted text-xs">
              <span className="flex-1 h-px bg-border-dark" />
              또는
              <span className="flex-1 h-px bg-border-dark" />
            </div>

            {/* Social OAuth Icons */}
            <div className="flex items-center justify-center gap-4">
              <OAuthButton
                provider="google"
                label="Google"
                onClick={() => handleOAuthLogin("google")}
                disabled={isLoading}
                unavailable={!OAUTH_CLIENT_IDS.google}
              >
                <GoogleIcon />
              </OAuthButton>
              <OAuthButton
                provider="github"
                label="GitHub"
                onClick={() => handleOAuthLogin("github")}
                disabled={isLoading}
                unavailable={!OAUTH_CLIENT_IDS.github}
              >
                <GitHubIcon />
              </OAuthButton>
              <OAuthButton
                provider="naver"
                label="네이버"
                onClick={() => handleOAuthLogin("naver")}
                disabled={isLoading}
                unavailable={!OAUTH_CLIENT_IDS.naver}
              >
                <NaverIcon />
              </OAuthButton>
              <OAuthButton
                provider="kakao"
                label="카카오"
                onClick={() => handleOAuthLogin("kakao")}
                disabled={isLoading}
                unavailable={!OAUTH_CLIENT_IDS.kakao}
              >
                <KakaoIcon />
              </OAuthButton>
            </div>
          </div>

          {/* Below card links */}
          <div className="text-center mt-5 space-y-3">
            <p className="text-sm text-text-muted">
              계정이 없으신가요?{" "}
              <Link
                href="/auth/signup"
                className="text-accent hover:underline font-medium"
              >
                회원가입
              </Link>
            </p>
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text-secondary transition-colors"
            >
              &larr; 로그인 없이 사용하기
            </Link>
          </div>

          {/* Footer */}
          <div className="text-center mt-6 text-xs text-text-muted/60">
            계속 진행하면{" "}
            <a href="#" className="hover:text-text-muted transition-colors">
              이용약관
            </a>{" "}
            및{" "}
            <a href="#" className="hover:text-text-muted transition-colors">
              개인정보 처리방침
            </a>
            에 동의하게 됩니다.
          </div>
        </div>
      </main>
    </div>
  );
}

/* ---------- Sub-components ---------- */

const PROVIDER_STYLES: Record<
  OAuthProvider,
  { border: string; hoverBg: string }
> = {
  google: {
    border: "border-gray-500/30",
    hoverBg: "hover:bg-white/10",
  },
  github: {
    border: "border-[#8b949e]/30",
    hoverBg: "hover:bg-[#8b949e]/15",
  },
  naver: {
    border: "border-[#03C75A]/30",
    hoverBg: "hover:bg-[#03C75A]/15",
  },
  kakao: {
    border: "border-[#FEE500]/30",
    hoverBg: "hover:bg-[#FEE500]/15",
  },
};

function OAuthButton({
  provider,
  label,
  onClick,
  disabled,
  unavailable,
  children,
}: {
  provider: OAuthProvider;
  label: string;
  onClick: () => void;
  disabled: boolean;
  unavailable?: boolean;
  children: React.ReactNode;
}) {
  const style = PROVIDER_STYLES[provider];
  return (
    <div className={`flex flex-col items-center gap-1.5 ${unavailable ? "opacity-30 pointer-events-none" : ""}`}>
      <button
        type="button"
        onClick={onClick}
        disabled={disabled || unavailable}
        aria-label={unavailable ? `${label} (준비 중)` : `${label}로 로그인`}
        title={unavailable ? "준비 중" : undefined}
        className={`w-12 h-12 rounded-full border ${style.border} ${unavailable ? "" : style.hoverBg} bg-bg-secondary flex items-center justify-center transition-colors disabled:pointer-events-none cursor-pointer`}
      >
        {children}
      </button>
      <span className="text-xs text-text-muted">{label}</span>
    </div>
  );
}

/* ---------- Icons ---------- */

function GoogleIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24">
      <path
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
        fill="#4285F4"
      />
      <path
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
        fill="#34A853"
      />
      <path
        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
        fill="#FBBC05"
      />
      <path
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
        fill="#EA4335"
      />
    </svg>
  );
}

function GitHubIcon() {
  return (
    <svg className="w-5 h-5" fill="#c9d1d9" viewBox="0 0 24 24">
      <path d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
    </svg>
  );
}

function NaverIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="#03C75A">
      <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z" />
    </svg>
  );
}

function KakaoIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="#FEE500">
      <path d="M12 3C6.477 3 2 6.463 2 10.691c0 2.726 1.8 5.117 4.508 6.473-.144.522-.926 3.361-.958 3.569 0 0-.02.166.088.229.108.063.234.014.234.014.308-.043 3.574-2.34 4.137-2.738.638.094 1.295.143 1.991.143 5.523 0 10-3.463 10-7.691S17.523 3 12 3z" />
    </svg>
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
