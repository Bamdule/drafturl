"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { updateDocument } from "@/lib/api/documents";
import { useDict } from "@/components/i18n/DictProvider";
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
  const { dict } = useDict();
  const [copied, setCopied] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(doc.title || "");
  const [saving, setSaving] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const shareRef = useRef<HTMLDivElement>(null);
  const moreRef = useRef<HTMLDivElement>(null);

  const formatRemaining = useCallback(
    (expiresAt: string): string => {
      const now = new Date();
      const expiry = new Date(expiresAt);
      const diffMs = expiry.getTime() - now.getTime();

      if (diffMs <= 0) return dict.document.expired;

      const diffMin = Math.floor(diffMs / 60000);
      const diffHour = Math.floor(diffMin / 60);
      const remainMin = diffMin % 60;

      if (diffHour > 0) return `${diffHour}${dict.document.hoursLeft} ${remainMin}${dict.document.minutesLeft}`;
      return `${diffMin}${dict.document.minutesLeft}`;
    },
    [dict],
  );

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  const handleEditClick = (e: React.MouseEvent) => {
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

  // 드롭다운 메뉴 외부 클릭 닫기
  useEffect(() => {
    if (!shareOpen && !moreOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (shareOpen && shareRef.current && !shareRef.current.contains(e.target as Node)) {
        setShareOpen(false);
      }
      if (moreOpen && moreRef.current && !moreRef.current.contains(e.target as Node)) {
        setMoreOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [shareOpen, moreOpen]);

  const handleShare = useCallback(async (e: React.MouseEvent) => {
    e.stopPropagation();
    // 모바일: Web Share API (네이티브 공유 시트 — 카카오톡, 메시지 등 포함)
    if (navigator.share) {
      try {
        await navigator.share({
          title: doc.title || "DraftURL",
          url: doc.url,
        });
        return;
      } catch {
        // 사용자가 취소한 경우 무시
      }
    }
    // 데스크톱: 공유 메뉴 토글
    setShareOpen((v) => !v);
  }, [doc.title, doc.url]);

  const shareToKakao = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShareOpen(false);
    window.open(
      `https://story.kakao.com/share?url=${encodeURIComponent(doc.url)}`,
      "_blank", "noopener,noreferrer,width=600,height=400"
    );
  };

  const shareToTwitter = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShareOpen(false);
    window.open(
      `https://twitter.com/intent/tweet?url=${encodeURIComponent(doc.url)}&text=${encodeURIComponent(doc.title || "DraftURL")}`,
      "_blank", "noopener,noreferrer,width=600,height=400"
    );
  };

  const shareToFacebook = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShareOpen(false);
    window.open(
      `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(doc.url)}`,
      "_blank", "noopener,noreferrer,width=600,height=400"
    );
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
              placeholder={dict.document.titlePlaceholder}
              className="text-[15px] font-semibold text-text-primary bg-bg-tertiary border border-accent/50 rounded px-2 py-0.5 outline-none focus:border-accent w-48"
            />
          ) : (
            <>
              <span className="text-[15px] font-semibold text-text-primary truncate group-hover:text-accent transition-colors">
                {doc.title || dict.document.noTitle}
              </span>
              <button
                onClick={handleEditClick}
                title={dict.document.editTitle}
                className="shrink-0 p-0.5 rounded text-text-muted hover:text-accent opacity-0 group-hover:opacity-100 transition-all cursor-pointer bg-transparent border-none"
              >
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
              </button>
            </>
          )}
          {doc.isPasswordProtected && (
            <span className="text-[11px] text-text-muted shrink-0" title={dict.document.passwordProtected}>&#128274;</span>
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
      <div className="flex items-center gap-1 shrink-0">
        <IconButton onClick={handleCopyUrl} title={copied ? dict.document.copied : dict.document.copyUrl} active={copied}>
          {copied ? <CheckIcon /> : <LinkIcon />}
        </IconButton>
        <div className="relative" ref={shareRef}>
          <IconButton onClick={handleShare} title={dict.document.share}>
            <ShareIcon />
          </IconButton>
          {shareOpen && (
            <div className="absolute right-0 top-full mt-1 z-50 bg-bg-secondary border border-border-dark rounded-lg shadow-lg shadow-black/30 py-1 min-w-[140px]">
              <ShareMenuItem onClick={shareToKakao} icon="kakao">{dict.document.kakao}</ShareMenuItem>
              <ShareMenuItem onClick={shareToTwitter} icon="twitter">{dict.document.twitter}</ShareMenuItem>
              <ShareMenuItem onClick={shareToFacebook} icon="facebook">{dict.document.facebook}</ShareMenuItem>
            </div>
          )}
        </div>
        <div className="relative" ref={moreRef}>
          <IconButton onClick={(e) => { e.stopPropagation(); setMoreOpen((v) => !v); }} title={dict.document.more}>
            <MoreIcon />
          </IconButton>
          {moreOpen && (
            <div className="absolute right-0 top-full mt-1 z-50 bg-bg-secondary border border-border-dark rounded-lg shadow-lg shadow-black/30 py-1 min-w-[120px]">
              <button
                onClick={(e) => { setMoreOpen(false); handleDelete(e); }}
                className="w-full flex items-center gap-2 px-3 py-2 text-xs text-danger hover:bg-danger/10 transition-colors cursor-pointer bg-transparent border-none text-left"
              >
                <TrashIcon />
                {dict.document.delete}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function IconButton({
  children,
  onClick,
  title,
  active,
}: {
  children: React.ReactNode;
  onClick: (e: React.MouseEvent) => void;
  title: string;
  active?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      title={title}
      className={`w-8 h-8 rounded-lg flex items-center justify-center transition-all cursor-pointer bg-transparent border border-transparent ${
        active
          ? "text-success bg-success/10"
          : "text-text-muted hover:text-text-primary hover:bg-bg-tertiary hover:border-border-dark"
      }`}
    >
      {children}
    </button>
  );
}

function LinkIcon() {
  return (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.172 13.828a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.102 1.101" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  );
}

function ShareIcon() {
  return (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13" />
    </svg>
  );
}

function MoreIcon() {
  return (
    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
      <circle cx="12" cy="5" r="1.5" />
      <circle cx="12" cy="12" r="1.5" />
      <circle cx="12" cy="19" r="1.5" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
    </svg>
  );
}

function ShareMenuItem({
  children,
  onClick,
  icon,
}: {
  children: React.ReactNode;
  onClick: (e: React.MouseEvent) => void;
  icon: "kakao" | "twitter" | "facebook";
}) {
  const icons = {
    kakao: (
      <svg className="w-4 h-4" viewBox="0 0 24 24" fill="#FEE500">
        <path d="M12 3C6.477 3 2 6.463 2 10.691c0 2.726 1.8 5.117 4.508 6.473-.144.522-.926 3.361-.958 3.569 0 0-.02.166.088.229.108.063.234.014.234.014.308-.043 3.574-2.34 4.137-2.738.638.094 1.295.143 1.991.143 5.523 0 10-3.463 10-7.691S17.523 3 12 3z" />
      </svg>
    ),
    twitter: (
      <svg className="w-4 h-4" viewBox="0 0 24 24" fill="#9090a8">
        <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
      </svg>
    ),
    facebook: (
      <svg className="w-4 h-4" viewBox="0 0 24 24" fill="#4267B2">
        <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
      </svg>
    ),
  };

  return (
    <button
      onClick={onClick}
      className="w-full flex items-center gap-2.5 px-3 py-2 text-xs text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors cursor-pointer bg-transparent border-none text-left"
    >
      {icons[icon]}
      {children}
    </button>
  );
}
