"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useDict } from "@/components/i18n/DictProvider";
import type { DocumentSummary } from "@/lib/api/types";

interface PublishResultModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  document: DocumentSummary | null;
  isAuthenticated: boolean;
}

export default function PublishResultModal({
  open,
  onOpenChange,
  document: doc,
  isAuthenticated,
}: PublishResultModalProps) {
  const { dict } = useDict();
  const router = useRouter();
  const [copied, setCopied] = useState(false);

  if (!doc || !open) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(doc.url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback: textarea를 만들어 복사 시도
      try {
        const textarea = document.createElement("textarea");
        textarea.value = doc.url;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand("copy");
        document.body.removeChild(textarea);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch {
        // 최종 실패 — 사용자가 직접 복사하도록 안내
        // PublishResultModal은 URL이 이미 input에 표시되므로
        // 사용자에게 직접 선택/복사를 유도
      }
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/70 backdrop-blur-lg"
      onClick={() => onOpenChange(false)}
    >
      <div
        className="bg-bg-secondary border border-border-dark rounded-xl p-5 sm:p-8 max-w-[480px] w-full text-center"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Check icon */}
        <div className="w-14 h-14 rounded-full bg-success/10 flex items-center justify-center mx-auto mb-4 text-3xl text-success">
          &#10003;
        </div>

        <h2 className="text-xl font-bold text-text-primary mb-2">
          {dict.publishModal.title}
        </h2>
        <p className="text-sm text-text-secondary mb-5">
          {dict.publishModal.description}
        </p>

        {/* URL display */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 bg-bg-primary border border-border-dark rounded-lg px-3 sm:px-4 py-3 mb-4">
          <input
            readOnly
            value={doc.url}
            className="flex-1 bg-transparent border-none text-accent text-[13px] sm:text-[15px] font-medium font-mono outline-none min-w-0"
          />
          <button
            onClick={handleCopy}
            className="shrink-0 px-4 py-2 sm:py-1.5 text-sm font-semibold text-white bg-accent hover:bg-accent-hover rounded-md transition-colors cursor-pointer"
          >
            {copied ? dict.publishModal.copied : dict.publishModal.copy}
          </button>
        </div>

        {/* Login CTA — 비로그인 사용자만 */}
        {!isAuthenticated && (
          <div className="flex flex-col sm:flex-row items-center gap-2 sm:gap-3 rounded-lg border border-accent/20 bg-accent/5 px-4 py-3 mb-4">
            <p className="flex-1 text-sm text-text-secondary text-left">
              {dict.publishModal.loginBanner}
            </p>
            <button
              onClick={() => {
                sessionStorage.setItem("pendingClaimSlug", doc.slug);
                router.push("/auth/login");
              }}
              className="shrink-0 px-4 py-1.5 text-sm font-semibold text-white bg-accent hover:bg-accent-hover rounded-md transition-colors cursor-pointer"
            >
              {dict.publishModal.loginButton}
            </button>
          </div>
        )}

        {/* Actions */}
        <div className="flex flex-col-reverse sm:flex-row gap-2 mt-4">
          <button
            onClick={() => onOpenChange(false)}
            className="flex-1 inline-flex items-center justify-center px-4 py-3 sm:py-2.5 rounded-lg text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors cursor-pointer"
          >
            {dict.publishModal.close}
          </button>
          <a
            href={`/${doc.slug}`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 inline-flex items-center justify-center px-4 py-3 sm:py-2.5 rounded-lg text-sm font-medium text-text-primary border border-border-dark hover:border-border-dark-hover hover:bg-bg-tertiary transition-colors"
          >
            {dict.publishModal.viewDetail}
          </a>
        </div>
      </div>
    </div>
  );
}
