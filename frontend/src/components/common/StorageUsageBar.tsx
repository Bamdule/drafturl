"use client";

import { useDict } from "@/components/i18n/DictProvider";
import type { StorageUsage } from "@/lib/api/types";

interface StorageUsageBarProps {
  usage: StorageUsage;
  /** Max storage in bytes. Free plan: 5MB */
  maxBytes: number;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function StorageUsageBar({
  usage,
  maxBytes,
}: StorageUsageBarProps) {
  const { dict } = useDict();
  const percentage = Math.min(
    (usage.totalBytes / maxBytes) * 100,
    100,
  );

  return (
    <div>
      <div className="text-3xl font-bold text-text-primary">
        {formatBytes(usage.totalBytes)}
        <span className="text-base font-normal text-text-muted ml-1">
          / {formatBytes(maxBytes)}
        </span>
      </div>
      <div className="text-xs text-text-muted mt-1">
        {usage.documentCount}{dict.storageUsage.docs}
      </div>
      <div className="w-full h-1.5 bg-bg-tertiary rounded-full mt-2 overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-accent to-[#60a5fa] rounded-full transition-all"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
