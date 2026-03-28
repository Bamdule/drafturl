"use client";

import { useState, useRef, useEffect } from "react";
import { updateDocument } from "@/lib/api/documents";
import type { DocumentSummary } from "@/lib/api/types";

interface DocumentCardProps {
  document: DocumentSummary;
  onDelete: (slug: string) => void;
  onUpdate?: () => void;
}

function formatDateTime(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatRemaining(expiresAt: string): string {
  const now = new Date();
  const expiry = new Date(expiresAt);
  const diffMs = expiry.getTime() - now.getTime();

  if (diffMs <= 0) return "만료됨";

  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMin / 60);
  const remainMin = diffMin % 60;

  if (diffHour > 0) return `${diffHour}시간 ${remainMin}분 남음`;
  return `${diffMin}분 남음`;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function DocumentCard({
  document: doc,
  onDelete,
  onUpdate,
}: DocumentCardProps) {
  const [copied, setCopied] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(doc.title || "");
  const [saving, setSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  const handleTitleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setEditTitle(doc.title || "");
    setEditing(true);
  };

  const handleTitleSave = async () => {
    const newTitle = editTitle.trim();
    if (newTitle === (doc.title || "")) {
      setEditing(false);
      return;
    }
    setSaving(true);
    try {
      await updateDocument(doc.slug, { title: newTitle || undefined });
      setEditing(false);
      onUpdate?.();
    } catch {
      // 실패 시 원래 제목으로 복원
      setEditTitle(doc.title || "");
      setEditing(false);
    } finally {
      setSaving(false);
    }
  };

  const handleTitleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleTitleSave();
    } else if (e.key === "Escape") {
      setEditTitle(doc.title || "");
      setEditing(false);
    }
  };

  const handleCopyUrl = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await navigator.clipboard.writeText(doc.url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
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
        // ignore
      }
    }
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDelete(doc.slug);
  };

  const handleRowClick = () => {
    window.open(`/${doc.slug}`, "_blank", "noopener,noreferrer");
  };

  const isHtml = doc.docType === "html";
  const hasExpiry = !!doc.expiresAt;

  return (
    <div
      onClick={handleRowClick}
      className="bg-bg-secondary border border-border-dark rounded-xl px-4 sm:px-5 py-3 sm:py-4 grid grid-cols-[1fr_auto] gap-2 sm:gap-4 items-center hover:border-accent/40 hover:bg-bg-tertiary/50 transition-all cursor-pointer group"
    >
      <div className="flex flex-col gap-1.5 min-w-0">
        {/* Title row */}
        <div className="flex items-center gap-2">
          {editing ? (
            <input
              ref={inputRef}
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              onBlur={handleTitleSave}
              onKeyDown={handleTitleKeyDown}
              onClick={(e) => e.stopPropagation()}
              disabled={saving}
              placeholder="제목을 입력하세요"
              className="text-[15px] font-semibold text-text-primary bg-bg-tertiary border border-accent/50 rounded px-2 py-0.5 outline-none focus:border-accent w-48"
            />
          ) : (
            <span
              onClick={handleTitleClick}
              title="클릭하여 제목 수정"
              className="text-[15px] font-semibold text-text-primary truncate group-hover:text-accent transition-colors hover:underline hover:decoration-dotted hover:underline-offset-4 cursor-text"
            >
              {doc.title || "제목 없음"}
            </span>
          )}
          {doc.isPasswordProtected && (
            <span className="text-[11px] text-text-muted shrink-0" title="비밀번호 보호">&#128274;</span>
          )}
          <span
            className={`inline-flex px-2 py-0.5 rounded text-[11px] font-semibold uppercase tracking-wide shrink-0 ${
              isHtml
                ? "bg-[rgba(251,146,60,0.12)] text-[#fb923c]"
                : "bg-[rgba(96,165,250,0.12)] text-[#60a5fa]"
            }`}
          >
            {isHtml ? "HTML" : "MD"}
          </span>
          {hasExpiry && (
            <span className="text-[10px] text-warning flex items-center gap-1 shrink-0 bg-warning/10 px-1.5 py-0.5 rounded">
              ⏱ {formatRemaining(doc.expiresAt!)}
            </span>
          )}
        </div>
        {/* Meta row */}
        <div className="flex gap-4 text-xs text-text-muted">
          <span>{formatFileSize(doc.contentSize)}</span>
          <span>{formatDateTime(doc.createdAt)}</span>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-0.5 shrink-0">
        <ActionButton
          onClick={handleCopyUrl}
          title="URL 복사"
          icon={copied ? "check" : "link"}
        >
          {copied ? "복사됨" : "복사"}
        </ActionButton>
        <ActionButton
          onClick={handleDelete}
          title="삭제"
          icon="trash"
          danger
        >
          삭제
        </ActionButton>
      </div>
    </div>
  );
}

function ActionButton({
  children,
  onClick,
  title,
  danger,
  icon,
}: {
  children: React.ReactNode;
  onClick?: (e: React.MouseEvent) => void;
  title: string;
  danger?: boolean;
  icon: "link" | "check" | "trash";
}) {
  const iconMap = {
    link: (
      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101" />
        <path strokeLinecap="round" strokeLinejoin="round" d="M10.172 13.828a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.102 1.101" />
      </svg>
    ),
    check: (
      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
      </svg>
    ),
    trash: (
      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
      </svg>
    ),
  };

  return (
    <button
      onClick={onClick}
      title={title}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all cursor-pointer bg-transparent border border-transparent ${
        danger
          ? "text-text-muted hover:text-danger hover:bg-danger/10 hover:border-danger/20"
          : "text-text-muted hover:text-text-primary hover:bg-bg-tertiary hover:border-border-dark"
      }`}
    >
      {iconMap[icon]}
      <span className="hidden sm:inline">{children}</span>
    </button>
  );
}
