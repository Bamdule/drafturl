"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { createDocument } from "@/lib/api/documents";
import { useDict } from "@/components/i18n/DictProvider";
import PublishResultModal from "./PublishResultModal";
import type { DocumentSummary } from "@/lib/api/types";

interface PublishButtonProps {
  onNewDocument?: () => void;
}

export default function PublishButton({ onNewDocument }: PublishButtonProps) {
  const { dict } = useDict();
  const { content, docType, title, isDemo, reset } = useEditorStore();
  const { isAuthenticated } = useAuthStore();
  const [isPublishing, setIsPublishing] = useState(false);
  const [result, setResult] = useState<DocumentSummary | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [passwordEnabled, setPasswordEnabled] = useState(false);
  const [docPassword, setDocPassword] = useState("");

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
      setError(dict.publish.emptyError);
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
          password: passwordEnabled && docPassword.trim() ? docPassword.trim() : undefined,
        },
        isAuthenticated,
      );
      setResult(doc);
      setModalOpen(true);
      window.umami?.track("document_create", { type: docType });
      publishedContentRef.current = content;
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : dict.publish.genericError,
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
<html><head><meta charset="UTF-8"><title>${dict.publish.previewTitle}</title>
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
              &#128064; {dict.publish.viewDetail}
            </a>
            <button
              onClick={handleNewDocument}
              className="px-6 py-3.5 text-base font-medium text-text-secondary border border-border-dark rounded-lg hover:bg-bg-tertiary hover:text-text-primary transition-all cursor-pointer"
            >
              {dict.publish.newDocument}
            </button>
          </div>
        ) : (
          <>
            {/* 비밀번호 설정 */}
            <div className="flex items-center gap-2 text-sm">
              <label className="flex items-center gap-1.5 text-text-secondary cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={passwordEnabled}
                  onChange={(e) => {
                    setPasswordEnabled(e.target.checked);
                    if (!e.target.checked) setDocPassword("");
                  }}
                  className="accent-accent"
                />
                {dict.publish.passwordLabel}
              </label>
              {passwordEnabled && (
                <input
                  type="password"
                  value={docPassword}
                  onChange={(e) => setDocPassword(e.target.value)}
                  placeholder={dict.publish.passwordPlaceholder}
                  className="h-8 w-36 rounded border border-border-dark bg-bg-secondary px-2 text-xs text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-1 focus:ring-accent/50"
                />
              )}
            </div>

            {/* 공유 전: 미리보기 + 공유하기 버튼 */}
            <div className="flex items-center gap-3">
            <button
              onClick={handlePreview}
              disabled={!content.trim()}
              className="px-6 py-3.5 text-base font-medium text-text-secondary border border-border-dark rounded-lg hover:bg-bg-tertiary hover:text-text-primary transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              &#128064; {dict.publish.preview}
            </button>
            <button
              onClick={handlePublish}
              disabled={isPublishing || !content.trim() || isDemo}
              className="px-8 py-3.5 text-base font-semibold text-white bg-gradient-to-br from-accent to-accent-hover rounded-lg shadow-[0_0_24px_rgba(124,92,252,0.3)] hover:shadow-[0_0_32px_rgba(124,92,252,0.45)] hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {isPublishing ? dict.publish.publishing : `\u26A1 ${dict.publish.share}`}
            </button>
            </div>
          </>
        )}

        {!isAuthenticated && !result && (
          <p className="text-sm text-text-muted">
            {dict.publish.expiryNotice}{" "}
            <Link
              href="/auth/login"
              className="text-accent hover:underline"
            >
              {dict.publish.loginKeep}
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
