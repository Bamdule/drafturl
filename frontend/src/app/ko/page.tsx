import type { Metadata } from "next";
import HomePage from "@/components/home/HomePage";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  title: "DraftURL — HTML/MD 문서를 URL로 즉시 공유",
  description:
    "HTML이나 Markdown 문서를 붙여넣고 공유 URL을 즉시 생성하세요. AI로 계속 수정할 수 있는 살아있는 문서 플랫폼.",
  alternates: {
    canonical: `${siteUrl}/ko`,
    languages: {
      en: `${siteUrl}/en`,
      ko: `${siteUrl}/ko`,
      "x-default": `${siteUrl}/en`,
    },
  },
  openGraph: {
    title: "DraftURL — HTML/MD 문서를 URL로 즉시 공유",
    description:
      "HTML이나 Markdown 문서를 붙여넣고 공유 URL을 즉시 생성하세요.",
    type: "website",
    siteName: "DraftURL",
    locale: "ko_KR",
    alternateLocale: "en_US",
    url: `${siteUrl}/ko`,
  },
  twitter: {
    card: "summary_large_image",
    title: "DraftURL — HTML/MD 문서를 URL로 즉시 공유",
    description:
      "HTML이나 Markdown 문서를 붙여넣고 공유 URL을 즉시 생성하세요.",
  },
};

export default function KoPage() {
  return <HomePage />;
}
