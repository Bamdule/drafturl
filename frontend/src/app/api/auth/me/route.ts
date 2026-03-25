import { NextResponse } from "next/server";
import { getAuthTokensFromCookies } from "@/lib/auth/cookies";
import type { ApiResponse, UserWithStorage } from "@/lib/api/types";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

export async function GET() {
  try {
    const { accessToken } = await getAuthTokensFromCookies();

    if (!accessToken) {
      // 비로그인 상태는 에러가 아니므로 200 + null 반환 (콘솔 에러 방지)
      return NextResponse.json({ success: true, data: null });
    }

    const res = await fetch(`${BACKEND_URL}/api/v1/auth/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    const json = (await res.json()) as ApiResponse<UserWithStorage>;

    if (!json.success) {
      return NextResponse.json(json, { status: res.status });
    }

    return NextResponse.json(json);
  } catch {
    return NextResponse.json(
      { success: false, error: { code: "INTERNAL_ERROR", message: "사용자 정보 조회 중 오류가 발생했습니다." } },
      { status: 500 },
    );
  }
}
