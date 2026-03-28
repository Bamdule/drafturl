import { unified } from "unified";
import remarkParse from "remark-parse";
import remarkGfm from "remark-gfm";
import remarkRehype from "remark-rehype";
import rehypeSanitize, { defaultSchema } from "rehype-sanitize";
import rehypeStringify from "rehype-stringify";

const processor = unified()
  .use(remarkParse)
  .use(remarkGfm)
  .use(remarkRehype)
  .use(rehypeSanitize, {
    ...defaultSchema,
    tagNames: [...(defaultSchema.tagNames ?? []), "del", "ins", "details", "summary"],
    attributes: {
      ...defaultSchema.attributes,
      img: ["src", "alt", "title", "width", "height"],
      a: ["href", "title", "target", "rel"],
      input: ["type", "checked", "disabled"],
    },
  })
  .use(rehypeStringify);

/**
 * Markdown 텍스트를 sanitize된 HTML 문자열로 변환.
 * 미리보기 및 문서 렌더링에서 공용으로 사용.
 */
export async function renderMarkdown(markdown: string): Promise<string> {
  const file = await processor.process(markdown);
  return String(file);
}
