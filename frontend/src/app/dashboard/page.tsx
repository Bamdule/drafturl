"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import Link from "next/link";
import DocumentList from "@/components/document/DocumentList";
import TagFilter from "@/components/document/TagFilter";
import StorageUsageBar from "@/components/common/StorageUsageBar";
import { getMyDocuments } from "@/lib/api/documents";
import { getMyTags, deleteTag } from "@/lib/api/tags";
import { getMe } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { useDict } from "@/components/i18n/DictProvider";
import type {
  DocumentSummary,
  PaginationInfo,
  StorageUsage,
} from "@/lib/api/types";
import type { TagWithCount } from "@/lib/api/tags";
import { DEFAULT_PAGE_SIZE } from "@/lib/constants";

/** Free plan limits */
const FREE_PLAN_MAX_BYTES = 5 * 1024 * 1024;
const FREE_PLAN_MAX_DOCUMENTS = 30;

export default function DashboardPage() {
  const { dict } = useDict();
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
  const [allTags, setAllTags] = useState<TagWithCount[]>([]);
  const [search, setSearch] = useState("");
  const [selectedTagId, setSelectedTagId] = useState<number | null>(null);
  const [sort, setSort] = useState<string>("createdAt");
  const [sortOpen, setSortOpen] = useState(false);
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const sortRef = useRef<HTMLDivElement>(null);

  const fetchDocuments = useCallback(
    async (
      page = 0,
      overrides?: { search?: string; tagId?: number | null; sort?: string },
    ) => {
      const s = overrides?.search ?? search;
      const t =
        overrides && "tagId" in overrides
          ? overrides.tagId
          : selectedTagId;
      const sortVal = overrides?.sort ?? sort;
      try {
        const data = await getMyDocuments({
          page,
          size: DEFAULT_PAGE_SIZE,
          search: s || undefined,
          tagId: t ?? undefined,
          sort: sortVal,
        });
        setDocuments(data.documents);
        setPagination(data.pagination);
      } catch (err) {
        console.error("문서 목록 조회 실패:", err);
      }
    },
    [search, selectedTagId, sort],
  );

  const fetchTags = useCallback(async () => {
    try {
      const tags = await getMyTags();
      setAllTags(tags);
    } catch (err) {
      console.error("태그 목록 조회 실패:", err);
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

  // 초기 로드: 문서 목록 + 사용자 정보 + 태그 병렬 호출 (1회만 실행)
  useEffect(() => {
    async function loadDashboard() {
      setLoading(true);
      await Promise.all([fetchDocuments(), refreshUserInfo(), fetchTags()]);
      setLoading(false);
    }
    loadDashboard();
    // fetchDocuments는 search/selectedTagId 변경 시 참조가 바뀌므로 의존성에서 제외.
    // 이후 재조회는 handleSearchChange / handleTagSelect에서 직접 호출한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 검색어 변경 시 debounce 300ms
  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (searchTimerRef.current) {
      clearTimeout(searchTimerRef.current);
    }
    searchTimerRef.current = setTimeout(() => {
      fetchDocuments(0, { search: value });
    }, 300);
  };

  // 정렬 변경
  const handleSortChange = (value: string) => {
    setSort(value);
    fetchDocuments(0, { sort: value });
  };

  // 태그 필터 변경
  const handleTagSelect = (tagId: number | null) => {
    setSelectedTagId(tagId);
    fetchDocuments(0, { tagId });
  };

  // 태그 변경 시 태그 + 문서 목록 새로고침
  const handleTagsChange = () => {
    fetchTags();
    fetchDocuments(pagination.page);
  };

  // 태그 영구 삭제
  const handleDeleteTag = async (tagId: number) => {
    try {
      await deleteTag(tagId);
      if (selectedTagId === tagId) {
        setSelectedTagId(null);
        fetchDocuments(0, { tagId: undefined });
      }
      fetchTags();
    } catch (err) {
      console.error("태그 삭제 실패:", err);
    }
  };

  // 정렬 드롭다운 외부 클릭 닫기
  useEffect(() => {
    if (!sortOpen) return;
    const handler = (e: MouseEvent) => {
      if (sortRef.current && !sortRef.current.contains(e.target as Node)) {
        setSortOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [sortOpen]);

  const sortLabels: Record<string, string> = {
    createdAt: "최신순",
    title: "제목순",
    size: "크기순",
  };

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
                {dict.dashboard.sharedDocs}
              </div>
              <div className="flex items-baseline gap-1 mb-2">
                <span className="text-3xl font-bold text-text-primary">
                  {pagination.totalElements}
                </span>
                <span className="text-sm text-text-muted">/ {FREE_PLAN_MAX_DOCUMENTS}</span>
              </div>
              {/* progress bar */}
              <div className="h-1 bg-bg-tertiary rounded-full overflow-hidden">
                <div
                  className="h-full bg-accent rounded-full transition-all"
                  style={{ width: `${Math.min((pagination.totalElements / FREE_PLAN_MAX_DOCUMENTS) * 100, 100)}%` }}
                />
              </div>
              {pagination.totalElements >= FREE_PLAN_MAX_DOCUMENTS && (
                <div className="text-xs text-warning mt-1">{dict.dashboard.limitReached}</div>
              )}
            </div>
            <div className="bg-bg-secondary border border-border-dark rounded-xl p-5">
              <div className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                {dict.dashboard.storage}
              </div>
              <StorageUsageBar
                usage={storageUsage}
                maxBytes={FREE_PLAN_MAX_BYTES}
              />
            </div>
            <div className="bg-bg-secondary border border-border-dark rounded-xl p-5">
              <div className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                {dict.dashboard.plan}
              </div>
              <div className="text-xl font-bold text-text-primary">{dict.dashboard.planFree}</div>
              <div className="text-xs text-text-muted mt-1 space-y-0.5">
                <div>{dict.dashboard.planMaxDocs}</div>
                <div>{dict.dashboard.permanentStorage}</div>
                <div>{dict.dashboard.sizeLimit}</div>
              </div>
            </div>
          </div>
        )
      )}

      {/* Page Header */}
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-[22px] font-bold text-text-primary">
          {dict.dashboard.title}{" "}
          {!loading && (
            <span className="text-base font-normal text-text-muted ml-2">
              {pagination.totalElements}
            </span>
          )}
        </h1>
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-semibold text-white bg-accent hover:bg-accent-hover transition-colors no-underline"
        >
          {dict.dashboard.newDocument}
        </Link>
      </div>

      {/* Search + Tag Filter */}
      {!loading && (
        <div className="flex flex-col gap-3 mb-4">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <svg
                className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted pointer-events-none"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <circle cx="11" cy="11" r="8" />
                <path strokeLinecap="round" d="M21 21l-4.35-4.35" />
              </svg>
              <input
                type="text"
                value={search}
                onChange={(e) => handleSearchChange(e.target.value)}
                placeholder={dict.dashboard.searchPlaceholder}
                className="w-full pl-9 pr-8 py-2 text-sm bg-bg-secondary border border-border-dark rounded-lg text-text-primary placeholder:text-text-muted outline-none focus:border-accent/50 transition-colors"
              />
              {search && (
                <button
                  type="button"
                  onClick={() => handleSearchChange("")}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-primary transition-colors cursor-pointer bg-transparent border-none p-0"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                    <line x1="18" y1="6" x2="6" y2="18" strokeLinecap="round"/>
                    <line x1="6" y1="6" x2="18" y2="18" strokeLinecap="round"/>
                  </svg>
                </button>
              )}
            </div>
            <div className="relative shrink-0" ref={sortRef}>
              <button
                type="button"
                onClick={() => setSortOpen(v => !v)}
                className="flex items-center gap-1.5 px-3 py-2 text-sm bg-bg-secondary border border-border-dark rounded-lg text-text-secondary hover:border-accent/40 hover:text-text-primary transition-colors cursor-pointer whitespace-nowrap"
              >
                {sortLabels[sort]}
                <svg className={`w-3.5 h-3.5 transition-transform ${sortOpen ? "rotate-180" : ""}`} fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              {sortOpen && (
                <div className="absolute right-0 top-full mt-1 z-50 bg-bg-secondary border border-border-dark rounded-lg shadow-lg shadow-black/30 py-1 min-w-[100px]">
                  {Object.entries(sortLabels).map(([value, label]) => (
                    <button
                      key={value}
                      type="button"
                      onClick={() => { handleSortChange(value); setSortOpen(false); }}
                      className={`w-full flex items-center gap-2 px-3 py-2 text-xs transition-colors cursor-pointer bg-transparent border-none text-left ${
                        sort === value
                          ? "text-accent bg-accent/5"
                          : "text-text-secondary hover:bg-bg-tertiary hover:text-text-primary"
                      }`}
                    >
                      {sort === value && (
                        <svg className="w-3 h-3 shrink-0" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      )}
                      <span className={sort === value ? "" : "ml-5"}>{label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
          <TagFilter
            tags={allTags}
            selectedTagId={selectedTagId}
            onSelect={handleTagSelect}
            onDelete={handleDeleteTag}
          />
        </div>
      )}

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
            allTags={allTags}
            onTagsChange={handleTagsChange}
            isSearching={!!search || !!selectedTagId}
          />
        )}
      </div>
    </div>
  );
}
