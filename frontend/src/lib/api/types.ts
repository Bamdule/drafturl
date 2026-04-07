import type { DocType } from "@/lib/constants";

// ─── 공통 응답 포맷 ─────────────────────────────────

export interface ApiSuccessResponse<T> {
  success: true;
  data: T;
}

export interface ApiErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
  };
}

export type ApiResponse<T> = ApiSuccessResponse<T> | ApiErrorResponse;

// ─── API 에러 ───────────────────────────────────────

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

// ─── 태그 관련 타입 ─────────────────────────────────

export interface Tag {
  id: number;
  name: string;
}

// ─── 문서 관련 타입 ─────────────────────────────────

export interface DocumentSummary {
  id: string;
  slug: string;
  url: string;
  title: string | null;
  docType: DocType;
  contentSize: number;
  status: "active" | "expired" | "deleted";
  isPasswordProtected: boolean;
  preview?: string | null;
  tags: Tag[];
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  content: string;
}

export interface DocumentView {
  id: string;
  title: string | null;
  docType: DocType;
  contentUrl: string | null;
  isPasswordProtected: boolean;
  createdAt: string;
}

export interface DocumentDeleteResult {
  id: string;
  slug: string;
  deletedAt: string;
}

export interface PaginationInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DocumentListResponse {
  documents: DocumentSummary[];
  pagination: PaginationInfo;
}

export interface CreateDocumentRequest {
  content: string;
  type: DocType;
  title?: string;
  password?: string;
}

export interface UpdateDocumentRequest {
  content?: string;
  title?: string;
  password?: string | null;
}

// ─── 인증 관련 타입 ─────────────────────────────────

export interface User {
  id: string;
  email: string;
  name: string;
  profileImage: string | null;
  plan: "free" | "starter" | "pro" | "team";
}

export interface StorageUsage {
  totalBytes: number;
  documentCount: number;
}

export interface UserWithStorage extends User {
  storageUsage: StorageUsage;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface OAuthCallbackResponse extends AuthTokens {
  user: User;
}

export interface OAuthStateResponse {
  state: string;
}

export interface OAuthCallbackRequest {
  code: string;
  redirectUri: string;
  state: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
}

export interface EmailSignupRequest {
  email: string;
  password: string;
  name: string;
}

export interface EmailLoginRequest {
  email: string;
  password: string;
}

export interface EmailAuthResponse extends AuthTokens {
  user: User;
}
