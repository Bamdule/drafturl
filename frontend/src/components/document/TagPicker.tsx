"use client";

import { useState, useRef, useEffect } from "react";
import { useDict } from "@/components/i18n/DictProvider";
import { createTag, addTagToDocument, removeTagFromDocument } from "@/lib/api/tags";
import type { Tag } from "@/lib/api/types";
import type { TagWithCount } from "@/lib/api/tags";

interface TagPickerProps {
  allTags: TagWithCount[];
  documentTags: Tag[];
  slug: string;
  onTagsChange: () => void;
  variant?: "icon" | "pill";
}

export default function TagPicker({
  allTags,
  documentTags,
  slug,
  onTagsChange,
  variant = "icon",
}: TagPickerProps) {
  const { dict } = useDict();
  const [open, setOpen] = useState(false);
  const [newTagName, setNewTagName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const docTagIds = new Set(documentTags.map((t) => t.id));
  const filteredTags = newTagName.trim()
    ? allTags.filter((t) =>
        t.name.toLowerCase().includes(newTagName.toLowerCase()),
      )
    : allTags;

  // 외부 클릭 닫기
  useEffect(() => {
    if (!open) return;
    const handleMouseDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [open]);

  const handleToggleTag = async (tagId: number) => {
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      if (docTagIds.has(tagId)) {
        await removeTagFromDocument(slug, tagId);
      } else {
        await addTagToDocument(slug, tagId);
      }
      onTagsChange();
    } catch {
      // 에러 무시 (API 에러는 콘솔에 로깅됨)
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTag = async () => {
    const name = newTagName.trim();
    if (!name) return;

    if (name.length > 50) {
      setError(dict.tag.maxLength);
      return;
    }

    const isDuplicate = allTags.some(
      (t) => t.name.toLowerCase() === name.toLowerCase(),
    );
    if (isDuplicate) {
      setError(dict.tag.duplicate);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const created = await createTag(name);
      await addTagToDocument(slug, created.id);
      setNewTagName("");
      onTagsChange();
    } catch {
      // 에러 무시
    } finally {
      setLoading(false);
    }
  };

  const handleCreateKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleCreateTag();
    }
  };

  return (
    <div className="relative" ref={ref}>
      {variant === "pill" ? (
        <button
          type="button"
          onClick={(e) => { e.stopPropagation(); setOpen((v) => !v); }}
          className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border border-dashed border-accent/40 text-accent/70 hover:border-accent hover:text-accent hover:bg-accent/10 transition-all cursor-pointer bg-transparent"
        >
          <span className="text-sm leading-none">+</span>
          {dict.tag.add}
        </button>
      ) : (
        <button
          type="button"
          onClick={(e) => { e.stopPropagation(); setOpen((v) => !v); }}
          title={dict.tag.add}
          className="inline-flex items-center justify-center w-5 h-5 rounded-full border border-dashed border-accent/30 text-accent/60 hover:border-accent hover:text-accent hover:bg-accent/10 transition-all cursor-pointer bg-transparent text-xs leading-none"
        >
          +
        </button>
      )}
      {open && (
        <div
          onClick={(e) => e.stopPropagation()}
          className="absolute left-0 top-full mt-1 z-50 bg-bg-secondary border border-border-dark rounded-lg shadow-lg shadow-black/30 py-1 min-w-[200px]"
        >
          {/* 기존 태그 목록 */}
          {filteredTags.length > 0 && (
            <div className="max-h-40 overflow-y-auto">
              {filteredTags.map((tag) => {
                const isAttached = docTagIds.has(tag.id);
                return (
                  <button
                    key={tag.id}
                    type="button"
                    onClick={() => handleToggleTag(tag.id)}
                    disabled={loading}
                    className="w-full flex items-center gap-2 px-3 py-1.5 text-xs text-text-secondary hover:bg-bg-tertiary transition-colors cursor-pointer bg-transparent border-none text-left disabled:opacity-50"
                  >
                    <span
                      className={`w-4 h-4 rounded border flex items-center justify-center text-[10px] shrink-0 ${
                        isAttached
                          ? "bg-accent border-accent text-white"
                          : "border-border-dark text-transparent"
                      }`}
                    >
                      &#10003;
                    </span>
                    <span className="truncate">{tag.name}</span>
                    <span className="ml-auto text-text-muted shrink-0">
                      {tag.documentCount}
                    </span>
                  </button>
                );
              })}
            </div>
          )}

          {/* 구분선 */}
          {filteredTags.length > 0 && (
            <div className="border-t border-border-dark my-1" />
          )}

          {/* 새 태그 생성 */}
          <div className="px-3 py-2">
            <div className="text-[10px] text-text-muted mb-1.5">
              {dict.tag.new}
            </div>
            <div className="flex items-center gap-1.5">
              <input
                type="text"
                value={newTagName}
                onChange={(e) => {
                  setNewTagName(e.target.value);
                  setError(null);
                }}
                onKeyDown={handleCreateKeyDown}
                placeholder={dict.tag.namePlaceholder}
                maxLength={50}
                className="flex-1 min-w-0 px-2 py-1 text-xs bg-bg-tertiary border border-border-dark rounded text-text-primary placeholder:text-text-muted outline-none focus:border-accent/50"
              />
              <button
                type="button"
                onClick={handleCreateTag}
                disabled={loading || !newTagName.trim()}
                className="shrink-0 px-2 py-1 text-xs rounded bg-accent text-white hover:bg-accent-hover transition-colors cursor-pointer border-none disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {dict.tag.create}
              </button>
            </div>
            {error && (
              <div className="text-[10px] text-danger mt-1">{error}</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
