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
          HTML이나 Markdown을 붙여넣거나 파일을 드래그앤드롭하면 즉시 공유 URL이 생성됩니다. 가입도 필요 없어요.
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
            편집기
          </button>
          <button
            onClick={() => setMobilePanel("preview")}
            className={`flex-1 py-2 text-xs font-medium transition-colors cursor-pointer ${
              mobilePanel === "preview"
                ? "text-text-primary bg-bg-tertiary"
                : "text-text-muted hover:text-text-secondary"
            }`}
          >
            미리보기
          </button>
        </div>

        {/* Editor Split View */}
        <div className="relative border border-border-dark rounded-b-xl overflow-hidden">
          {/* Desktop: side by side */}
          <div className="hidden md:grid md:grid-cols-2 min-h-[600px]">
            <EditorPanel />
            <PreviewPanel />
          </div>
          {/* Mobile: tab switch */}
          <div className="md:hidden min-h-[400px]">
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
            title="3초 배포"
          >
            붙여넣고 버튼 하나면 끝. 가입, 설정, 빌드 없이 즉시 URL이
            생성됩니다.
          </FeatureCard>
          <FeatureCard
            icon={
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            }
            title="실시간 미리보기"
          >
            코드를 편집하면 즉시 결과를 확인할 수 있습니다. HTML과 Markdown 모두
            지원합니다.
          </FeatureCard>
          <FeatureCard
            icon={
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
              </svg>
            }
            title="깔끔한 URL"
          >
            drafturl.com/xK9mP2nQ 형태의 간결하고 공유하기 쉬운 URL을
            발급합니다.
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
            title="AI 수정 (Coming Soon)"
          >
            곧 AI에게 &quot;표를 보기 좋게 정리해줘&quot; 같은 수정 요청이
            가능해집니다.
          </FeatureCard>
        </div>

        {/* How to Use Section */}
        <section className="mt-16 mb-4">
          <h2 className="text-xl font-bold text-text-primary text-center mb-8">
            이렇게 사용하세요
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <StepCard step={1} title="붙여넣기 또는 드래그앤드롭">
              ChatGPT, Claude 등 LLM이 생성한 HTML이나 Markdown을 에디터에
              붙여넣거나, .html/.md 파일을 화면에 드래그앤드롭하세요.
            </StepCard>
            <StepCard step={2} title="미리보기 확인">
              오른쪽 미리보기에서 결과를 실시간으로 확인하고, 필요하면 바로
              수정하세요.
            </StepCard>
            <StepCard step={3} title="공유 URL 발급">
              공유하기 버튼 한 번이면 끝. 받은 URL을 누구에게나 보낼 수 있습니다.
            </StepCard>
          </div>
        </section>
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
