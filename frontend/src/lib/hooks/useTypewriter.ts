"use client";

import { useEffect, useRef, useCallback } from "react";
import { useEditorStore, SAMPLE_HTML, SAMPLE_MARKDOWN } from "@/lib/store/useEditorStore";

const CHARS_PER_TICK = 3;
const TICK_MS = 16;

/**
 * 에디터에 샘플 콘텐츠를 타이핑 애니메이션으로 입력한다.
 * 사용자가 직접 입력하면 애니메이션을 중단한다.
 */
export function useTypewriter() {
  const { content, docType, setContent } = useEditorStore();
  const animatingRef = useRef(false);
  const cancelRef = useRef(false);

  const initializedRef = useRef(false);

  const startTyping = useCallback(() => {
    if (animatingRef.current) return;

    const sample = docType === "html" ? SAMPLE_HTML : SAMPLE_MARKDOWN;
    animatingRef.current = true;
    cancelRef.current = false;
    let index = 0;

    const tick = () => {
      if (cancelRef.current || index >= sample.length) {
        animatingRef.current = false;
        if (!cancelRef.current) {
          setContent(sample);
        }
        return;
      }

      index = Math.min(index + CHARS_PER_TICK, sample.length);
      setContent(sample.slice(0, index));
      setTimeout(tick, TICK_MS);
    };

    // 약간의 딜레이 후 시작
    setTimeout(() => requestAnimationFrame(tick), 300);
  }, [docType, setContent]);

  const stopTyping = useCallback(() => {
    cancelRef.current = true;
    animatingRef.current = false;
  }, []);

  const isAnimating = useCallback(() => animatingRef.current, []);

  // 최초 진입 시 자동 시작
  useEffect(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      if (content === "") {
        startTyping();
      }
    }
  }, [content, startTyping]);

  // 에디터 클릭/포커스 시 타이핑 중단
  useEffect(() => {
    const handleEditorFocus = () => {
      if (animatingRef.current) {
        stopTyping();
      }
    };
    document.addEventListener("editor-user-input", handleEditorFocus);
    return () => document.removeEventListener("editor-user-input", handleEditorFocus);
  }, [stopTyping]);

  return { startTyping, stopTyping, isAnimating };
}
