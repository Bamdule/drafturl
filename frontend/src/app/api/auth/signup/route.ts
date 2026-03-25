import { NextResponse } from "next/server";
import { setAuthCookies } from "@/lib/auth/cookies";
import type { ApiResponse, EmailAuthResponse } from "@/lib/api/types";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

export async function POST(request: Request) {
  try {
    const body = await request.json();

    const res = await fetch(`${BACKEND_URL}/api/v1/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const json = (await res.json()) as ApiResponse<EmailAuthResponse>;

    if (!json.success) {
      return NextResponse.json(json, { status: res.status });
    }

    const { accessToken, refreshToken, user } = json.data;

    await setAuthCookies(accessToken, refreshToken);

    return NextResponse.json({
      success: true,
      data: { user },
    });
  } catch {
    return NextResponse.json(
      { success: false, error: { code: "INTERNAL_ERROR", message: "회원가입 처리 중 오류가 발생했습니다." } },
      { status: 500 },
    );
  }
}
