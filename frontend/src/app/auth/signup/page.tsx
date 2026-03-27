"use client";

import { useCallback, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { emailSignup } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { ApiError } from "@/lib/api/types";
import Header from "@/components/layout/Header";

interface FormErrors {
  name?: string;
  email?: string;
  password?: string;
  passwordConfirm?: string;
}

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function validatePassword(password: string): boolean {
  return password.length >= 8 && /[a-zA-Z]/.test(password) && /\d/.test(password);
}

export default function SignupPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const validate = useCallback((): boolean => {
    const newErrors: FormErrors = {};

    if (!name.trim()) {
      newErrors.name = "이름을 입력해주세요.";
    }

    if (!email.trim()) {
      newErrors.email = "이메일을 입력해주세요.";
    } else if (!validateEmail(email)) {
      newErrors.email = "올바른 이메일 형식이 아닙니다.";
    }

    if (!password) {
      newErrors.password = "비밀번호를 입력해주세요.";
    } else if (!validatePassword(password)) {
      newErrors.password = "비밀번호는 최소 8자, 영문과 숫자를 포함해야 합니다.";
    }

    if (!passwordConfirm) {
      newErrors.passwordConfirm = "비밀번호 확인을 입력해주세요.";
    } else if (password !== passwordConfirm) {
      newErrors.passwordConfirm = "비밀번호가 일치하지 않습니다.";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [name, email, password, passwordConfirm]);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setServerError(null);

      if (!validate()) return;
      if (isLoading) return;

      setIsLoading(true);
      try {
        const result = await emailSignup({
          email: email.trim(),
          password,
          name: name.trim(),
        });

        login(result.user);
        router.replace("/dashboard");
      } catch (err) {
        if (err instanceof ApiError) {
          setServerError(err.message);
        } else {
          setServerError("회원가입 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
      } finally {
        setIsLoading(false);
      }
    },
    [validate, isLoading, email, password, name, login, router],
  );

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      <Header />

      {/* Main */}
      <main className="flex-1 flex items-center justify-center px-6">
        <div className="w-full max-w-[400px] text-center">
          {/* Icon */}
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center mx-auto mb-6 text-3xl text-white shadow-[0_8px_32px_rgba(124,92,252,0.25)]">
            D
          </div>

          <h1 className="text-2xl font-bold text-text-primary mb-2">
            DraftURL 회원가입
          </h1>
          <p className="text-[15px] text-text-secondary mb-8">
            계정을 만들고 문서를 영구 보관하세요.
          </p>

          {/* Signup Form */}
          <form onSubmit={handleSubmit} className="flex flex-col gap-4 text-left">
            {/* Name */}
            <div>
              <label
                htmlFor="name"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                이름
              </label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (errors.name) setErrors((prev) => ({ ...prev, name: undefined }));
                }}
                placeholder="홍길동"
                className="flex h-11 w-full rounded-lg border border-border-dark bg-bg-secondary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
              />
              {errors.name && (
                <p className="mt-1 text-xs text-danger">{errors.name}</p>
              )}
            </div>

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
                  if (errors.email) setErrors((prev) => ({ ...prev, email: undefined }));
                }}
                placeholder="you@example.com"
                className="flex h-11 w-full rounded-lg border border-border-dark bg-bg-secondary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
              />
              {errors.email && (
                <p className="mt-1 text-xs text-danger">{errors.email}</p>
              )}
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
                    if (errors.password)
                      setErrors((prev) => ({ ...prev, password: undefined }));
                  }}
                  placeholder="영문 + 숫자 포함 8자 이상"
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
              {errors.password && (
                <p className="mt-1 text-xs text-danger">{errors.password}</p>
              )}
            </div>

            {/* Password Confirm */}
            <div>
              <label
                htmlFor="passwordConfirm"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                비밀번호 확인
              </label>
              <div className="relative">
                <input
                  id="passwordConfirm"
                  type={showPasswordConfirm ? "text" : "password"}
                  value={passwordConfirm}
                  onChange={(e) => {
                    setPasswordConfirm(e.target.value);
                    if (errors.passwordConfirm)
                      setErrors((prev) => ({
                        ...prev,
                        passwordConfirm: undefined,
                      }));
                  }}
                  placeholder="비밀번호를 다시 입력해주세요"
                  className="flex h-11 w-full rounded-lg border border-border-dark bg-bg-secondary px-3 pr-10 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPasswordConfirm((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
                  tabIndex={-1}
                >
                  {showPasswordConfirm ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
              {errors.passwordConfirm && (
                <p className="mt-1 text-xs text-danger">
                  {errors.passwordConfirm}
                </p>
              )}
            </div>

            {/* Server Error */}
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
              {isLoading ? "가입 중..." : "회원가입"}
            </button>
          </form>

          {/* Login Link */}
          <p className="mt-6 text-sm text-text-muted">
            이미 계정이 있으신가요?{" "}
            <Link
              href="/auth/login"
              className="text-accent hover:underline font-medium"
            >
              로그인
            </Link>
          </p>
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
