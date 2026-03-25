import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getDocumentViewServer } from "@/lib/api/documents";
import { ApiError } from "@/lib/api/types";
import DocumentViewPage from "./DocumentViewPage";

interface SlugPageProps {
  params: Promise<{ slug: string }>;
}

/** 동적 메타데이터: 문서 제목/설명을 Open Graph 태그로 노출 */
export async function generateMetadata({
  params,
}: SlugPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const doc = await getDocumentViewServer(slug);
    const title = doc.title || "공유 문서";
    const description = `DraftURL로 공유된 ${doc.docType === "html" ? "HTML" : "Markdown"} 문서`;

    return {
      title,
      description,
      openGraph: {
        title,
        description,
        type: "article",
        siteName: "DraftURL",
      },
    };
  } catch {
    return {
      title: "문서를 찾을 수 없습니다",
      description: "요청한 문서가 존재하지 않거나 만료되었습니다.",
    };
  }
}

/**
 * 문서 서빙 페이지.
 * 공유 URL로 접근 시 서버 컴포넌트에서 API를 호출하여 문서를 렌더링한다.
 * - 200: 문서 렌더링
 * - 404: notFound()
 * - 410: 만료/삭제 안내 UI
 */
export default async function SlugPage({ params }: SlugPageProps) {
  const { slug } = await params;

  try {
    const doc = await getDocumentViewServer(slug);
    return <DocumentViewPage document={doc} />;
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 404) {
        notFound();
      }

      if (err.status === 410) {
        const isExpired = err.code === "DOCUMENT_EXPIRED";
        return (
          <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
            <div className="max-w-md text-center">
              <h1 className="text-4xl font-bold text-text-primary">
                {isExpired ? "문서가 만료되었습니다" : "문서가 삭제되었습니다"}
              </h1>
              <p className="mt-4 text-text-secondary">
                {isExpired
                  ? "이 문서는 만료 기간이 지나 더 이상 볼 수 없습니다."
                  : "이 문서는 소유자에 의해 삭제되었습니다."}
              </p>
              <a
                href="/"
                className="mt-6 inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
              >
                나도 만들어보기
              </a>
            </div>
          </div>
        );
      }
    }

    throw err;
  }
}
