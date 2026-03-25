"use client";

import { useEffect, useState } from "react";
import { renderMarkdown } from "@/lib/markdown";

interface MarkdownViewerProps {
  content: string;
  title?: string | null;
}

export default function MarkdownViewer({
  content,
  title,
}: MarkdownViewerProps) {
  const [html, setHtml] = useState("");

  useEffect(() => {
    renderMarkdown(content).then(setHtml);
  }, [content]);

  const wrappedHtml = `<!DOCTYPE html>
<html><head>
<style>
  body { font-family: system-ui, -apple-system, sans-serif; line-height: 1.7; padding: 24px; color: #1a1a2e; max-width: 800px; margin: 0 auto; }
  h1, h2, h3 { margin-top: 1.5em; margin-bottom: 0.5em; }
  h1 { font-size: 28px; border-bottom: 2px solid #e8e8f0; padding-bottom: 8px; }
  h2 { font-size: 22px; margin-top: 32px; }
  h3 { font-size: 18px; }
  pre { background: #1a1a2e; color: #d4d4f0; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; }
  code { background: #f0f0f4; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }
  pre code { background: none; padding: 0; color: inherit; }
  blockquote { border-left: 3px solid #7c5cfc; padding-left: 16px; color: #666; margin: 16px 0; }
  table { border-collapse: collapse; width: 100%; margin: 16px 0; }
  th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
  th { background: #f5f5ff; font-weight: 600; }
  img { max-width: 100%; }
  a { color: #7c5cfc; }
  li { margin: 4px 0; margin-left: 20px; }
</style>
</head><body>${html}</body></html>`;

  return (
    <div className="flex h-full w-full flex-col">
      <iframe
        srcDoc={wrappedHtml}
        className="flex-1 w-full bg-white"
        sandbox="allow-same-origin"
        title={title ?? "문서"}
      />
    </div>
  );
}
