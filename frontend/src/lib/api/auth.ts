import { apiFetch } from "./client";
import { ApiError } from "./types";
import type {
  OAuthStateResponse,
  UserWithStorage,
  EmailSignupRequest,
  EmailLoginRequest,
  User,
  ApiResponse,
} from "./types";

/** 백엔드에서 HMAC 서명된 OAuth2 state 값을 발급받는다 */
export function generateOAuthState(): Promise<OAuthStateResponse> {
  return apiFetch<OAuthStateResponse>("/api/v1/auth/oauth2/state");
}

/** OAuth2 콜백 처리: Next.js Route Handler를 통해 쿠키 설정 */
export async function oauthCallback(
  provider: string,
  data: { code: string; redirectUri: string; state: string },
): Promise<{ user: User }> {
  const res = await fetch("/api/auth/oauth-callback", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ provider, ...data }),
  });

  const json = (await res.json()) as ApiResponse<{ user: User }>;

  if (!json.success) {
    throw new ApiError(json.error.code, json.error.message, res.status);
  }

  return json.data;
}

/** 현재 로그인 사용자 정보 조회 (Next.js Route Handler 경유) */
export async function getMe(): Promise<UserWithStorage | null> {
  const res = await fetch("/api/auth/me");

  const json = (await res.json()) as ApiResponse<UserWithStorage | null>;

  if (!json.success) {
    throw new ApiError(json.error.code, json.error.message, res.status);
  }

  return json.data;
}

/** 로그아웃 (Next.js Route Handler 경유) */
export async function logout(): Promise<void> {
  await fetch("/api/auth/logout", {
    method: "POST",
  });
}

/** 이메일 회원가입 (Next.js Route Handler 경유) */
export async function emailSignup(
  data: EmailSignupRequest,
): Promise<{ user: User }> {
  const res = await fetch("/api/auth/signup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  const json = (await res.json()) as ApiResponse<{ user: User }>;

  if (!json.success) {
    throw new ApiError(json.error.code, json.error.message, res.status);
  }

  return json.data;
}

/** 이메일 로그인 (Next.js Route Handler 경유) */
export async function emailLogin(
  data: EmailLoginRequest,
): Promise<{ user: User }> {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  const json = (await res.json()) as ApiResponse<{ user: User }>;

  if (!json.success) {
    throw new ApiError(json.error.code, json.error.message, res.status);
  }

  return json.data;
}
