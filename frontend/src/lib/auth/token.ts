/**
 * JWT 토큰은 이제 httpOnly 쿠키로 관리된다.
 * 클라이언트 JavaScript에서 직접 토큰에 접근할 수 없다.
 *
 * 인증 관련 모든 작업은 Next.js Route Handler를 통해 수행된다:
 * - 로그인/회원가입: /api/auth/login, /api/auth/signup
 * - 로그아웃: /api/auth/logout
 * - 사용자 정보 조회: /api/auth/me
 * - API 프록시: /api/proxy/[...path]
 *
 * 이 파일의 함수들은 하위 호환을 위해 유지하되, 모두 no-op이다.
 */

import {
  COOKIE_ACCESS_TOKEN,
} from "@/lib/constants";

/**
 * @deprecated httpOnly 쿠키는 클라이언트에서 접근 불가.
 * 인증 상태 확인은 /api/auth/me를 사용하라.
 */
export function getAccessToken(): string | null {
  return null;
}

/**
 * @deprecated httpOnly 쿠키는 클라이언트에서 접근 불가.
 */
export function getRefreshToken(): string | null {
  return null;
}

/**
 * @deprecated 토큰은 서버 사이드 Route Handler에서 설정한다.
 */
export function setTokens(
  _accessToken: string,
  _refreshToken: string,
): void {
  // no-op: httpOnly 쿠키는 서버에서만 설정 가능
}

/**
 * @deprecated 토큰 삭제는 /api/auth/logout Route Handler에서 수행한다.
 */
export function clearTokens(): void {
  // no-op: httpOnly 쿠키는 서버에서만 삭제 가능
}

/** proxy.ts(서버 사이드)에서 쿠키 존재 여부 확인용 */
export function hasAccessTokenCookie(
  cookieHeader: string | null,
): boolean {
  if (!cookieHeader) return false;
  return cookieHeader.includes(`${COOKIE_ACCESS_TOKEN}=`);
}
