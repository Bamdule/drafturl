/**
 * 브라우저 쿠키 유틸리티.
 *
 * token.ts, callback/page.tsx 등에서 중복 정의되어 있던 getCookie / setCookie / deleteCookie를
 * 한 곳으로 통합한다.
 */

export function getCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = document.cookie.match(
    new RegExp(`(?:^|; )${escaped}=([^;]*)`),
  );
  return match ? decodeURIComponent(match[1]) : null;
}

export interface SetCookieOptions {
  maxAge?: number;
  path?: string;
  sameSite?: string;
  secure?: boolean;
}

export function setCookie(
  name: string,
  value: string,
  options: SetCookieOptions = {},
): void {
  if (typeof document === "undefined") return;
  const isProduction =
    typeof location !== "undefined" && location.protocol === "https:";
  const {
    maxAge,
    path = "/",
    sameSite = "Strict",
    secure = isProduction,
  } = options;
  let cookie = `${name}=${encodeURIComponent(value)}; path=${path}; SameSite=${sameSite}`;
  if (maxAge !== undefined) cookie += `; max-age=${maxAge}`;
  if (secure) cookie += "; Secure";
  document.cookie = cookie;
}

export function deleteCookie(name: string): void {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; path=/; max-age=0`;
}
