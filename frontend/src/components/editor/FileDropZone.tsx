"use client";

import { useCallback, useEffect, useState } from "react";
import { useEditorStore } from "@/lib/store/useEditorStore";
import { useDict } from "@/components/i18n/DictProvider";
import {
  FILE_EXTENSION_MAP,
  ALLOWED_EXTENSIONS,
  MAX_CONTENT_SIZE,
} from "@/lib/constants";
import type { DocType } from "@/lib/constants";

interface FileDropZoneProps {
  onFileDrop?: () => void;
}

export default function FileDropZone({ onFileDrop }: FileDropZoneProps) {
  const { dict } = useDict();
  const { setContent, setDocType } = useEditorStore();
  const [isDragOver, setIsDragOver] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: "success" | "error" } | null>(null);

  const processFile = useCallback(
    (file: File) => {
      if (file.size > MAX_CONTENT_SIZE) {
        setToast({ message: dict.fileDrop.fileSizeError, type: "error" });
        return;
      }

      const ext = file.name
        .slice(file.name.lastIndexOf("."))
        .toLowerCase();
      const docType = FILE_EXTENSION_MAP[ext] as DocType | undefined;

      if (!docType) {
        setToast({
          message: `${dict.fileDrop.unsupportedError} (${ALLOWED_EXTENSIONS.join(", ")})`,
          type: "error",
        });
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        setContent(text);
        setDocType(docType);
        setToast({ message: `${file.name} ${dict.fileDrop.loadSuccess}`, type: "success" });
        onFileDrop?.();
      };
      reader.onerror = () => {
        setToast({ message: dict.fileDrop.loadError, type: "error" });
      };
      reader.readAsText(file, "utf-8");
    },
    [setContent, setDocType, onFileDrop, dict],
  );

  // 전체 페이지 드래그앤드롭 이벤트
  useEffect(() => {
    let dragCounter = 0;

    const handleDragEnter = (e: DragEvent) => {
      e.preventDefault();
      dragCounter++;
      if (e.dataTransfer?.types.includes("Files")) {
        setIsDragOver(true);
      }
    };

    const handleDragLeave = (e: DragEvent) => {
      e.preventDefault();
      dragCounter--;
      if (dragCounter === 0) {
        setIsDragOver(false);
      }
    };

    const handleDragOver = (e: DragEvent) => {
      e.preventDefault();
    };

    const handleDrop = (e: DragEvent) => {
      e.preventDefault();
      dragCounter = 0;
      setIsDragOver(false);

      const file = e.dataTransfer?.files[0];
      if (file) processFile(file);
    };

    document.addEventListener("dragenter", handleDragEnter);
    document.addEventListener("dragleave", handleDragLeave);
    document.addEventListener("dragover", handleDragOver);
    document.addEventListener("drop", handleDrop);

    return () => {
      document.removeEventListener("dragenter", handleDragEnter);
      document.removeEventListener("dragleave", handleDragLeave);
      document.removeEventListener("dragover", handleDragOver);
      document.removeEventListener("drop", handleDrop);
    };
  }, [processFile]);

  // 토스트 자동 숨김
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(timer);
  }, [toast]);

  return (
    <>
      {/* 전체 페이지 드래그 오버레이 */}
      {isDragOver && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="flex flex-col items-center gap-4 rounded-2xl border-2 border-dashed border-accent bg-bg-secondary/90 px-16 py-12">
            <span className="text-5xl">📄</span>
            <p className="text-lg font-semibold text-text-primary">
              {dict.fileDrop.dropHere}
            </p>
            <p className="text-sm text-text-secondary">
              {dict.fileDrop.supportedFormats}
            </p>
          </div>
        </div>
      )}

      {/* 토스트 알림 */}
      {toast && (
        <div
          className={`fixed bottom-6 right-6 z-50 rounded-lg border px-5 py-3 text-sm shadow-[0_8px_32px_rgba(0,0,0,0.3)] transition-all duration-300 ${
            toast.type === "error"
              ? "border-danger/30 bg-danger/10 text-danger"
              : "border-border-dark bg-bg-elevated text-text-primary"
          }`}
        >
          {toast.message}
        </div>
      )}
    </>
  );
}
