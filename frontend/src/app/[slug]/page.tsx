import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getDocumentViewServer } from "@/lib/api/documents";
import { ApiError } from "@/lib/api/types";
import DocumentViewPage from "./DocumentViewPage";
import PasswordGatePage from "./PasswordGatePage";
import GoneContent from "./GoneContent";

interface SlugPageProps {
  params: Promise<{ slug: string }>;
}

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

/** 동적 메타데이터: 문서 제목/설명을 Open Graph 태그로 노출 */
export async function generateMetadata({
  params,
}: SlugPageProps): Promise<Metadata> {
  const { slug } = await params;
  const pageUrl = `${siteUrl}/${slug}`;

  try {
    const doc = await getDocumentViewServer(slug);
    const title = doc.title || "Shared Document";
    const docTypeName = doc.docType === "html" ? "HTML" : "Markdown";
    const description = `${docTypeName} document shared via DraftURL — ${title}`;

    return {
      title,
      description,
      alternates: {
        canonical: pageUrl,
      },
      openGraph: {
        title,
        description,
        type: "article",
        siteName: "DraftURL",
        locale: "en_US",
        alternateLocale: "ko_KR",
        url: pageUrl,
      },
      twitter: {
        card: "summary_large_image",
        title,
        description,
      },
    };
  } catch {
    return {
      title: "Document Not Found",
      description: "The requested document does not exist or has expired.",
      robots: { index: false, follow: false },
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

    if (doc.isPasswordProtected && !doc.contentUrl) {
      return <PasswordGatePage slug={slug} document={doc} />;
    }

    const articleJsonLd = {
      "@context": "https://schema.org",
      "@type": "Article",
      headline: doc.title || "Shared Document",
      url: `${siteUrl}/${slug}`,
      datePublished: doc.createdAt,
      dateModified: doc.createdAt,
      publisher: {
        "@type": "Organization",
        name: "DraftURL",
        url: siteUrl,
      },
      description: `${doc.docType === "html" ? "HTML" : "Markdown"} document shared via DraftURL`,
    };

    return (
      <>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(articleJsonLd) }}
        />
        <DocumentViewPage document={doc} />
      </>
    );
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 404) {
        notFound();
      }

      if (err.status === 410) {
        const isExpired = err.code === "DOCUMENT_EXPIRED";
        return <GoneContent isExpired={isExpired} />;
      }
    }

    throw err;
  }
}
