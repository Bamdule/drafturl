"use client";

import { useState } from "react";
import { useDict } from "@/components/i18n/DictProvider";
import { updateDocument } from "@/lib/api/documents";
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
  const [copied, setCopied] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordSaved, setPasswordSaved] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const handleSetPassword = async () => {
    if (!doc || password.trim().length < 4) {
      setPasswordError(dict.publishModal.passwordMinLength);
      return;
    }
    setPasswordSaving(true);
    setPasswordError(null);
    try {
      await updateDocument(doc.slug, { password: password.trim() });
      setPasswordSaved(true);
      setPasswordOpen(false);
    } catch {
      setPasswordError(dict.publishModal.passwordError);
    } finally {
      setPasswordSaving(false);
    }
  };

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
      className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/70 backdrop-blur-lg"
      onClick={() => onOpenChange(false)}
    >
      <div
        className="bg-bg-secondary border border-border-dark rounded-xl p-8 max-w-[480px] w-full text-center"
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
          {!isAuthenticated && dict.publishModal.expiryNotice}
        </p>

        {/* URL display */}
        <div className="flex items-center gap-2 bg-bg-primary border border-border-dark rounded-lg px-4 py-3 mb-4">
          <input
            readOnly
            value={doc.url}
            className="flex-1 bg-transparent border-none text-accent text-[15px] font-medium font-mono outline-none min-w-0"
          />
          <button
            onClick={handleCopy}
            className="px-3 py-1.5 text-sm font-semibold text-white bg-accent hover:bg-accent-hover rounded-md transition-colors cursor-pointer"
          >
            {copied ? dict.publishModal.copied : dict.publishModal.copy}
          </button>
        </div>

        {/* Password setting — 로그인 사용자만 */}
        {isAuthenticated && !passwordSaved && !passwordOpen && (
          <button
            onClick={() => setPasswordOpen(true)}
            className="inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text-secondary transition-colors cursor-pointer mb-2"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
            {dict.publishModal.addPassword}
          </button>
        )}
        {isAuthenticated && passwordOpen && (
          <div className="flex items-center gap-2 mb-2">
            <input
              type="text"
              name="doc-pin"
              autoComplete="off"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={dict.publishModal.passwordPlaceholder}
              className="h-9 flex-1 rounded-md border border-border-dark bg-bg-primary px-3 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-1 focus:ring-accent/50 [-webkit-text-security:disc]"
              onKeyDown={(e) => e.key === "Enter" && handleSetPassword()}
            />
            <button
              onClick={handleSetPassword}
              disabled={passwordSaving}
              className="px-3 py-2 text-sm font-medium text-white bg-accent hover:bg-accent-hover rounded-md transition-colors cursor-pointer disabled:opacity-50"
            >
              {passwordSaving ? "..." : dict.publishModal.passwordConfirm}
            </button>
            <button
              onClick={() => { setPasswordOpen(false); setPassword(""); setPasswordError(null); }}
              className="px-2 py-2 text-sm text-text-muted hover:text-text-secondary transition-colors cursor-pointer"
            >
              {dict.publishModal.passwordCancel}
            </button>
          </div>
        )}
        {passwordError && (
          <p className="text-xs text-danger mb-2">{passwordError}</p>
        )}
        {passwordSaved && (
          <p className="inline-flex items-center gap-1.5 text-sm text-success mb-2">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
            {dict.publishModal.passwordSet}
          </p>
        )}

        {/* Actions */}
        <div className="flex gap-2 mt-4">
          <a
            href={`/${doc.slug}`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 inline-flex items-center justify-center px-4 py-2.5 rounded-md text-sm font-medium text-text-primary border border-border-dark hover:border-border-dark-hover hover:bg-bg-tertiary transition-colors"
          >
            {dict.publishModal.viewDetail}
          </a>
          <button
            onClick={() => onOpenChange(false)}
            className="flex-1 inline-flex items-center justify-center px-4 py-2.5 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors cursor-pointer"
          >
            {dict.publishModal.close}
          </button>
        </div>
      </div>
    </div>
  );
}
