import fs from "fs";
import path from "path";
import type { Locale } from "@/dictionaries/types";

const MONOREPO_GUIDE_DIR = path.join(process.cwd(), "..", "docs", "guide");
const LOCAL_GUIDE_DIR = path.join(process.cwd(), "docs", "guide");
const GUIDE_DIR = fs.existsSync(MONOREPO_GUIDE_DIR) ? MONOREPO_GUIDE_DIR : LOCAL_GUIDE_DIR;

type GuideDoc = "mcp";

export function renderGuideHtml(doc: GuideDoc, locale: Locale): string {
  const filename = `${doc}.${locale}.html`;
  const filePath = path.join(GUIDE_DIR, filename);
  const fallbackPath = path.join(GUIDE_DIR, `${doc}.ko.html`);
  const resolvedPath = fs.existsSync(filePath) ? filePath : fallbackPath;

  return fs.readFileSync(resolvedPath, "utf-8");
}
