"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import Link from "next/link";
import Header from "@/components/layout/Header";
import FileDropZone from "@/components/editor/FileDropZone";
import PublishResultModal from "@/components/common/PublishResultModal";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { createDocument } from "@/lib/api/documents";
import { getMyTags, createTag, addTagToDocument } from "@/lib/api/tags";
import { useDict } from "@/components/i18n/DictProvider";
import { ApiError } from "@/lib/api/types";
import type { DocumentSummary } from "@/lib/api/types";
import type { TagWithCount } from "@/lib/api/tags";

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
  const tagInputRef = useRef<HTMLInputElement>(null);

  // 공유 전 태그/비밀번호 설정 state (로그인 사용자 전용)
  const [preShareTags, setPreShareTags] = useState<TagWithCount[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<Set<number>>(new Set());
  const [newTagName, setNewTagName] = useState("");
  const [pendingNewTagNames, setPendingNewTagNames] = useState<string[]>([]);
  const [flashTagId, setFlashTagId] = useState<number | null>(null);
  const [pendingPassword, setPendingPassword] = useState("");
  const [passwordOpen, setPasswordOpen] = useState(false);

  const hasFile = content.trim().length > 0;

  useEffect(() => {
    if (hasFile && isAuthenticated) {
      getMyTags().then(setPreShareTags).catch(() => {});
    }
  }, [hasFile, isAuthenticated]);

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

  const handleCreatePreShareTag = () => {
    const name = newTagName.trim();
    if (!name) return;

    // 중복 체크 — 기존 태그 하이라이트 후 자동 선택
    const duplicate = preShareTags.find(
      (t) => t.name.toLowerCase() === name.toLowerCase(),
    );
    if (duplicate) {
      setFlashTagId(duplicate.id);
      setSelectedTagIds((prev) => new Set([...prev, duplicate.id]));
      setNewTagName("");
      setTimeout(() => setFlashTagId(null), 700);
      tagInputRef.current?.focus();
      return;
    }

    // pending 태그 중복 체크
    if (pendingNewTagNames.some((n) => n.toLowerCase() === name.toLowerCase())) {
      setNewTagName("");
      tagInputRef.current?.focus();
      return;
    }

    // 로컬에만 추가 (API 호출 없음)
    setPendingNewTagNames((prev) => [...prev, name]);
    setNewTagName("");
    tagInputRef.current?.focus();
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
          password: pendingPassword.trim() || undefined,
        },
        isAuthenticated,
      );
      // 기존 선택 태그 + 새로 만들 태그 처리
      const allTagIds: number[] = [...selectedTagIds];

      if (pendingNewTagNames.length > 0) {
        const results = await Promise.allSettled(
          pendingNewTagNames.map((name) => createTag(name)),
        );
        for (const result of results) {
          if (result.status === "fulfilled") {
            allTagIds.push(result.value.id);
          }
        }
      }

      if (allTagIds.length > 0) {
        await Promise.allSettled(
          allTagIds.map((id) => addTagToDocument(doc.slug, id)),
        );
      }

      setResult(doc);
      setModalOpen(true);
      setContent("");
      setFileName(null);
      setFileSize(null);
      setSelectedTagIds(new Set());
      setPendingNewTagNames([]);
      setPendingPassword("");
      setPasswordOpen(false);
      setNewTagName("");
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

      <div className="flex flex-1 items-center py-8">
      <div className="mx-auto w-full max-w-[1200px] grid grid-cols-1 md:grid-cols-[5fr_7fr] gap-8 px-6 md:gap-12 md:px-10">
        {/* Left: Hero + Steps */}
        <div className="flex flex-col gap-7">
          <section className="animate-fade-up" style={{ animationDelay: "0.05s" }}>
            <h1 className="mb-3 text-[clamp(28px,3.5vw,42px)] font-[900] leading-[1.3] tracking-tight text-white">
              {dict.home.title}{" "}
              <span className="whitespace-nowrap">
                <span className="gradient-text">{dict.home.titleHighlight}</span>
                {dict.home.titleSuffix ? ` ${dict.home.titleSuffix}` : ""}
              </span>
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
            ].map((step, index) => (
              <div
                key={step.num}
                className="animate-fade-up flex items-center gap-3.5 rounded-xl border border-border-dark bg-gradient-to-br from-bg-secondary to-bg-secondary/50 px-4 py-3 transition-all hover:translate-x-1 hover:border-accent/30"
                style={{ animationDelay: `${0.15 + index * 0.1}s` }}
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

          {/* Use cases */}
          <div className="flex flex-col gap-2.5">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-text-muted/40">
              {dict.useCases.label}
            </p>
            <div className="flex flex-wrap gap-2">
              {dict.useCases.items.map((item, index) => (
                <span
                  key={item}
                  className="inline-flex items-center gap-1.5 rounded-full border border-border-dark bg-bg-secondary px-3 py-1 text-xs text-text-muted transition-colors hover:border-accent/30 hover:text-text-secondary"
                >
                  <UseCaseIcon index={index} />
                  {item}
                </span>
              ))}
            </div>
          </div>

          <div className="text-xs text-text-muted">
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
            accept=".html,.htm,.md,.markdown"
            className="hidden"
            onChange={handleFileInput}
          />

          <div
            className={`relative flex min-h-[280px] md:min-h-[420px] flex-col items-center justify-center gap-5 overflow-hidden rounded-2xl p-6 md:p-8 transition-all ${
              hasFile
                ? "border-2 border-solid border-green-500/30 bg-gradient-to-b from-green-500/[0.04] to-bg-secondary/80"
                : "border-2 border-dashed border-accent/25 bg-gradient-to-b from-accent/[0.04] to-bg-secondary/80 hover:border-accent/50 hover:shadow-[0_0_48px_rgba(124,92,252,0.08)]"
            }`}
            style={
              hasFile
                ? { animation: "fade-up 0.5s ease-out both" }
                : {
                    animation:
                      "fade-up 0.5s 0.1s ease-out both, pulse-glow 3.5s 0.6s ease-in-out infinite",
                  }
            }
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
                  : "animate-float border border-accent/20 bg-gradient-to-br from-accent/15 to-blue-400/5"
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

                {/* 공유 전 태그/비밀번호 설정 — 로그인 사용자만 */}
                {isAuthenticated && (
                  <div className="w-full max-w-[400px] text-left border-t border-border-dark/40 pt-3 pb-3 border-b flex flex-col gap-3">

                    {/* 태그: 라벨 | 칩들(wrap) + 입력(별도 행) */}
                    <div className="flex gap-3 items-start">
                      <span className="w-16 shrink-0 text-sm text-text-muted mt-[5px]">{dict.home.tagLabel}</span>
                      <div className="flex-1 flex flex-col gap-1.5">
                        {(preShareTags.length > 0 || pendingNewTagNames.length > 0) && (
                          <div className="flex flex-wrap gap-1.5">
                            {preShareTags.map((tag) => {
                              const selected = selectedTagIds.has(tag.id);
                              const flashing = flashTagId === tag.id;
                              return (
                                <button
                                  key={tag.id}
                                  type="button"
                                  onClick={() =>
                                    setSelectedTagIds((prev) => {
                                      const s = new Set(prev);
                                      if (s.has(tag.id)) { s.delete(tag.id); } else { s.add(tag.id); }
                                      return s;
                                    })
                                  }
                                  className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium transition-all cursor-pointer ${
                                    selected
                                      ? "bg-accent text-white shadow-[0_0_10px_rgba(124,92,252,0.4)]"
                                      : "bg-bg-secondary text-text-secondary border border-border-dark hover:border-accent/50 hover:text-text-primary"
                                  } ${flashing ? "outline outline-2 outline-[#7c5cfc] outline-offset-1 scale-105" : ""}`}
                                >
                                  {selected && (
                                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6L9 17l-5-5" /></svg>
                                  )}
                                  {tag.name}
                                </button>
                              );
                            })}
                            {pendingNewTagNames.map((name, i) => (
                              <span
                                key={`pending-${i}`}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-accent/20 text-accent border border-accent/40"
                              >
                                {name}
                                <button
                                  type="button"
                                  onClick={() =>
                                    setPendingNewTagNames((prev) =>
                                      prev.filter((_, idx) => idx !== i),
                                    )
                                  }
                                  className="opacity-60 hover:opacity-100 bg-transparent border-none cursor-pointer leading-none"
                                >
                                  &times;
                                </button>
                              </span>
                            ))}
                          </div>
                        )}
                        <input
                          ref={tagInputRef}
                          type="text"
                          value={newTagName}
                          onChange={(e) => setNewTagName(e.target.value)}
                          onKeyDown={(e) => e.key === "Enter" && handleCreatePreShareTag()}
                          placeholder={preShareTags.length === 0 ? dict.home.tagEnterHint : dict.home.tagAddPlaceholder}
                          maxLength={50}
                          className="text-sm bg-transparent border-none outline-none text-accent placeholder:text-accent/40 hover:placeholder:text-accent/70 w-full transition-colors"
                        />
                      </div>
                    </div>

                    {/* 비밀번호 */}
                    <div className="flex gap-3 items-center">
                      <span className="w-16 shrink-0 text-sm text-text-muted">{dict.home.passwordLabel}</span>
                      {!passwordOpen ? (
                        <button
                          type="button"
                          onClick={() => setPasswordOpen(true)}
                          className="text-sm bg-transparent border-none outline-none text-accent/40 hover:text-accent/80 transition-colors cursor-pointer"
                        >
                          {dict.home.passwordAdd}
                        </button>
                      ) : (
                        <div className="flex gap-2 items-center flex-1">
                          <input
                            type="text"
                            name="doc-pin"
                            autoComplete="off"
                            value={pendingPassword}
                            onChange={(e) => setPendingPassword(e.target.value)}
                            placeholder={dict.home.passwordPlaceholder}
                            className="flex-1 h-8 px-3 text-sm bg-bg-secondary border border-border-dark rounded-lg text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent/50 transition-colors [-webkit-text-security:disc]"
                          />
                          <button
                            type="button"
                            onClick={() => { setPasswordOpen(false); setPendingPassword(""); }}
                            className="shrink-0 text-sm text-text-muted hover:text-text-secondary transition-colors cursor-pointer"
                          >
                            {dict.publishModal.passwordCancel}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {error && (
                  <p className="text-center text-sm text-danger">{error}</p>
                )}

                {/* 공유하기 — 단독 풀-폭 CTA */}
                <button
                  onClick={(e) => { e.stopPropagation(); handlePublish(); }}
                  disabled={isPublishing}
                  className="w-full max-w-[400px] inline-flex cursor-pointer items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-accent to-accent-hover px-5 py-3.5 text-sm font-bold text-white shadow-[0_4px_20px_rgba(124,92,252,0.25)] transition-all hover:-translate-y-0.5 hover:shadow-[0_6px_28px_rgba(124,92,252,0.4)] disabled:opacity-50"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
                    <polyline points="16 6 12 2 8 6" />
                    <line x1="12" y1="2" x2="12" y2="15" />
                  </svg>
                  {isPublishing ? dict.publish.publishing : dict.publish.share}
                </button>

                {/* 서브 액션 행 — 미리보기 · 상태 */}
                <div className="flex items-center justify-center gap-3 text-sm">
                  <button
                    onClick={(e) => { e.stopPropagation(); handlePreview(); }}
                    className="inline-flex items-center gap-1.5 text-text-muted hover:text-text-primary transition-colors cursor-pointer"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    {dict.publish.preview}
                  </button>
                  <span className="text-border-dark/60 select-none">·</span>
                  {isAuthenticated ? (
                    <span className="inline-flex items-center gap-1 text-success/80">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6L9 17l-5-5" /></svg>
                      {dict.home.permanentNotice}
                    </span>
                  ) : (
                    <Link href="/auth/login" className="text-accent hover:underline">
                      {dict.publish.loginKeep}
                    </Link>
                  )}
                </div>
              </div>
            ) : (
              /* Empty state */
              <div className="relative flex flex-col items-center gap-3">
                <div className="text-base md:text-lg font-bold text-text-primary">
                  {dict.fileDrop.dropHere}
                </div>
                <div className="text-xs text-text-muted">{dict.fileDrop.pasteHint}</div>
                <p className="max-w-[320px] text-center text-xs leading-relaxed text-text-muted/70">
                  {dict.editor.homePlaceholder}
                </p>
                <button
                  onClick={(e) => { e.stopPropagation(); handleDropZoneClick(); }}
                  className="mt-1 rounded-lg border border-accent/30 bg-accent/10 px-4 py-2 text-sm font-semibold text-accent transition-colors hover:bg-accent/20 cursor-pointer"
                >
                  {dict.fileDrop.selectFile}
                </button>
                <div className="text-xs text-text-muted/60">
                  {dict.fileDrop.supportedFormats}
                </div>
              </div>
            )}
          </div>
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

function UseCaseIcon({ index }: { index: number }) {
  const icons = [
    // 0: AI / sparkles (Claude)
    <svg key={0} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-accent/70"><path d="M12 3l1.9 5.8L19.6 9l-4.6 3.4 1.7 5.8L12 15l-4.7 3.2 1.7-5.8L4.4 9l5.7-.2z"/></svg>,
    // 1: message bubble (ChatGPT)
    <svg key={1} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-accent/70"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>,
    // 2: code / terminal (Cursor)
    <svg key={2} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-accent/70"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>,
    // 3: users (team)
    <svg key={3} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-accent/70"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>,
    // 4: file-text (Markdown)
    <svg key={4} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-accent/70"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>,
  ];
  return icons[index % icons.length];
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
}
