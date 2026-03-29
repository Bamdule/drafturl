import type ko from "./ko.json";

export type Dict = typeof ko;
export type Locale = "en" | "ko";

export const LOCALES: Locale[] = ["en", "ko"];
export const DEFAULT_LOCALE: Locale = "ko";
