"use client";

import { useDict } from "@/components/i18n/DictProvider";
import type { TagWithCount } from "@/lib/api/tags";

interface TagFilterProps {
  tags: TagWithCount[];
  selectedTagId: number | null;
  onSelect: (id: number | null) => void;
  onDelete?: (id: number) => void;
}

export default function TagFilter({
  tags,
  selectedTagId,
  onSelect,
  onDelete,
}: TagFilterProps) {
  const { dict } = useDict();

  const visibleTags = tags.filter(t => t.documentCount > 0);
  if (visibleTags.length === 0) return null;

  const chipBase =
    "group inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium transition-colors cursor-pointer border whitespace-nowrap";
  const chipSelected = "bg-accent text-white border-accent";
  const chipDefault =
    "bg-bg-secondary text-text-muted border-border-dark hover:border-accent/40 hover:text-text-secondary";

  return (
    <div className="flex flex-wrap gap-1.5">
      <button
        type="button"
        onClick={() => onSelect(null)}
        className={`${chipBase} ${selectedTagId === null ? chipSelected : chipDefault}`}
      >
        {dict.tag.allDocs}
      </button>
      {visibleTags.map((tag) => (
        <span
          key={tag.id}
          className={`${chipBase} ${selectedTagId === tag.id ? chipSelected : chipDefault}`}
        >
          <button
            type="button"
            onClick={() => onSelect(tag.id)}
            className="flex items-center gap-1.5 cursor-pointer bg-transparent border-none p-0"
          >
            {tag.name}
            <span className="opacity-0 group-hover:opacity-50 transition-opacity text-[10px]">{tag.documentCount}</span>
          </button>
          {onDelete && (
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); onDelete(tag.id); }}
              className="opacity-0 group-hover:opacity-60 hover:!opacity-100 transition-opacity cursor-pointer bg-transparent border-none p-0 leading-none"
              title="태그 삭제"
            >
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          )}
        </span>
      ))}
    </div>
  );
}
