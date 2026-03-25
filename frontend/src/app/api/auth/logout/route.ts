import { NextResponse } from "next/server";
import { getAuthTokensFromCookies, clearAuthCookies } from "@/lib/auth/cookies";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

export async function POST(request: Request) {
  try {
    const { accessToken, refreshToken } = await getAuthTokensFromCookies();

    // 백엔드에 로그아웃 요청 (refresh token 무효화)
    if (accessToken && refreshToken) {
      try {
        await fetch(`${BACKEND_URL}/api/v1/auth/logout`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({ refreshToken }),
        });
      } catch {
        // 백엔드 로그아웃 실패해도 쿠키는 삭제
      }
    }

    await clearAuthCookies();

    return NextResponse.json({ success: true, data: null });
  } catch {
    // 에러가 나도 쿠키는 삭제 시도
    try {
      await clearAuthCookies();
    } catch {
      // ignore
    }
    return NextResponse.json({ success: true, data: null });
  }
}
