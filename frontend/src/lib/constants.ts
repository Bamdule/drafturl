/** 문서 최대 크기 (5MB) */
export const MAX_CONTENT_SIZE = 5 * 1024 * 1024;

/** 미리보기 debounce 시간 (ms) */
export const PREVIEW_DEBOUNCE_MS = 300;

/** 지원하는 문서 타입 */
export const DOC_TYPES = ["html", "markdown"] as const;
export type DocType = (typeof DOC_TYPES)[number];

/** 파일 확장자 -> 문서 타입 매핑 */
export const FILE_EXTENSION_MAP: Record<string, DocType> = {
  ".html": "html",
  ".htm": "html",
  ".md": "markdown",
  ".markdown": "markdown",
};

/** 허용하는 파일 확장자 */
export const ALLOWED_EXTENSIONS = Object.keys(FILE_EXTENSION_MAP);

/** 쿠키 이름 */
export const COOKIE_ACCESS_TOKEN = "access_token";
export const COOKIE_REFRESH_TOKEN = "refresh_token";
export const COOKIE_OAUTH_STATE = "oauth_state";
export const COOKIE_OAUTH_PROVIDER = "oauth_provider";

/** OAuth2 설정 */
export const OAUTH_PROVIDERS = {
  google: {
    name: "Google",
    authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
    scope: "openid email profile",
  },
  github: {
    name: "GitHub",
    authUrl: "https://github.com/login/oauth/authorize",
    scope: "read:user user:email",
  },
} as const;

export type OAuthProvider = keyof typeof OAUTH_PROVIDERS;

/** 페이지당 기본 문서 수 */
export const DEFAULT_PAGE_SIZE = 20;
