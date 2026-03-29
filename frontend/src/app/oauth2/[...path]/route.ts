import { type NextRequest } from "next/server";
import { mcpProxy } from "@/lib/mcp-proxy";

async function handler(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const { path } = await params;
  return mcpProxy(request, `/oauth2/${path.join("/")}`);
}

export const GET = handler;
export const POST = handler;
