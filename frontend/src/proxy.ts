import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { COOKIE_ACCESS_TOKEN } from "@/lib/constants";

const LOCALES = ["en", "ko"] as const;
const DEFAULT_LOCALE = "ko";
const LOCALE_COOKIE = "preferred-locale";

/**
 * Accept-Language 헤더에서 선호 locale을 추출한다.
 */
function getPreferredLocale(request: NextRequest): string {
  // 1. 쿠키에 저장된 locale 우선
  const cookie = request.cookies.get(LOCALE_COOKIE)?.value;
  if (cookie && (LOCALES as readonly string[]).includes(cookie)) return cookie;

  // 2. Accept-Language 헤더 분석
  const acceptLang = request.headers.get("accept-language") ?? "";
  for (const part of acceptLang.split(",")) {
    const lang = part.split(";")[0].trim().toLowerCase();
    if (lang.startsWith("ko")) return "ko";
    if (lang.startsWith("en")) return "en";
  }
  return DEFAULT_LOCALE;
}

/**
 * Next.js 16 Proxy (구 middleware).
 * - / → Accept-Language 기반으로 /en 또는 /ko로 리다이렉트
 * - /en, /ko → locale 쿠키 저장 + x-locale 헤더 주입
 * - 기타 경로 → 쿠키에서 locale 읽어 x-locale 헤더 주입
 * - /dashboard/* → JWT 쿠키 인증 확인
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // 1. 루트 경로 → 선호 locale로 리다이렉트
  if (pathname === "/") {
    const locale = getPreferredLocale(request);
    return NextResponse.redirect(new URL(`/${locale}`, request.url));
  }

  // 2. locale 경로 → 쿠키 저장 + x-locale 헤더 주입
  const localeMatch = LOCALES.find(
    (l) => pathname === `/${l}` || pathname.startsWith(`/${l}/`),
  );
  if (localeMatch) {
    const requestHeaders = new Headers(request.headers);
    requestHeaders.set("x-locale", localeMatch);
    const response = NextResponse.next({
      request: { headers: requestHeaders },
    });
    response.cookies.set(LOCALE_COOKIE, localeMatch, {
      path: "/",
      maxAge: 60 * 60 * 24 * 365, // 1년
      sameSite: "lax",
    });
    return response;
  }

  // 3. /dashboard 경로 보호
  if (pathname.startsWith("/dashboard")) {
    const token = request.cookies.get(COOKIE_ACCESS_TOKEN);
    if (!token?.value) {
      const loginUrl = new URL("/auth/login", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }
  }

  // 4. 기타 경로 → 쿠키에서 locale 읽어 x-locale 헤더 주입
  const locale = request.cookies.get(LOCALE_COOKIE)?.value ?? DEFAULT_LOCALE;
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-locale", locale);
  return NextResponse.next({
    request: { headers: requestHeaders },
  });
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon\\.ico|favicon\\.svg|sitemap\\.xml|robots\\.txt|manifest\\.webmanifest).*)",
  ],
};
