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

export default function HomePage() {
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

        {/* Feature Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-12">
          <FeatureCard
            icon={
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
              </svg>
            }
            title={dict.home.features.deploy.title}
          >
            {dict.home.features.deploy.description}
          </FeatureCard>
          <FeatureCard
            icon={
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            }
            title={dict.home.features.preview.title}
          >
            {dict.home.features.preview.description}
          </FeatureCard>
          <FeatureCard
            icon={
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
              </svg>
            }
            title={dict.home.features.url.title}
          >
            {dict.home.features.url.description}
          </FeatureCard>
          <FeatureCard
            icon={
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z" />
                <path d="M16 14H8a4 4 0 0 0-4 4v2h16v-2a4 4 0 0 0-4-4z" />
                <circle cx="9" cy="6" r="0.5" fill="currentColor" />
                <circle cx="15" cy="6" r="0.5" fill="currentColor" />
              </svg>
            }
            title={dict.home.features.ai.title}
          >
            {dict.home.features.ai.description}
          </FeatureCard>
        </div>

        {/* How to Use Section */}
        <section className="mt-16 mb-4">
          <h2 className="text-xl font-bold text-text-primary text-center mb-8">
            {dict.home.howToUse}
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <StepCard step={1} title={dict.home.steps.paste.title}>
              {dict.home.steps.paste.description}
            </StepCard>
            <StepCard step={2} title={dict.home.steps.check.title}>
              {dict.home.steps.check.description}
            </StepCard>
            <StepCard step={3} title={dict.home.steps.share.title}>
              {dict.home.steps.share.description}
            </StepCard>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-border-dark mt-12">
        <div className="max-w-screen-xl mx-auto px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-text-muted/60">
          <span>&copy; 2026 DraftURL</span>
          <div className="flex items-center gap-4">
            <a href="#" className="hover:text-text-muted transition-colors">{dict.home.footer.terms}</a>
            <a href="#" className="hover:text-text-muted transition-colors">{dict.home.footer.privacy}</a>
          </div>
        </div>
      </footer>
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

function FeatureCard({
  icon,
  title,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-bg-secondary border border-border-dark rounded-xl p-6 hover:border-border-dark-hover transition-colors">
      <div className="w-10 h-10 rounded-[10px] bg-accent-subtle flex items-center justify-center text-accent mb-3">
        {icon}
      </div>
      <h3 className="text-[15px] font-semibold mb-1.5 text-text-primary">
        {title}
      </h3>
      <p className="text-[13px] text-text-secondary leading-relaxed">
        {children}
      </p>
    </div>
  );
}

function StepCard({
  step,
  title,
  children,
}: {
  step: number;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="relative bg-bg-secondary border border-border-dark rounded-xl p-6">
      <div className="w-8 h-8 rounded-full bg-accent text-white flex items-center justify-center text-sm font-bold mb-3">
        {step}
      </div>
      <h3 className="text-[15px] font-semibold mb-1.5 text-text-primary">
        {title}
      </h3>
      <p className="text-[13px] text-text-secondary leading-relaxed">
        {children}
      </p>
    </div>
  );
}
