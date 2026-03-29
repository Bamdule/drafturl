"use client";

import { useEffect, useState, useRef } from "react";
import dynamic from "next/dynamic";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useDict } from "@/components/i18n/DictProvider";

const MonacoEditor = dynamic(() => import("@monaco-editor/react"), {
  ssr: false,
  loading: () => (
    <div className="flex h-full items-center justify-center bg-bg-secondary">
      <p className="text-sm text-text-muted">에디터 로딩 중...</p>
    </div>
  ),
});

const MOBILE_BREAKPOINT = 640;

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    function check() {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    }
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);

  return isMobile;
}

function useByteCount(content: string) {
  const [byteCount, setByteCount] = useState("0 bytes");

  useEffect(() => {
    const bytes = new Blob([content]).size;
    const label =
      bytes < 1024
        ? `${bytes} bytes`
        : `${(bytes / 1024).toFixed(1)} KB`;
    setByteCount(label);
  }, [content]);

  return byteCount;
}

export default function EditorPanel() {
  const { dict } = useDict();
  const { content, docType, isDemo, setContent, setIsDemo } = useEditorStore();
  const byteCount = useByteCount(content);
  const editorRef = useRef<HTMLDivElement>(null);
  const isMobile = useIsMobile();

  // Monaco 에디터가 드래그 이벤트를 가로채지 않도록 처리
  useEffect(() => {
    const el = editorRef.current;
    if (!el) return;

    const preventMonacoDrop = (e: DragEvent) => {
      if (e.dataTransfer?.types.includes("Files")) {
        e.stopPropagation();
        // document 레벨의 FileDropZone이 처리하도록 이벤트를 다시 발생
        document.dispatchEvent(new DragEvent(e.type, { bubbles: true, cancelable: true, dataTransfer: e.dataTransfer }));
      }
    };

    el.addEventListener("dragenter", preventMonacoDrop, true);
    el.addEventListener("dragover", preventMonacoDrop, true);
    el.addEventListener("drop", preventMonacoDrop, true);

    return () => {
      el.removeEventListener("dragenter", preventMonacoDrop, true);
      el.removeEventListener("dragover", preventMonacoDrop, true);
      el.removeEventListener("drop", preventMonacoDrop, true);
    };
  }, []);

  return (
    <div className="relative bg-bg-secondary flex flex-col">
      {/* Pane Header */}
      <div className="flex items-center justify-between px-4 py-2 bg-bg-tertiary border-b border-border-dark text-xs font-medium text-text-muted uppercase tracking-wider">
        <span>{dict.editor.editorLabel}</span>
        <span className="normal-case tracking-normal">{byteCount}</span>
      </div>

      {/* Editor: Monaco on desktop, textarea on mobile */}
      {isMobile ? (
        <textarea
          value={content}
          onChange={(e) => {
            document.dispatchEvent(new Event("editor-user-input"));
            if (isDemo) setIsDemo(false);
            setContent(e.target.value);
          }}
          spellCheck={false}
          className="flex-1 min-h-[260px] w-full resize-none border-none bg-[#1e1e1e] p-4 pt-3 text-[13px] leading-[1.7] text-[#d4d4d4] outline-none font-mono placeholder:text-text-muted"
          placeholder={docType === "html" ? dict.editor.htmlPlaceholder : dict.editor.mdPlaceholder}
        />
      ) : (
        <div ref={editorRef} className="flex-1 min-h-[380px]">
          <MonacoEditor
            height="100%"
            language={docType === "html" ? "html" : "markdown"}
            value={content}
            onChange={(value) => {
              document.dispatchEvent(new Event("editor-user-input"));
              if (isDemo) setIsDemo(false);
              setContent(value ?? "");
            }}
            theme="vs-dark"
            options={{
              minimap: { enabled: false },
              fontSize: 13,
              lineNumbers: "on",
              wordWrap: "on",
              scrollBeyondLastLine: false,
              automaticLayout: true,
              tabSize: 2,
              padding: { top: 12 },
              dropIntoEditor: { enabled: false },
              fontFamily:
                "'SF Mono', 'Fira Code', 'JetBrains Mono', 'Cascadia Code', monospace",
              lineHeight: 1.7 * 13,
            }}
          />
        </div>
      )}
    </div>
  );
}
