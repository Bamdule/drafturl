"use client";

import HtmlViewer from "@/components/viewer/HtmlViewer";
import MarkdownViewer from "@/components/viewer/MarkdownViewer";
import type { DocumentView } from "@/lib/api/types";

interface DocumentViewPageProps {
  document: DocumentView;
}

export default function DocumentViewPage({
  document: doc,
}: DocumentViewPageProps) {
  return (
    <div className="min-h-screen">
      {/* Document Content - 풀스크린 */}
      <div className="h-screen w-full">
        {doc.docType === "html" ? (
          <HtmlViewer content={doc.content} title={doc.title} />
        ) : (
          <MarkdownViewer content={doc.content} title={doc.title} />
        )}
      </div>
    </div>
  );
}
