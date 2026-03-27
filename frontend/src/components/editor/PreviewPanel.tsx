"use client";

import { useEffect, useRef, useState } from "react";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { renderMarkdown } from "@/lib/markdown";
import { PREVIEW_DEBOUNCE_MS } from "@/lib/constants";

export default function PreviewPanel() {
  const { content, docType } = useEditorStore();
  const [previewHtml, setPreviewHtml] = useState("");
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(async () => {
      if (docType === "html") {
        setPreviewHtml(content);
      } else {
        const html = await renderMarkdown(content);
        setPreviewHtml(html);
      }
    }, PREVIEW_DEBOUNCE_MS);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [content, docType]);

  const wrappedHtml =
    docType === "markdown"
      ? `<!DOCTYPE html>
<html><head>
<style>
  body { font-family: system-ui, -apple-system, sans-serif; line-height: 1.7; padding: 16px; color: #1a1a2e; max-width: 800px; margin: 0 auto; }
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
</head><body>${previewHtml}</body></html>`
      : previewHtml;

  return (
    <div className="border-l border-border-dark bg-white flex flex-col md:border-l md:border-t-0 border-t">
      {/* Pane Header */}
      <div className="flex items-center justify-between px-4 py-2 bg-[#f0f0f4] border-b border-[#ddd] text-xs font-medium text-[#666] uppercase tracking-wider">
        <span>미리보기</span>
        <span className="text-success normal-case tracking-normal">실시간</span>
      </div>

      {/* Preview iframe */}
      <div className="flex-1 min-h-[350px] md:min-h-[560px]">
        <iframe
          ref={iframeRef}
          srcDoc={wrappedHtml}
          className="w-full h-full min-h-[350px] md:min-h-[560px] border-none bg-white"
          sandbox="allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox"
          title="미리보기"
        />
      </div>
    </div>
  );
}
