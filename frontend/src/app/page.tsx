"use client";

import { useState, useCallback } from "react";
import Header from "@/components/layout/Header";
import EditorPanel from "@/components/editor/EditorPanel";
import PreviewPanel from "@/components/editor/PreviewPanel";
import FileDropZone from "@/components/editor/FileDropZone";
import PublishButton from "@/components/common/PublishButton";
import TypewriterOverlay from "@/components/editor/TypewriterOverlay";
import { useEditorStore } from "@/lib/store/useEditorStore";
import type { DocType } from "@/lib/constants";

export default function HomePage() {
  const { docType, setDocType, setContent } = useEditorStore();
  const [typewriterActive, setTypewriterActive] = useState(true);

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
    <div className="flex min-h-screen flex-col bg-bg-primary">
      {/* 전체 페이지 드래그앤드롭 오버레이 */}
      <FileDropZone onFileDrop={() => setTypewriterActive(false)} />

      <Header />

      {/* Hero Section */}
      <section className="relative text-center px-6 pt-12 pb-8">
        {/* Glow effect */}
        <div className="pointer-events-none absolute top-[-100px] left-1/2 -translate-x-1/2 w-[600px] h-[400px] bg-[radial-gradient(ellipse,var(--color-accent-glow),transparent_70%)]" />
        <h1 className="relative text-[clamp(24px,4vw,36px)] font-extrabold tracking-tight mb-3">
          LLM이 만든 문서를{" "}
          <span className="gradient-text">3초 만에</span> 공유하세요
        </h1>
        <p className="text-text-secondary text-[clamp(14px,2vw,16px)] max-w-[500px] mx-auto">
          HTML이나 Markdown을 붙여넣으면 즉시 공유 URL이 생성됩니다. 가입도
          필요 없어요.
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

        {/* Editor Split View */}
        <div className="relative grid grid-cols-1 md:grid-cols-2 border border-border-dark rounded-b-xl overflow-hidden min-h-[600px]">
          <EditorPanel />
          <PreviewPanel />

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
          <FeatureCard icon={"\u26A1"} title="3초 배포">
            붙여넣고 버튼 하나면 끝. 가입, 설정, 빌드 없이 즉시 URL이
            생성됩니다.
          </FeatureCard>
          <FeatureCard icon={"\uD83D\uDC40"} title="실시간 미리보기">
            코드를 편집하면 즉시 결과를 확인할 수 있습니다. HTML과 Markdown 모두
            지원합니다.
          </FeatureCard>
          <FeatureCard icon={"\uD83D\uDD17"} title="깔끔한 URL">
            drafturl.com/xK9mP2nQ 형태의 간결하고 공유하기 쉬운 URL을
            발급합니다.
          </FeatureCard>
          <FeatureCard icon={"\uD83E\uDD16"} title="AI 수정 (Coming Soon)">
            곧 AI에게 &quot;표를 보기 좋게 정리해줘&quot; 같은 수정 요청이
            가능해집니다.
          </FeatureCard>
        </div>
      </main>
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
  icon: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-bg-secondary border border-border-dark rounded-xl p-6 hover:border-border-dark-hover transition-colors">
      <div className="w-10 h-10 rounded-[10px] bg-accent-subtle flex items-center justify-center text-lg mb-3">
        <span>{icon}</span>
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
