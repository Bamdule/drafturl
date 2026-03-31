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
        <p className="text-text-secondary text-[clamp(14px,2vw,16px)] max-w-[500px] mx-auto">
          {dict.home.subtitle}
        </p>
      </section>

      {/* Main Editor Area */}
      <main className="max-w-[1280px] mx-auto w-full px-6 pb-12">
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

