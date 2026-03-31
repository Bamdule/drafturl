"use client";

import { useCallback, useState } from "react";
import HtmlViewer from "@/components/viewer/HtmlViewer";
import MarkdownViewer from "@/components/viewer/MarkdownViewer";
import ReportDialog from "@/components/document/ReportDialog";
import type { DocumentView } from "@/lib/api/types";
import { useDict } from "@/components/i18n/DictProvider";

interface DocumentViewPageProps {
  document: DocumentView;
}

export default function DocumentViewPage({
  document: doc,
}: DocumentViewPageProps) {
  const { dict } = useDict();
  const [reportOpen, setReportOpen] = useState(false);
  const [reportSuccess, setReportSuccess] = useState(false);

  const handleReportSubmitted = useCallback(() => {
    setReportSuccess(true);
    setTimeout(() => setReportSuccess(false), 3000);
  }, []);

  return (
    <div className="min-h-screen">
      {/* Document Content - 풀스크린 */}
      <div className="h-screen w-full">
        {doc.docType === "html" ? (
          <HtmlViewer contentUrl={doc.contentUrl!} title={doc.title} />
        ) : (
          <MarkdownViewer contentUrl={doc.contentUrl!} title={doc.title} />
        )}
      </div>

      {/* 신고 버튼 - 하단 고정 */}
      <div className="fixed bottom-4 right-4 z-50">
        {reportSuccess && (
          <div className="mb-2 rounded-lg bg-bg-secondary border border-border-dark px-3 py-2 text-xs text-text-secondary shadow-lg">
            {dict.report.success}
          </div>
        )}
        <button
          type="button"
          onClick={() => setReportOpen(true)}
          className="text-xs text-text-muted/40 hover:text-text-muted transition-colors cursor-pointer"
        >
          {dict.report.button}
        </button>
      </div>

      <ReportDialog
        open={reportOpen}
        onOpenChange={setReportOpen}
        documentId={doc.id}
        onSubmitted={handleReportSubmitted}
      />
    </div>
  );
}
