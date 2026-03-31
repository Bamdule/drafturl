import type { Metadata } from "next";
import HomePage from "@/components/home/HomePage";
import HomeStaticSections from "@/components/home/HomeStaticSections";

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

const faqSchema = {
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: [
    {
      "@type": "Question",
      name: "DraftURL이 무엇인가요?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "DraftURL은 HTML이나 Markdown 문서를 고유 URL로 즉시 공유할 수 있는 무료 서비스입니다. 내용을 붙여넣고 공유 버튼을 클릭하면 링크가 바로 생성됩니다. 회원가입이 필요 없습니다.",
      },
    },
    {
      "@type": "Question",
      name: "회원가입 없이 사용할 수 있나요?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "네, 가입 없이도 바로 사용할 수 있습니다. 비로그인 문서는 24시간 후 자동 만료됩니다. Google 또는 GitHub으로 로그인하면 문서를 영구적으로 보관할 수 있습니다.",
      },
    },
    {
      "@type": "Question",
      name: "어떤 파일 형식을 지원하나요?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "HTML(.html, .htm)과 Markdown(.md, .markdown) 파일을 지원합니다. 최대 파일 크기는 5MB입니다.",
      },
    },
    {
      "@type": "Question",
      name: "공유한 문서는 얼마나 보관되나요?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "비로그인으로 만든 문서는 24시간 후 자동 만료됩니다. 로그인 사용자의 문서는 영구적으로 보관됩니다.",
      },
    },
    {
      "@type": "Question",
      name: "공유 후 문서를 수정할 수 있나요?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "로그인 사용자는 대시보드에서 자신의 문서를 언제든지 수정할 수 있습니다. 비로그인 문서는 공유 후 수정이 불가능합니다.",
      },
    },
    {
      "@type": "Question",
      name: "DraftURL은 무료인가요?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "네, 완전 무료로 사용할 수 있습니다. 기본 기능은 신용카드나 회원가입 없이 이용 가능합니다.",
      },
    },
  ],
};

const howToSchema = {
  "@context": "https://schema.org",
  "@type": "HowTo",
  name: "HTML 또는 Markdown 문서를 URL로 공유하는 방법",
  description:
    "DraftURL을 사용하여 HTML 또는 Markdown 문서를 고유 URL로 즉시 공유하세요.",
  step: [
    {
      "@type": "HowToStep",
      position: 1,
      name: "붙여넣기 또는 드래그앤드롭",
      text: "ChatGPT, Claude 등 LLM이 생성한 HTML이나 Markdown을 붙여넣거나, .html/.md 파일을 드래그앤드롭하세요.",
    },
    {
      "@type": "HowToStep",
      position: 2,
      name: "미리보기 확인",
      text: "오른쪽 미리보기에서 결과를 실시간으로 확인하고, 필요하면 바로 수정하세요.",
    },
    {
      "@type": "HowToStep",
      position: 3,
      name: "공유 URL 발급",
      text: "공유하기 버튼 한 번이면 고유 URL이 즉시 생성됩니다. 누구에게나 보낼 수 있습니다.",
    },
  ],
};

export default function KoPage() {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(howToSchema) }}
      />
      <HomePage>
        <HomeStaticSections locale="ko" />
      </HomePage>
    </>
  );
}
