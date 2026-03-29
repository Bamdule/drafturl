import "server-only";

export type { Dict, Locale } from "./types";
export { LOCALES, DEFAULT_LOCALE } from "./types";

const dictionaries = {
  en: () => import("./en.json").then((m) => m.default),
  ko: () => import("./ko.json").then((m) => m.default),
};

export const hasLocale = (locale: string): locale is keyof typeof dictionaries =>
  locale in dictionaries;

export const getDictionary = async (locale: keyof typeof dictionaries) =>
  dictionaries[locale]();
