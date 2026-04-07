import { apiFetch } from "./client";
import type { Tag } from "./types";

export type { Tag };

// ─── 태그 타입 ──────────────────────────────────────

export interface TagWithCount extends Tag {
  documentCount: number;
}

// ─── 태그 CRUD ──────────────────────────────────────

/** 내 태그 목록 조회 (문서 수 포함) */
export function getMyTags(): Promise<TagWithCount[]> {
  return apiFetch<TagWithCount[]>("/api/v1/tags", { auth: true });
}

/** 태그 생성 */
export function createTag(name: string): Promise<Tag> {
  return apiFetch<Tag>("/api/v1/tags", {
    method: "POST",
    body: { name },
    auth: true,
  });
}

/** 태그 삭제 */
export function deleteTag(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/tags/${id}`, {
    method: "DELETE",
    auth: true,
  });
}

// ─── 문서-태그 관계 ─────────────────────────────────

/** 문서에 태그 추가 */
export function addTagToDocument(slug: string, tagId: number): Promise<void> {
  return apiFetch<void>(`/api/v1/tags/documents/${slug}/tags`, {
    method: "POST",
    body: { tagId },
    auth: true,
  });
}

/** 문서에서 태그 제거 */
export function removeTagFromDocument(
  slug: string,
  tagId: number,
): Promise<void> {
  return apiFetch<void>(`/api/v1/tags/documents/${slug}/tags/${tagId}`, {
    method: "DELETE",
    auth: true,
  });
}
