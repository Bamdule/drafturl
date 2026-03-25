"use client";

interface HtmlViewerProps {
  content: string;
  title?: string | null;
}

export default function HtmlViewer({ content, title }: HtmlViewerProps) {
  return (
    <div className="flex h-full w-full flex-col">
      <iframe
        srcDoc={content}
        className="flex-1 w-full bg-white"
        sandbox="allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox"
        title={title ?? "문서"}
      />
    </div>
  );
}
