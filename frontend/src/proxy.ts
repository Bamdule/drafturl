import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { COOKIE_ACCESS_TOKEN } from "@/lib/constants";

/**
 * Next.js 16 Proxy (구 middleware).
 * /dashboard/* 경로에 대해 JWT 쿠키 존재 여부를 확인하여
 * 미인증 시 /auth/login으로 리다이렉트한다.
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // /dashboard 경로 보호
  if (pathname.startsWith("/dashboard")) {
    const token = request.cookies.get(COOKIE_ACCESS_TOKEN);

    if (!token?.value) {
      const loginUrl = new URL("/auth/login", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*"],
};
