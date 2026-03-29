import { type NextRequest, NextResponse } from "next/server";

const BACKEND_URL =
  process.env.INTERNAL_API_URL ?? "http://localhost:8080";

/**
 * MCP/OAuth2/Well-known 경로를 백엔드로 투명 프록시.
 * SSE 스트리밍, HTML 응답, 리다이렉트를 모두 지원한다.
 */
export async function mcpProxy(
  request: NextRequest,
  backendPath: string,
) {
  try {
    const url = new URL(backendPath, BACKEND_URL);

    // 쿼리 파라미터 전달
    request.nextUrl.searchParams.forEach((value, key) => {
      url.searchParams.set(key, value);
    });

    // 헤더 전달 (필요한 것만)
    const headers = new Headers();
    for (const key of [
      "content-type",
      "accept",
      "authorization",
      "mcp-session-id",
      "cookie",
    ]) {
      const value = request.headers.get(key);
      if (value) headers.set(key, value);
    }

    const fetchOptions: RequestInit = {
      method: request.method,
      headers,
      redirect: "manual",
    };

    if (request.method !== "GET" && request.method !== "HEAD") {
      fetchOptions.body = await request.arrayBuffer();
    }

    const res = await fetch(url.toString(), fetchOptions);

    // 리다이렉트 응답 전달
    if (res.status >= 300 && res.status < 400) {
      const location = res.headers.get("location");
      if (location) {
        return NextResponse.redirect(location, res.status);
      }
    }

    // 응답 헤더 복사
    const responseHeaders = new Headers();
    for (const key of [
      "content-type",
      "cache-control",
      "www-authenticate",
      "mcp-session-id",
      "set-cookie",
    ]) {
      const value = res.headers.get(key);
      if (value) responseHeaders.set(key, value);
    }

    return new NextResponse(res.body, {
      status: res.status,
      headers: responseHeaders,
    });
  } catch {
    return NextResponse.json(
      { error: "Proxy error" },
      { status: 502 },
    );
  }
}
