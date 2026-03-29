"use client";

import { useCallback, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { emailSignup } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { useDict } from "@/components/i18n/DictProvider";
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
  const { dict } = useDict();
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
      newErrors.name = dict.auth.signup.nameRequired;
    }

    if (!email.trim()) {
      newErrors.email = dict.auth.signup.emailRequired;
    } else if (!validateEmail(email)) {
      newErrors.email = dict.auth.signup.emailInvalid;
    }

    if (!password) {
      newErrors.password = dict.auth.signup.passwordRequired;
    } else if (!validatePassword(password)) {
      newErrors.password = dict.auth.signup.passwordInvalid;
    }

    if (!passwordConfirm) {
      newErrors.passwordConfirm = dict.auth.signup.confirmRequired;
    } else if (password !== passwordConfirm) {
      newErrors.passwordConfirm = dict.auth.signup.confirmMismatch;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [name, email, password, passwordConfirm, dict]);

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
        window.umami?.track("signup", { provider: "email" });
        router.replace("/dashboard");
      } catch (err) {
        if (err instanceof ApiError) {
          setServerError(err.message);
        } else {
          setServerError(dict.auth.signup.error);
        }
      } finally {
        setIsLoading(false);
      }
    },
    [validate, isLoading, email, password, name, login, router, dict],
  );

  const inputClass =
    "flex h-10 w-full rounded-lg border border-border-dark bg-bg-primary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors";

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      <Header />

      <main className="flex-1 flex items-center justify-center px-4 py-8">
        <div className="w-full max-w-[420px]">
          {/* Card */}
          <div className="bg-bg-secondary border border-border-dark rounded-2xl p-8 shadow-lg shadow-black/20">
            {/* Header */}
            <div className="text-center mb-5">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center mx-auto mb-4 text-xl font-bold text-white">
                D
              </div>
              <h1 className="text-xl font-bold text-text-primary">
                {dict.auth.signup.title}
              </h1>
            </div>

            {/* Signup Form */}
            <form onSubmit={handleSubmit} className="flex flex-col gap-3 text-left">
              <div>
                <label
                  htmlFor="name"
                  className="block text-sm font-medium text-text-secondary mb-1.5"
                >
                  {dict.auth.signup.name}
                </label>
                <input
                  id="name"
                  type="text"
                  value={name}
                  onChange={(e) => {
                    setName(e.target.value);
                    if (errors.name) setErrors((prev) => ({ ...prev, name: undefined }));
                  }}
                  placeholder={dict.auth.signup.namePlaceholder}
                  className={inputClass}
                />
                {errors.name && (
                  <p className="mt-1 text-xs text-danger">{errors.name}</p>
                )}
              </div>

              <div>
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-text-secondary mb-1.5"
                >
                  {dict.auth.signup.email}
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
                  className={inputClass}
                />
                {errors.email && (
                  <p className="mt-1 text-xs text-danger">{errors.email}</p>
                )}
              </div>

              <div>
                <label
                  htmlFor="password"
                  className="block text-sm font-medium text-text-secondary mb-1.5"
                >
                  {dict.auth.signup.password}
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
                    placeholder={dict.auth.signup.passwordPlaceholder}
                    className={`${inputClass} pr-10`}
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

              <div>
                <label
                  htmlFor="passwordConfirm"
                  className="block text-sm font-medium text-text-secondary mb-1.5"
                >
                  {dict.auth.signup.passwordConfirm}
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
                    placeholder={dict.auth.signup.passwordConfirmPlaceholder}
                    className={`${inputClass} pr-10`}
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
                {isLoading ? dict.auth.signup.submitting : dict.auth.signup.submit}
              </button>
            </form>
          </div>

          {/* Below card links */}
          <div className="text-center mt-5">
            <p className="text-sm text-text-muted">
              {dict.auth.signup.hasAccount}{" "}
              <Link
                href="/auth/login"
                className="text-accent hover:underline font-medium"
              >
                {dict.auth.signup.login}
              </Link>
            </p>
          </div>

          {/* Footer */}
          <div className="text-center mt-6 text-xs text-text-muted/60">
            {dict.auth.signup.agreement}{" "}
            <a href="#" className="hover:text-text-muted transition-colors">
              {dict.auth.signup.terms}
            </a>{" "}
            &amp;{" "}
            <a href="#" className="hover:text-text-muted transition-colors">
              {dict.auth.signup.privacy}
            </a>
            {dict.auth.signup.agreementSuffix}
          </div>
        </div>
      </main>
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
