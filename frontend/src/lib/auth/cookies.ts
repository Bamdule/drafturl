import { cookies } from "next/headers";
import {
  COOKIE_ACCESS_TOKEN,
  COOKIE_REFRESH_TOKEN,
} from "@/lib/constants";

/**
 * 서버 사이드 httpOnly 쿠키 설정 유틸.
 * Route Handler에서만 사용한다.
 */

const isProduction = process.env.NODE_ENV === "production";

export async function setAuthCookies(
  accessToken: string,
  refreshToken: string,
): Promise<void> {
  const cookieStore = await cookies();

  cookieStore.set(COOKIE_ACCESS_TOKEN, accessToken, {
    httpOnly: true,
    secure: isProduction,
    sameSite: "lax",
    path: "/",
    maxAge: 3600, // 1시간
  });

  cookieStore.set(COOKIE_REFRESH_TOKEN, refreshToken, {
    httpOnly: true,
    secure: isProduction,
    sameSite: "lax",
    path: "/",
    maxAge: 604800, // 7일
  });
}

export async function clearAuthCookies(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(COOKIE_ACCESS_TOKEN);
  cookieStore.delete(COOKIE_REFRESH_TOKEN);
}

export async function getAuthTokensFromCookies(): Promise<{
  accessToken: string | null;
  refreshToken: string | null;
}> {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(COOKIE_ACCESS_TOKEN)?.value ?? null;
  const refreshToken = cookieStore.get(COOKIE_REFRESH_TOKEN)?.value ?? null;
  return { accessToken, refreshToken };
}
