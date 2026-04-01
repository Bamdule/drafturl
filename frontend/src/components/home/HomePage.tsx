"use client";

import { useState, useCallback, useRef } from "react";
import Link from "next/link";
import Header from "@/components/layout/Header";
import FileDropZone from "@/components/editor/FileDropZone";
import PublishResultModal from "@/components/common/PublishResultModal";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { createDocument } from "@/lib/api/documents";
import { useDict } from "@/components/i18n/DictProvider";
import { ApiError } from "@/lib/api/types";
import type { DocumentSummary } from "@/lib/api/types";

export default function HomePage({ children }: { children?: React.ReactNode }) {
  const { dict, locale } = useDict();
  const { content, docType, setContent, setDocType } = useEditorStore();
  const { isAuthenticated } = useAuthStore();
  const [isPublishing, setIsPublishing] = useState(false);
  const [result, setResult] = useState<DocumentSummary | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const [fileSize, setFileSize] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const hasFile = content.trim().length > 0;

  const handleFileDrop = useCallback(() => {
    // FileDropZone이 에디터 스토어에 content를 설정함
    // 파일 이름/크기 정보는 FileDropZone에서 받을 수 없으므로 기본값 사용
  }, []);

  const handleDropZoneClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const ext = file.name.split(".").pop()?.toLowerCase();
    if (!ext || !["html", "htm", "md", "markdown"].includes(ext)) {
      setError(dict.fileDrop.unsupportedError);
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError(dict.fileDrop.fileSizeError);
      return;
    }

    const reader = new FileReader();
    reader.onload = (ev) => {
      const text = ev.target?.result as string;
      setContent(text);
      setDocType(["html", "htm"].includes(ext) ? "html" : "markdown");
      setFileName(file.name);
      setFileSize(formatBytes(file.size));
      setError(null);
    };
    reader.readAsText(file);
    // reset input so same file can be re-selected
    e.target.value = "";
  };

  const handlePreview = () => {
    if (!content.trim()) return;
    const w = window.open("", "_blank");
    if (!w) return;

    if (docType === "html") {
      w.document.write(content);
      w.document.close();
    } else {
      w.document.write(`<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>Preview</title>
<style>body{font-family:system-ui,-apple-system,sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.7;color:#1a1a2e}
h1,h2,h3{margin-top:1.5em}h1{font-size:28px;border-bottom:2px solid #e8e8f0;padding-bottom:8px}
pre{background:#1a1a2e;color:#d4d4f0;padding:16px;border-radius:8px;overflow-x:auto}
code{background:#f0f0f4;padding:2px 6px;border-radius:4px}pre code{background:none}
blockquote{border-left:3px solid #7c5cfc;padding-left:16px;color:#666}
table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:8px 12px}th{background:#f5f5ff}</style>
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"><\/script>
</head><body><div id="c"></div>
<script>document.getElementById('c').innerHTML=marked.parse(${JSON.stringify(content)});<\/script>
</body></html>`);
      w.document.close();
    }
  };

  const handlePublish = async () => {
    if (!content.trim()) return;
    setIsPublishing(true);
    setError(null);

    try {
      const doc = await createDocument(
        {
          content,
          type: docType,
          title:
            fileName?.replace(/\.(html?|md|markdown)$/i, "") || undefined,
        },
        isAuthenticated,
      );
      setResult(doc);
      setModalOpen(true);
      setContent("");
      setFileName(null);
      setFileSize(null);
      window.umami?.track("document_create", { type: docType });
    } catch (err) {
      if (err instanceof ApiError && err.code === "DOCUMENT_LIMIT_EXCEEDED") {
        setError(dict.publish.limitExceeded);
      } else {
        setError(
          err instanceof Error ? err.message : dict.publish.genericError,
        );
      }
    } finally {
      setIsPublishing(false);
    }
  };

  const fileType = docType === "html" ? "HTML" : "MD";
  const displayName =
    fileName || (docType === "html" ? "document.html" : "document.md");
  const displaySize = fileSize || formatBytes(new Blob([content]).size);
  return (
    <div className="flex min-h-screen flex-col bg-bg-primary" lang={locale}>
      <FileDropZone onFileDrop={handleFileDrop} />
      <Header />

      <div className="mx-auto flex w-full max-w-[1200px] flex-1 items-center gap-8 px-6 py-8 md:gap-12 md:px-10 grid grid-cols-1 md:grid-cols-[5fr_7fr]">
        {/* Left: Hero + Steps */}
        <div className="flex flex-col gap-7">
          <section>
            <h1 className="mb-3 text-[clamp(28px,3.5vw,42px)] font-[900] leading-[1.3] tracking-tight text-white">
              {dict.home.title}{" "}
              <span className="gradient-text">
                {dict.home.titleHighlight}
              </span>
              {dict.home.titleSuffix ? ` ${dict.home.titleSuffix}` : ""}
            </h1>
            <p className="text-[clamp(14px,1.6vw,16px)] leading-relaxed text-text-secondary">
              {dict.home.subtitle}
            </p>
          </section>

          <div className="flex flex-col gap-2.5">
            {[
              {
                num: "1",
                title: dict.home.steps.paste.title,
                desc: dict.home.steps.paste.description,
                arrow: true,
              },
              {
                num: "2",
                title: dict.home.steps.check.title,
                desc: dict.home.steps.check.description,
                arrow: true,
              },
              {
                num: "3",
                title: dict.home.steps.share.title,
                desc: dict.home.steps.share.description,
                arrow: false,
              },
            ].map((step) => (
              <div
                key={step.num}
                className="flex items-center gap-3.5 rounded-xl border border-border-dark bg-gradient-to-br from-bg-secondary to-bg-secondary/50 px-4 py-3 transition-all hover:translate-x-1 hover:border-accent/30"
              >
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-accent to-accent-hover text-sm font-extrabold text-white shadow-[0_0_10px_rgba(124,92,252,0.25)]">
                  {step.num}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-bold text-text-primary">
                    {step.title}
                  </div>
                  <div className="text-xs leading-snug text-text-muted">
                    {step.desc}
                  </div>
                </div>
                {step.arrow && (
                  <span className="shrink-0 text-base text-text-muted/25">
                    →
                  </span>
                )}
              </div>
            ))}
          </div>

          <div className="hidden md:block text-xs text-text-muted">
            {dict.publish.expiryNotice}{" "}
            <Link href="/auth/login" className="text-accent hover:underline">
              {dict.publish.loginKeep}
            </Link>
          </div>
        </div>

        {/* Right: Drop zone */}
        <div className="flex flex-col gap-3 self-center">
          {/* Hidden file input */}
          <input
            ref={fileInputRef}
            type="file"
            accept=".html,.htm,.md,.markdown,text/html,text/markdown,text/plain"
            className="hidden"
            onChange={handleFileInput}
          />

          <div
            onClick={!hasFile ? handleDropZoneClick : undefined}
            role={!hasFile ? "button" : undefined}
            tabIndex={!hasFile ? 0 : undefined}
            onKeyDown={
              !hasFile
                ? (e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      handleDropZoneClick();
                    }
                  }
                : undefined
            }
            className={`relative flex min-h-[280px] md:min-h-[420px] flex-col items-center justify-center gap-5 overflow-hidden rounded-2xl p-6 md:p-8 transition-all ${
              hasFile
                ? "border-2 border-solid border-green-500/30 bg-gradient-to-b from-green-500/[0.04] to-bg-secondary/80"
                : "cursor-pointer border-2 border-dashed border-accent/25 bg-gradient-to-b from-accent/[0.04] to-bg-secondary/80 hover:border-accent/50 hover:shadow-[0_0_48px_rgba(124,92,252,0.08)]"
            }`}
          >
            {/* Glow effect */}
            <div
              className={`pointer-events-none absolute inset-0 transition-opacity ${hasFile ? "opacity-100" : "opacity-0"}`}
              style={{
                background: hasFile
                  ? "radial-gradient(circle at 50% 40%, rgba(52,211,153,0.04), transparent 60%)"
                  : "radial-gradient(circle at 50% 40%, rgba(124,92,252,0.05), transparent 60%)",
              }}
            />

            {/* Icon */}
            <div
              className={`relative flex h-16 w-16 items-center justify-center rounded-2xl shadow-lg ${
                hasFile
                  ? "border border-green-500/20 bg-gradient-to-br from-green-500/15 to-green-500/5"
                  : "border border-accent/20 bg-gradient-to-br from-accent/15 to-blue-400/5"
              }`}
            >
              {hasFile ? (
                <svg
                  width="28"
                  height="28"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#34d399"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M20 6L9 17l-5-5" />
                </svg>
              ) : (
                <svg
                  width="28"
                  height="28"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="var(--color-accent)"
                  strokeWidth="2"
                  strokeLinecap="round"
                >
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
              )}
            </div>

            {hasFile ? (
              /* File loaded content */
              <div className="relative flex w-full flex-col items-center gap-5">
                <div className="flex flex-col items-center gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-lg font-bold text-text-primary">
                      {displayName}
                    </span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setContent("");
                        setFileName(null);
                        setFileSize(null);
                        setError(null);
                      }}
                      className="flex h-6 w-6 items-center justify-center rounded-full text-text-muted hover:bg-white/10 hover:text-text-primary transition-colors cursor-pointer"
                      aria-label="Remove file"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
                    </button>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="rounded border border-accent/20 bg-accent/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-accent">
                      {fileType}
                    </span>
                    <span className="text-sm text-text-muted">
                      {displaySize}
                    </span>
                  </div>
                </div>

                {error && (
                  <p className="text-center text-sm text-danger">{error}</p>
                )}

                <div className="flex w-full max-w-[360px] gap-2.5 flex-col sm:flex-row">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handlePreview();
                    }}
                    className="inline-flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl border border-border-dark bg-bg-tertiary/50 px-5 py-3.5 text-sm font-bold text-text-secondary transition-all hover:border-border-dark-hover hover:bg-accent/[0.06] hover:text-text-primary"
                  >
                    <svg
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    {dict.publish.preview}
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handlePublish();
                    }}
                    disabled={isPublishing}
                    className="inline-flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-accent to-accent-hover px-5 py-3.5 text-sm font-bold text-white shadow-[0_4px_20px_rgba(124,92,252,0.25)] transition-all hover:-translate-y-0.5 hover:shadow-[0_6px_28px_rgba(124,92,252,0.4)] disabled:opacity-50"
                  >
                    <svg
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                    >
                      <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
                      <polyline points="16 6 12 2 8 6" />
                      <line x1="12" y1="2" x2="12" y2="15" />
                    </svg>
                    {isPublishing
                      ? dict.publish.publishing
                      : dict.publish.share}
                  </button>
                </div>

                <div className="text-center text-xs text-text-muted">
                  {dict.publish.expiryNotice}{" "}
                  <Link
                    href="/auth/login"
                    className="text-accent hover:underline"
                  >
                    {dict.publish.loginKeep}
                  </Link>
                </div>
              </div>
            ) : (
              /* Empty state */
              <div className="relative flex flex-col items-center gap-2">
                <div className="text-base md:text-lg font-bold text-text-primary">
                  {dict.fileDrop.dropHere}
                </div>
                <div className="mt-2 text-xs md:text-sm text-text-muted">
                  {dict.fileDrop.supportedFormats}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {children}

      <PublishResultModal
        open={modalOpen}
        onOpenChange={setModalOpen}
        document={result}
        isAuthenticated={isAuthenticated}
      />
    </div>
  );
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
}
