import { apiFetch } from "./client";
import { serverFetch } from "./server";
import type {
  CreateDocumentRequest,
  UpdateDocumentRequest,
  DocumentSummary,
  DocumentDetail,
  DocumentView,
  DocumentDeleteResult,
  DocumentListResponse,
} from "./types";

/** 문서 생성 (비로그인/로그인 공용) */
export function createDocument(
  data: CreateDocumentRequest,
  authenticated = false,
): Promise<DocumentSummary> {
  return apiFetch<DocumentSummary>("/api/v1/documents", {
    method: "POST",
    body: data,
    auth: authenticated,
  });
}

/** 내 문서 목록 조회 */
export function getMyDocuments(params?: {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  tagId?: number;
}): Promise<DocumentListResponse> {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined)
    searchParams.set("page", String(params.page));
  if (params?.size !== undefined)
    searchParams.set("size", String(params.size));
  if (params?.sort) searchParams.set("sort", params.sort);
  if (params?.search) searchParams.set("search", params.search);
  if (params?.tagId !== undefined)
    searchParams.set("tagId", String(params.tagId));

  const query = searchParams.toString();
  const path = `/api/v1/documents${query ? `?${query}` : ""}`;

  return apiFetch<DocumentListResponse>(path, { auth: true });
}

/** 문서 상세 조회 (편집용, 인증 필수) */
export function getDocument(slug: string): Promise<DocumentDetail> {
  return apiFetch<DocumentDetail>(`/api/v1/documents/${slug}`, {
    auth: true,
  });
}

/** 문서 서빙용 조회 (공개, 인증 불필요) — 클라이언트 컴포넌트용 */
export function getDocumentView(slug: string): Promise<DocumentView> {
  return apiFetch<DocumentView>(`/api/v1/documents/${slug}/view`);
}

/**
 * 문서 서빙용 조회 (공개, 인증 불필요) — 서버 컴포넌트 전용.
 * document.cookie에 의존하지 않는 serverFetch를 사용한다.
 */
export function getDocumentViewServer(slug: string): Promise<DocumentView> {
  return serverFetch<DocumentView>(`/api/v1/documents/${slug}/view`);
}

/** 비밀번호 보호 문서 검증 */
export function verifyDocumentPassword(
  slug: string,
  password: string,
): Promise<DocumentView> {
  return apiFetch<DocumentView>(`/api/v1/documents/${slug}/verify-password`, {
    method: "POST",
    body: { password },
  });
}

/** 문서 수정 */
export function updateDocument(
  slug: string,
  data: UpdateDocumentRequest,
): Promise<DocumentSummary> {
  return apiFetch<DocumentSummary>(`/api/v1/documents/${slug}`, {
    method: "PUT",
    body: data,
    auth: true,
  });
}

/** 게스트 문서를 내 계정으로 이관 (claim) */
export function claimDocument(slug: string): Promise<void> {
  return apiFetch<void>(`/api/v1/documents/${slug}/claim`, {
    method: "POST",
    auth: true,
  });
}

/** 문서 삭제 */
export function deleteDocument(
  slug: string,
): Promise<DocumentDeleteResult> {
  return apiFetch<DocumentDeleteResult>(`/api/v1/documents/${slug}`, {
    method: "DELETE",
    auth: true,
  });
}
