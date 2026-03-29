import { NextResponse } from "next/server";
import { getAuthTokensFromCookies, clearAuthCookies } from "@/lib/auth/cookies";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

export async function DELETE() {
  try {
    const { accessToken } = await getAuthTokensFromCookies();

    if (!accessToken) {
      return NextResponse.json(
        { success: false, error: { code: "UNAUTHORIZED", message: "로그인이 필요합니다." } },
        { status: 401 },
      );
    }

    const res = await fetch(`${BACKEND_URL}/api/v1/auth/me`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    const json = await res.json();

    if (!json.success) {
      return NextResponse.json(json, { status: res.status });
    }

    await clearAuthCookies();

    return NextResponse.json({ success: true, data: null });
  } catch {
    try {
      await clearAuthCookies();
    } catch {
      // ignore
    }
    return NextResponse.json(
      { success: false, error: { code: "INTERNAL_ERROR", message: "회원탈퇴 처리 중 오류가 발생했습니다." } },
      { status: 500 },
    );
  }
}
