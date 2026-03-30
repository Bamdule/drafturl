import fs from "fs";
import path from "path";
import { unified } from "unified";
import remarkParse from "remark-parse";
import remarkGfm from "remark-gfm";
import remarkRehype from "remark-rehype";
import rehypeStringify from "rehype-stringify";
import type { Locale } from "@/dictionaries/types";

// Try monorepo root first (local dev), then fallback to copied location (Docker)
const MONOREPO_LEGAL_DIR = path.join(process.cwd(), "..", "docs", "legal");
const LOCAL_LEGAL_DIR = path.join(process.cwd(), "docs", "legal");
const LEGAL_DIR = fs.existsSync(MONOREPO_LEGAL_DIR) ? MONOREPO_LEGAL_DIR : LOCAL_LEGAL_DIR;

type LegalDoc = "terms-of-service" | "privacy-policy" | "acceptable-use-policy";

export async function renderLegalMarkdown(
  doc: LegalDoc,
  locale: Locale,
): Promise<string> {
  const filename = `${doc}.${locale}.md`;
  const filePath = path.join(LEGAL_DIR, filename);

  // Fallback to Korean if the requested locale file does not exist
  const fallbackPath = path.join(LEGAL_DIR, `${doc}.ko.md`);
  const resolvedPath = fs.existsSync(filePath) ? filePath : fallbackPath;

  const markdown = fs.readFileSync(resolvedPath, "utf-8");

  const result = await unified()
    .use(remarkParse)
    .use(remarkGfm)
    .use(remarkRehype, { allowDangerousHtml: true })
    .use(rehypeStringify, { allowDangerousHtml: true })
    .process(markdown);

  return String(result);
}
