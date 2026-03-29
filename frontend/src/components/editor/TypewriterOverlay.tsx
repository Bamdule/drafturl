"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { useEditorStore, SAMPLE_HTML, SAMPLE_MARKDOWN } from "@/lib/store/useEditorStore";
import { useDict } from "@/components/i18n/DictProvider";

const CHARS_PER_TICK = 2;
const TICK_MS = 12;

interface TypewriterOverlayProps {
  active: boolean;
  onDismiss: () => void;
}

export default function TypewriterOverlay({ active, onDismiss }: TypewriterOverlayProps) {
  const { dict } = useDict();
  const { docType, setContent, setIsDemo } = useEditorStore();
  const codeRef = useRef<HTMLPreElement>(null);
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const indexRef = useRef(0);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const previewTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const sampleRef = useRef("");
  const [visible, setVisible] = useState(false);

  const PREVIEW_INTERVAL = 250; // 미리보기 갱신 간격 (ms)

  // 클릭 시 빈 에디터로 전환 (타이핑 내용은 데모용이므로 넘기지 않음)
  const handleDismiss = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    if (previewTimerRef.current) clearTimeout(previewTimerRef.current);
    setContent("");
    setIsDemo(false);
    setVisible(false);
    onDismiss();
  }, [setContent, setIsDemo, onDismiss]);

  useEffect(() => {
    if (!active) {
      setVisible(false);
      return;
    }

    const sample = docType === "html" ? SAMPLE_HTML : SAMPLE_MARKDOWN;
    sampleRef.current = sample;
    indexRef.current = 0;
    setVisible(true);

    // 코드 영역 초기화
    if (codeRef.current) codeRef.current.textContent = "";

    const tick = () => {
      if (indexRef.current >= sample.length) {
        // 완료 — 에디터에 전체 내용 세팅 (데모 플래그 on)
        setIsDemo(true);
        setContent(sample);
        setTimeout(() => {
          setVisible(false);
          onDismiss();
        }, 500);
        return;
      }

      indexRef.current = Math.min(indexRef.current + CHARS_PER_TICK, sample.length);
      const currentText = sample.slice(0, indexRef.current);

      // 코드 영역 업데이트 (DOM 직접 조작 — 리렌더링 없음)
      if (codeRef.current) {
        codeRef.current.textContent = currentText;
        // 자동 스크롤
        codeRef.current.scrollTop = codeRef.current.scrollHeight;
      }

      // 미리보기는 별도 타이머에서 갱신 (여기서는 코드만 업데이트)

      timerRef.current = setTimeout(tick, TICK_MS);
    };

    // 미리보기 갱신 타이머 (PREVIEW_INTERVAL 간격으로 iframe 업데이트)
    const updatePreview = () => {
      if (!iframeRef.current) return;
      const currentText = sample.slice(0, indexRef.current);
      const iframe = iframeRef.current;

      if (docType === "html") {
        // DOMParser로 불완전한 HTML을 파싱하여 완전한 HTML로 변환
        const parser = new DOMParser();
        const doc = parser.parseFromString(currentText, "text/html");
        iframe.srcdoc = doc.documentElement.outerHTML;
      } else {
        iframe.srcdoc = `<!DOCTYPE html>
<html><head><style>
body { font-family: system-ui, -apple-system, sans-serif; line-height: 1.7; padding: 16px; color: #1a1a2e; max-width: 800px; margin: 0 auto; }
h1, h2, h3 { margin-top: 1.5em; margin-bottom: 0.5em; }
h1 { font-size: 28px; border-bottom: 2px solid #e8e8f0; padding-bottom: 8px; }
pre { background: #1a1a2e; color: #d4d4f0; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; }
code { background: #f0f0f4; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }
pre code { background: none; padding: 0; color: inherit; }
blockquote { border-left: 3px solid #7c5cfc; padding-left: 16px; color: #666; margin: 16px 0; }
table { border-collapse: collapse; width: 100%; margin: 16px 0; }
th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
th { background: #f5f5ff; font-weight: 600; }
</style>
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"><\/script>
</head><body><div id="c"></div>
<script>document.getElementById('c').innerHTML=marked.parse(${JSON.stringify(currentText)});<\/script>
</body></html>`;
      }

      previewTimerRef.current = setTimeout(updatePreview, PREVIEW_INTERVAL);
    };

    // 약간의 딜레이 후 시작
    timerRef.current = setTimeout(tick, 500);
    previewTimerRef.current = setTimeout(updatePreview, 800);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      if (previewTimerRef.current) clearTimeout(previewTimerRef.current);
    };
  }, [active, docType, setContent, setIsDemo, onDismiss]);

  if (!visible) return null;

  return (
    <div className="absolute inset-0 z-10">
      {/* 클릭만 감지 — 드래그 이벤트는 통과시켜 document의 FileDropZone이 처리 */}
      <div
        className="absolute inset-0 z-20 cursor-text"
        onClick={handleDismiss}
      />

      {/* 콘텐츠 영역 — 포인터 이벤트 없음 (클릭 레이어가 처리) */}
      <div className="absolute inset-0 grid grid-cols-1 md:grid-cols-2 pointer-events-none">
        {/* 코드 영역 */}
        <div className="bg-[#1e1e1e] overflow-hidden flex flex-col">
          <div className="flex items-center justify-between px-4 py-2 bg-bg-tertiary border-b border-border-dark text-xs font-medium text-text-muted uppercase tracking-wider">
            <span>{dict.typewriter.editor}</span>
            <span className="normal-case tracking-normal animate-pulse text-accent">{dict.typewriter.typing}</span>
          </div>
          <pre
            ref={codeRef}
            className="flex-1 overflow-auto p-4 m-0 text-[13px] leading-[1.7] text-[#d4d4f0] whitespace-pre-wrap break-all"
            style={{ fontFamily: "'SF Mono', 'Fira Code', 'JetBrains Mono', 'Cascadia Code', monospace" }}
          />
        </div>

        {/* 미리보기 영역 */}
        <div className="bg-white overflow-hidden flex flex-col border-l border-border-dark hidden md:flex">
          <div className="flex items-center justify-between px-4 py-2 bg-[#f0f0f4] border-b border-[#ddd] text-xs font-medium text-[#666] uppercase tracking-wider">
            <span>{dict.typewriter.preview}</span>
            <span className="text-[#34d399] normal-case tracking-normal">{dict.typewriter.live}</span>
          </div>
          <iframe
            ref={iframeRef}
            className="flex-1 border-none bg-white"
            sandbox="allow-scripts"
            title="타이핑 미리보기"
          />
        </div>
      </div>
    </div>
  );
}
