"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import EditorPanel from "@/components/editor/EditorPanel";
import PreviewPanel from "@/components/editor/PreviewPanel";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useDict } from "@/components/i18n/DictProvider";
import { getDocument, updateDocument } from "@/lib/api/documents";
import type { DocType } from "@/lib/constants";

export default function EditDocumentPage() {
  const { dict } = useDict();
  const params = useParams<{ slug: string }>();
  const router = useRouter();
  const { content, title, docType, loadDocument, setTitle, reset } =
    useEditorStore();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Track saved content for unsaved changes detection
  const savedContentRef = useRef<string>("");
  const savedTitleRef = useRef<string>("");

  useEffect(() => {
    reset();

    async function load() {
      try {
        const doc = await getDocument(params.slug);
        loadDocument(doc.content, doc.docType as DocType, doc.title ?? "");
        savedContentRef.current = doc.content;
        savedTitleRef.current = doc.title ?? "";
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : dict.editor.loadError,
        );
      } finally {
        setLoading(false);
      }
    }
    load();

    return () => {
      reset();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.slug]);

  // Unsaved changes warning via beforeunload
  const hasUnsavedChanges = useCallback(() => {
    return (
      content !== savedContentRef.current ||
      title !== savedTitleRef.current
    );
  }, [content, title]);

  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges()) {
        e.preventDefault();
      }
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [hasUnsavedChanges]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setSaveSuccess(false);

    try {
      await updateDocument(params.slug, {
        content,
        title: title || undefined,
      });
      savedContentRef.current = content;
      savedTitleRef.current = title;
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : dict.editor.saveError,
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <p className="text-text-muted">{dict.editor.loading}</p>
      </div>
    );
  }

  if (error && !content) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-bg-primary">
        <p className="text-danger">{error}</p>
        <button
          onClick={() => router.push("/dashboard")}
          className="px-4 py-2 rounded-md text-sm font-medium text-text-primary border border-border-dark hover:bg-bg-tertiary transition-colors cursor-pointer"
        >
          {dict.editor.backToDashboard}
        </button>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      {/* Edit Header */}
      <div className="border-b border-border-dark bg-bg-secondary px-4 py-3">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-2 sm:gap-3">
            <button
              onClick={() => router.push("/dashboard")}
              className="px-3 py-1.5 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors cursor-pointer bg-transparent border-none"
            >
              &larr; {dict.editor.back}
            </button>
            <input
              placeholder={dict.editor.titlePlaceholder}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-40 sm:w-64 h-9 rounded-md border border-border-dark bg-bg-primary px-3 py-1.5 text-sm text-text-primary placeholder:text-text-muted outline-none focus:border-accent"
            />
            <span
              className={`inline-flex px-2 py-0.5 rounded text-[11px] font-semibold uppercase tracking-wide ${
                docType === "html"
                  ? "bg-[rgba(251,146,60,0.12)] text-[#fb923c]"
                  : "bg-[rgba(96,165,250,0.12)] text-[#60a5fa]"
              }`}
            >
              {docType === "html" ? "HTML" : "Markdown"}
            </span>
          </div>
          <div className="flex items-center gap-2">
            {error && (
              <span className="hidden text-sm text-danger sm:inline">
                {error}
              </span>
            )}
            {saveSuccess && (
              <span className="text-sm text-success">{dict.editor.saved}</span>
            )}
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-4 py-2 rounded-md text-sm font-semibold text-white bg-accent hover:bg-accent-hover transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {saving ? dict.editor.saving : dict.editor.save}
            </button>
          </div>
        </div>
      </div>

      {/* Editor + Preview */}
      <div className="flex flex-1 max-w-7xl mx-auto w-full">
        <div className="flex-1 min-h-[500px] border-r border-border-dark">
          <EditorPanel />
        </div>
        <div className="hidden sm:flex flex-1 min-h-[500px] flex-col">
          <PreviewPanel />
        </div>
      </div>
    </div>
  );
}
