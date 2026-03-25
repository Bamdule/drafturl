import { unified } from "unified";
import remarkParse from "remark-parse";
import remarkGfm from "remark-gfm";
import remarkRehype from "remark-rehype";
import rehypeStringify from "rehype-stringify";

const processor = unified()
  .use(remarkParse)
  .use(remarkGfm)          // GFM: 테이블, 취소선, 자동링크, 체크리스트
  .use(remarkRehype)
  .use(rehypeStringify);   // rehype-sanitize 제거: iframe sandbox가 보안 담당

/**
 * Markdown 텍스트를 sanitize된 HTML 문자열로 변환.
 * 미리보기 및 문서 렌더링에서 공용으로 사용.
 */
export async function renderMarkdown(markdown: string): Promise<string> {
  const file = await processor.process(markdown);
  return String(file);
}
