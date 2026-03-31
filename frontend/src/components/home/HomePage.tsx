"use client";

import { useState, useCallback } from "react";
import Header from "@/components/layout/Header";
import EditorPanel from "@/components/editor/EditorPanel";
import PreviewPanel from "@/components/editor/PreviewPanel";
import FileDropZone from "@/components/editor/FileDropZone";
import PublishButton from "@/components/common/PublishButton";
import TypewriterOverlay from "@/components/editor/TypewriterOverlay";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useDict } from "@/components/i18n/DictProvider";
import type { DocType } from "@/lib/constants";

export default function HomePage({ children }: { children?: React.ReactNode }) {
  const { dict, locale } = useDict();
  const { docType, setDocType, setContent } = useEditorStore();
  const [typewriterActive, setTypewriterActive] = useState(true);
  const [mobilePanel, setMobilePanel] = useState<"editor" | "preview">("editor");

  const handleTabChange = (type: DocType) => {
    setContent("");
    setDocType(type);
    setTypewriterActive(true);
  };

  const handleTypewriterDismiss = useCallback(() => {
    setTypewriterActive(false);
  }, []);

  const handleNewDocument = useCallback(() => {
    setTypewriterActive(true);
  }, []);

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary" lang={locale}>
      {/* 전체 페이지 드래그앤드롭 오버레이 */}
      <FileDropZone onFileDrop={() => setTypewriterActive(false)} />

      <Header />

      {/* Hero Section */}
      <section className="relative text-center px-6 pt-8 pb-6">
        {/* Glow effect */}
        <div className="pointer-events-none absolute top-[-100px] left-1/2 -translate-x-1/2 w-[600px] h-[400px] bg-[radial-gradient(ellipse,var(--color-accent-glow),transparent_70%)]" />
        <h1 className="relative text-[clamp(24px,4vw,36px)] font-extrabold tracking-tight mb-3">
          {dict.home.title}{" "}
          <span className="gradient-text">{dict.home.titleHighlight}</span>
          {dict.home.titleSuffix ? ` ${dict.home.titleSuffix}` : ""}
        </h1>
        <p className="text-text-secondary text-[clamp(14px,2vw,16px)] max-w-[500px] mx-auto mb-5">
          {dict.home.subtitle}
        </p>
        <a
          href="#editor"
          className="relative inline-flex items-center gap-2 rounded-lg bg-accent px-5 py-2.5 text-sm font-semibold text-white hover:bg-accent/90 transition-colors"
        >
          {dict.home.heroCta}
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" /></svg>
        </a>
      </section>

      {/* Main Editor Area */}
      <main id="editor" className="max-w-[1280px] mx-auto w-full px-6 pb-12 scroll-mt-4">
        {/* Drop Banner */}
        <div className="mx-auto mb-3 max-w-[640px] flex items-center gap-2.5 rounded-[10px] border border-dashed border-accent/40 bg-accent/[0.04] px-4 py-2.5 cursor-default">
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="var(--color-accent)"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="shrink-0"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <span
            className="text-xs text-text-secondary [&_strong]:text-text-primary [&_strong]:font-semibold"
            dangerouslySetInnerHTML={{ __html: dict.home.dropBanner }}
          />
          <span className="ml-auto shrink-0 text-[10px] text-text-muted">
            {dict.home.dropBannerSize}
          </span>
        </div>

        {/* Editor Tabs */}
        <div className="flex bg-bg-secondary border border-border-dark border-b-0 rounded-t-xl overflow-hidden">
          <TabButton
            active={docType === "html"}
            onClick={() => handleTabChange("html")}
          >
            HTML
          </TabButton>
          <TabButton
            active={docType === "markdown"}
            onClick={() => handleTabChange("markdown")}
          >
            Markdown
          </TabButton>
        </div>

        {/* Mobile panel toggle */}
        <div className="flex md:hidden border border-border-dark border-b-0 border-t-0">
          <button
            onClick={() => setMobilePanel("editor")}
            className={`flex-1 py-2 text-xs font-medium transition-colors cursor-pointer ${
              mobilePanel === "editor"
                ? "text-text-primary bg-bg-tertiary"
                : "text-text-muted hover:text-text-secondary"
            }`}
          >
            {dict.home.mobileEditor}
          </button>
          <button
            onClick={() => setMobilePanel("preview")}
            className={`flex-1 py-2 text-xs font-medium transition-colors cursor-pointer ${
              mobilePanel === "preview"
                ? "text-text-primary bg-bg-tertiary"
                : "text-text-muted hover:text-text-secondary"
            }`}
          >
            {dict.home.mobilePreview}
          </button>
        </div>

        {/* Editor Split View */}
        <div className="relative border border-border-dark rounded-b-xl overflow-hidden">
          {/* Desktop: side by side */}
          <div className="hidden md:grid md:grid-cols-2 min-h-[420px]">
            <EditorPanel />
            <PreviewPanel />
          </div>
          {/* Mobile: tab switch */}
          <div className="md:hidden min-h-[300px]">
            <div className={mobilePanel === "editor" ? "" : "hidden"}>
              <EditorPanel />
            </div>
            <div className={mobilePanel === "preview" ? "" : "hidden"}>
              <PreviewPanel />
            </div>
          </div>

          {/* 타이핑 애니메이션 오버레이 */}
          <TypewriterOverlay
            active={typewriterActive}
            onDismiss={handleTypewriterDismiss}
          />
        </div>

        {/* Publish Area */}
        <PublishButton onNewDocument={handleNewDocument} />
      </main>

      {children}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`relative px-5 py-2.5 text-sm font-medium transition-colors cursor-pointer border-none bg-transparent ${
        active
          ? "text-text-primary bg-bg-tertiary"
          : "text-text-muted hover:text-text-secondary hover:bg-bg-tertiary"
      }`}
    >
      {children}
      {active && (
        <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-accent" />
      )}
    </button>
  );
}

