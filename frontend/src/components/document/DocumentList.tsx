"use client";

import { useState } from "react";
import Link from "next/link";
import DocumentCard from "./DocumentCard";
import DeleteConfirmDialog from "./DeleteConfirmDialog";
import { deleteDocument } from "@/lib/api/documents";
import type { DocumentSummary, PaginationInfo } from "@/lib/api/types";

interface DocumentListProps {
  documents: DocumentSummary[];
  pagination: PaginationInfo;
  onPageChange: (page: number) => void;
  onDocumentDeleted: () => void;
}

export default function DocumentList({
  documents,
  pagination,
  onPageChange,
  onDocumentDeleted,
}: DocumentListProps) {
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await deleteDocument(deleteTarget);
      onDocumentDeleted();
    } catch (err) {
      console.error("문서 삭제 실패:", err);
    } finally {
      setIsDeleting(false);
      setDeleteTarget(null);
    }
  };

  if (documents.length === 0) {
    return (
      <div className="text-center py-16 text-text-muted">
        <div className="text-5xl opacity-30 mb-4">&#128196;</div>
        <h3 className="text-lg text-text-secondary mb-2">
          아직 문서가 없습니다
        </h3>
        <p className="text-sm mb-6">
          메인 페이지에서 새 문서를 만들어보세요.
        </p>
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-semibold text-white bg-accent hover:bg-accent-hover transition-colors no-underline"
        >
          + 새 문서 만들기
        </Link>
      </div>
    );
  }

  return (
    <>
      <div className="flex flex-col gap-2">
        {documents.map((doc) => (
          <DocumentCard
            key={doc.id}
            document={doc}
            onDelete={(slug) => setDeleteTarget(slug)}
            onUpdate={onDocumentDeleted}
          />
        ))}
      </div>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            disabled={pagination.page === 0}
            onClick={() => onPageChange(pagination.page - 1)}
            className="px-4 py-2 rounded-md text-sm font-medium text-text-primary border border-border-dark hover:bg-bg-tertiary transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed bg-transparent"
          >
            이전
          </button>
          <span className="text-sm text-text-muted">
            {pagination.page + 1} / {pagination.totalPages}
          </span>
          <button
            disabled={pagination.page >= pagination.totalPages - 1}
            onClick={() => onPageChange(pagination.page + 1)}
            className="px-4 py-2 rounded-md text-sm font-medium text-text-primary border border-border-dark hover:bg-bg-tertiary transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed bg-transparent"
          >
            다음
          </button>
        </div>
      )}

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        onConfirm={handleDelete}
        isDeleting={isDeleting}
      />
    </>
  );
}
