import { type NextRequest, NextResponse } from "next/server";
import { getAuthTokensFromCookies } from "@/lib/auth/cookies";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

/**
 * 범용 API 프록시.
 * 인증이 필요한 모든 API 호출을 프록시한다.
 * httpOnly 쿠키에서 토큰을 읽어 Authorization 헤더를 추가한다.
 */
async function handler(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  try {
    const { path } = await params;
    const backendPath = `/api/v1/${path.join("/")}`;
    const url = new URL(backendPath, BACKEND_URL);

    // 쿼리 파라미터 전달
    request.nextUrl.searchParams.forEach((value, key) => {
      url.searchParams.set(key, value);
    });

    const { accessToken } = await getAuthTokensFromCookies();

    const headers = new Headers();
    // Content-Type 전달
    const contentType = request.headers.get("content-type");
    if (contentType) {
      headers.set("Content-Type", contentType);
    }
    // Authorization 헤더 추가
    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }

    const fetchOptions: RequestInit = {
      method: request.method,
      headers,
    };

    // GET/HEAD가 아닌 경우 body 전달
    if (request.method !== "GET" && request.method !== "HEAD") {
      fetchOptions.body = await request.text();
    }

    const res = await fetch(url.toString(), fetchOptions);

    // 백엔드 응답을 그대로 전달
    const responseBody = await res.text();
    return new NextResponse(responseBody, {
      status: res.status,
      headers: {
        "Content-Type": res.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch {
    return NextResponse.json(
      { success: false, error: { code: "PROXY_ERROR", message: "API 프록시 오류가 발생했습니다." } },
      { status: 502 },
    );
  }
}

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;
