"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { createDocument } from "@/lib/api/documents";
import PublishResultModal from "./PublishResultModal";
import type { DocumentSummary } from "@/lib/api/types";

export default function PublishButton({ onNewDocument }: { onNewDocument?: () => void } = {}) {
  const { content, docType, title, reset } = useEditorStore();
  const { isAuthenticated } = useAuthStore();
  const [isPublishing, setIsPublishing] = useState(false);
  const [result, setResult] = useState<DocumentSummary | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 공유 완료 상태에서 에디터 내용이 변경되면 자동으로 새 문서 모드로 전환
  const publishedContentRef = useRef<string | null>(null);

  useEffect(() => {
    if (result && publishedContentRef.current !== null && content !== publishedContentRef.current) {
      setResult(null);
      publishedContentRef.current = null;
    }
  }, [content, result]);

  const handlePublish = async () => {
    if (!content.trim()) {
      setError("문서 내용을 입력해주세요.");
      return;
    }

    setIsPublishing(true);
    setError(null);

    try {
      const doc = await createDocument(
        {
          content,
          type: docType,
          title: title || undefined,
        },
        isAuthenticated,
      );
      setResult(doc);
      setModalOpen(true);
      publishedContentRef.current = content;
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "문서 생성 중 오류가 발생했습니다.",
      );
    } finally {
      setIsPublishing(false);
    }
  };

  const handlePreview = () => {
    const previewWindow = window.open("", "_blank");
    if (!previewWindow) return;

    if (docType === "html") {
      previewWindow.document.write(content);
      previewWindow.document.close();
    } else {
      // Markdown: 간단한 HTML 래퍼로 표시 (서버 렌더링 없이 클라이언트에서 처리)
      previewWindow.document.write(`<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>미리보기</title>
<style>
  body { font-family: system-ui, -apple-system, sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; line-height: 1.7; color: #1a1a2e; }
  h1, h2, h3 { margin-top: 1.5em; } h1 { font-size: 28px; border-bottom: 2px solid #e8e8f0; padding-bottom: 8px; }
  pre { background: #1a1a2e; color: #d4d4f0; padding: 16px; border-radius: 8px; overflow-x: auto; }
  code { background: #f0f0f4; padding: 2px 6px; border-radius: 4px; } pre code { background: none; }
  blockquote { border-left: 3px solid #7c5cfc; padding-left: 16px; color: #666; }
  table { border-collapse: collapse; width: 100%; } th, td { border: 1px solid #ddd; padding: 8px 12px; } th { background: #f5f5ff; }
</style>
</head><body><pre style="white-space:pre-wrap;background:none;color:inherit;padding:0;">${content.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"><\/script>
<script>document.body.innerHTML = marked.parse(document.querySelector('pre').textContent);<\/script>
</body></html>`);
      previewWindow.document.close();
    }
  };

  const handleNewDocument = () => {
    setResult(null);
    reset();
    onNewDocument?.();
  };

  return (
    <>
      <div className="flex flex-col items-center gap-3 py-8">
        {error && (
          <p className="text-sm text-danger">{error}</p>
        )}

        {result ? (
          // 공유 완료 후: 상세 보기 + 새 문서 버튼
          <div className="flex items-center gap-3">
            <a
              href={`/${result.slug}`}
              target="_blank"
              rel="noopener noreferrer"
              className="px-8 py-3.5 text-base font-semibold text-white bg-gradient-to-br from-accent to-accent-hover rounded-lg shadow-[0_0_24px_rgba(124,92,252,0.3)] hover:shadow-[0_0_32px_rgba(124,92,252,0.45)] hover:-translate-y-0.5 transition-all no-underline"
            >
              &#128064; 상세 보기
            </a>
            <button
              onClick={handleNewDocument}
              className="px-6 py-3.5 text-base font-medium text-text-secondary border border-border-dark rounded-lg hover:bg-bg-tertiary hover:text-text-primary transition-all cursor-pointer"
            >
              + 새 문서
            </button>
          </div>
        ) : (
          // 공유 전: 미리보기 + 공유하기 버튼
          <div className="flex items-center gap-3">
            <button
              onClick={handlePreview}
              disabled={!content.trim()}
              className="px-6 py-3.5 text-base font-medium text-text-secondary border border-border-dark rounded-lg hover:bg-bg-tertiary hover:text-text-primary transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              &#128064; 미리보기
            </button>
            <button
              onClick={handlePublish}
              disabled={isPublishing || !content.trim()}
              className="px-8 py-3.5 text-base font-semibold text-white bg-gradient-to-br from-accent to-accent-hover rounded-lg shadow-[0_0_24px_rgba(124,92,252,0.3)] hover:shadow-[0_0_32px_rgba(124,92,252,0.45)] hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {isPublishing ? "배포 중..." : "\u26A1 공유하기"}
            </button>
          </div>
        )}

        {!isAuthenticated && !result && (
          <p className="text-sm text-text-muted">
            비로그인 시 24시간 후 만료됩니다.{" "}
            <Link
              href="/auth/login"
              className="text-accent hover:underline"
            >
              로그인하면 영구 보관!
            </Link>
          </p>
        )}
      </div>

      <PublishResultModal
        open={modalOpen}
        onOpenChange={(open) => {
          setModalOpen(open);
        }}
        document={result}
        isAuthenticated={isAuthenticated}
      />
    </>
  );
}
