import { NextResponse } from "next/server";
import {
  getAuthTokensFromCookies,
  setAuthCookies,
  clearAuthCookies,
} from "@/lib/auth/cookies";
import type { ApiResponse } from "@/lib/api/types";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

/**
 * 토큰 갱신 Route Handler.
 * 프록시에서 401을 받았을 때 클라이언트가 이 엔드포인트를 호출하여
 * refresh token으로 새 access token을 받는다.
 */
export async function POST() {
  try {
    const { refreshToken } = await getAuthTokensFromCookies();

    if (!refreshToken) {
      return NextResponse.json(
        { success: false, error: { code: "UNAUTHORIZED", message: "리프레시 토큰이 없습니다." } },
        { status: 401 },
      );
    }

    const res = await fetch(`${BACKEND_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    const json = (await res.json()) as ApiResponse<{
      accessToken: string;
      refreshToken: string;
      expiresIn: number;
    }>;

    if (!json.success) {
      await clearAuthCookies();
      return NextResponse.json(json, { status: res.status });
    }

    await setAuthCookies(json.data.accessToken, json.data.refreshToken);

    return NextResponse.json({ success: true, data: null });
  } catch {
    return NextResponse.json(
      { success: false, error: { code: "INTERNAL_ERROR", message: "토큰 갱신 중 오류가 발생했습니다." } },
      { status: 500 },
    );
  }
}
