/**
 * 서버 컴포넌트 전용 API 유틸리티.
 *
 * 클라이언트 전용 `apiFetch`(document.cookie 기반)와 달리,
 * 서버 환경에서 안전하게 사용할 수 있는 fetch 래퍼를 제공한다.
 * 인증이 필요한 경우 Next.js의 `cookies()` API를 사용한다.
 */

import { ApiError, type ApiResponse } from "./types";

// 서버 컴포넌트에서는 Docker 내부 네트워크 URL을 사용 (INTERNAL_API_URL)
// 클라이언트에서는 브라우저가 접근 가능한 URL을 사용 (NEXT_PUBLIC_API_URL)
const BASE_URL =
  process.env.INTERNAL_API_URL ??
  process.env.NEXT_PUBLIC_API_URL ??
  "http://localhost:8080";

/**
 * 서버 컴포넌트에서 사용하는 API fetch.
 * - 인증 불필요한 공개 API 호출에 사용
 * - 인증이 필요한 경우 `authHeaders`를 전달
 */
export async function serverFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  const json = (await res.json()) as ApiResponse<T>;

  if (!json.success) {
    throw new ApiError(json.error.code, json.error.message, res.status);
  }

  return json.data;
}
