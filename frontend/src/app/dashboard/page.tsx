"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import DocumentList from "@/components/document/DocumentList";
import StorageUsageBar from "@/components/common/StorageUsageBar";
import { getMyDocuments } from "@/lib/api/documents";
import { getMe } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/useAuthStore";
import type {
  DocumentSummary,
  PaginationInfo,
  StorageUsage,
} from "@/lib/api/types";
import { DEFAULT_PAGE_SIZE } from "@/lib/constants";

/** Free plan limits */
const FREE_PLAN_MAX_BYTES = 5 * 1024 * 1024;
const FREE_PLAN_MAX_DOCUMENTS = 30;
const FREE_PLAN_EXPIRY_HOURS = 24;

export default function DashboardPage() {
  const { user, setUser } = useAuthStore();
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [pagination, setPagination] = useState<PaginationInfo>({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [storageUsage, setStorageUsage] = useState<StorageUsage | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchDocuments = useCallback(async (page = 0) => {
    try {
      const data = await getMyDocuments({ page, size: DEFAULT_PAGE_SIZE });
      setDocuments(data.documents);
      setPagination(data.pagination);
    } catch (err) {
      console.error("문서 목록 조회 실패:", err);
    }
  }, []);

  const refreshUserInfo = useCallback(async () => {
    try {
      const userData = await getMe();
      if (userData) {
        setUser(userData);
        setStorageUsage(userData.storageUsage);
      }
    } catch (err) {
      console.error("사용자 정보 조회 실패:", err);
    }
  }, [setUser]);

  // 초기 로드: 문서 목록 + 사용자 정보 병렬 호출
  useEffect(() => {
    async function loadDashboard() {
      setLoading(true);
      await Promise.all([fetchDocuments(), refreshUserInfo()]);
      setLoading(false);
    }
    loadDashboard();
  }, [fetchDocuments, refreshUserInfo]);

  // AuthInitializer가 이미 가져온 storageUsage가 있으면 즉시 표시
  useEffect(() => {
    if (user && "storageUsage" in user && !storageUsage) {
      setStorageUsage((user as { storageUsage: StorageUsage }).storageUsage);
    }
  }, [user, storageUsage]);

  return (
    <div className="max-w-[960px] mx-auto px-6 py-8">
      {/* Stats Bar */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="bg-bg-secondary border border-border-dark rounded-xl p-5"
            >
              <div className="h-3 w-16 rounded bg-bg-tertiary animate-pulse mb-3" />
              <div className="h-8 w-24 rounded bg-bg-tertiary animate-pulse mb-2" />
              <div className="h-3 w-32 rounded bg-bg-tertiary animate-pulse" />
            </div>
          ))}
        </div>
      ) : (
        storageUsage && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8">
            <div className="bg-bg-secondary border border-border-dark rounded-xl p-5">
              <div className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                공유 문서
              </div>
              <div className="flex items-baseline gap-1">
                <span className="text-3xl font-bold text-text-primary">
                  {pagination.totalElements}
                </span>
                <span className="text-sm text-text-muted">/ {FREE_PLAN_MAX_DOCUMENTS}개</span>
              </div>
              {pagination.totalElements >= FREE_PLAN_MAX_DOCUMENTS && (
                <div className="text-xs text-warning mt-1">문서 한도에 도달했습니다</div>
              )}
            </div>
            <div className="bg-bg-secondary border border-border-dark rounded-xl p-5">
              <div className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                저장 용량
              </div>
              <StorageUsageBar
                usage={storageUsage}
                maxBytes={FREE_PLAN_MAX_BYTES}
              />
            </div>
            <div className="bg-bg-secondary border border-border-dark rounded-xl p-5">
              <div className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                플랜
              </div>
              <div className="text-xl font-bold text-text-primary">Free</div>
              <div className="text-xs text-text-muted mt-1 space-y-0.5">
                <div>문서 최대 {FREE_PLAN_MAX_DOCUMENTS}개</div>
                <div>로그인 시 영구 보관</div>
                <div>용량 제한: 5MB</div>
              </div>
            </div>
          </div>
        )
      )}

      {/* Page Header */}
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-[22px] font-bold text-text-primary">
          내 문서{" "}
          {!loading && (
            <span className="text-base font-normal text-text-muted ml-2">
              {pagination.totalElements}개
            </span>
          )}
        </h1>
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-semibold text-white bg-accent hover:bg-accent-hover transition-colors no-underline"
        >
          + 새 문서 만들기
        </Link>
      </div>

      {/* Document List */}
      <div>
        {loading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="bg-bg-secondary border border-border-dark rounded-xl p-5"
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="h-4 w-40 rounded bg-bg-tertiary animate-pulse" />
                  <div className="h-5 w-16 rounded bg-bg-tertiary animate-pulse" />
                </div>
                <div className="h-3 w-56 rounded bg-bg-tertiary animate-pulse mb-2" />
                <div className="h-3 w-32 rounded bg-bg-tertiary animate-pulse" />
              </div>
            ))}
          </div>
        ) : (
          <DocumentList
            documents={documents}
            pagination={pagination}
            onPageChange={(page) => fetchDocuments(page)}
            onDocumentDeleted={() => {
              fetchDocuments(pagination.page);
              refreshUserInfo();
            }}
          />
        )}
      </div>
    </div>
  );
}
