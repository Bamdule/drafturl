import { ApiError, type ApiResponse } from "./types";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** 토큰 갱신 중복 방지 */
let refreshPromise: Promise<boolean> | null = null;

async function tryRefreshToken(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const res = await fetch("/api/auth/refresh", {
        method: "POST",
      });
      if (!res.ok) return false;

      const json = (await res.json()) as ApiResponse<null>;
      return json.success;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

interface FetchOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  auth?: boolean;
}

/**
 * API fetch 래퍼.
 * - auth: true 인 경우 Next.js 프록시를 경유하여 httpOnly 쿠키로 인증
 * - auth: false 인 경우 백엔드에 직접 호출 (인증 불필요 API)
 * - 401 시 토큰 자동 갱신 후 재시도
 */
export async function apiFetch<T>(
  path: string,
  options: FetchOptions = {},
): Promise<T> {
  const { body, auth = false, headers: customHeaders, ...rest } = options;

  const headers = new Headers(customHeaders);
  if (!headers.has("Content-Type") && body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  const fetchOptions: RequestInit = {
    ...rest,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  };

  // auth가 true이면 프록시 경유, false이면 직접 백엔드 호출
  const url = auth ? `/api/proxy${path.replace(/^\/api\/v1/, "")}` : `${BASE_URL}${path}`;

  let res = await fetch(url, fetchOptions);

  // 401 시 토큰 갱신 후 1회 재시도
  if (res.status === 401 && auth) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      res = await fetch(url, fetchOptions);
    } else {
      if (typeof window !== "undefined") {
        window.location.href = "/auth/login";
      }
      throw new ApiError("UNAUTHORIZED", "인증이 만료되었습니다.", 401);
    }
  }

  const json = (await res.json()) as ApiResponse<T>;

  if (!json.success) {
    throw new ApiError(json.error.code, json.error.message, res.status);
  }

  return json.data;
}
