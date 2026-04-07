"use client";

import type { Tag } from "@/lib/api/types";

interface TagBadgeProps {
  tag: Tag;
  onRemove?: () => void;
}

export default function TagBadge({ tag, onRemove }: TagBadgeProps) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-accent/10 border border-accent/20 px-2 py-0.5 text-xs text-accent">
      {tag.name}
      {onRemove && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          className="inline-flex items-center justify-center w-3.5 h-3.5 rounded-full hover:bg-accent/20 transition-colors cursor-pointer bg-transparent border-none text-accent text-[10px] leading-none"
          aria-label={`Remove tag ${tag.name}`}
        >
          &times;
        </button>
      )}
    </span>
  );
}
