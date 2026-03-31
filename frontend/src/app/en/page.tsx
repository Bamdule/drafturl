import type { Metadata } from "next";
import HomePage from "@/components/home/HomePage";
import HomeStaticSections from "@/components/home/HomeStaticSections";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  title: "DraftURL — Share HTML & Markdown Instantly via URL",
  description:
    "Paste HTML or Markdown and get a shareable URL in seconds. No signup required. A living document platform you can keep editing with AI.",
  alternates: {
    canonical: `${siteUrl}/en`,
    languages: {
      en: `${siteUrl}/en`,
      ko: `${siteUrl}/ko`,
      "x-default": `${siteUrl}/en`,
    },
  },
  openGraph: {
    title: "DraftURL — Share HTML & Markdown Instantly via URL",
    description:
      "Paste HTML or Markdown and get a shareable URL in seconds. No signup, no build step.",
    type: "website",
    siteName: "DraftURL",
    locale: "en_US",
    alternateLocale: "ko_KR",
    url: `${siteUrl}/en`,
  },
  twitter: {
    card: "summary_large_image",
    title: "DraftURL — Share HTML & Markdown Instantly via URL",
    description:
      "Paste HTML or Markdown and get a shareable URL in seconds. No signup, no build step.",
  },
};

const faqSchema = {
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: [
    {
      "@type": "Question",
      name: "What is DraftURL?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "DraftURL is a free tool that lets you share HTML and Markdown documents via a unique URL instantly. Paste your content, click Share, and get a link — no signup required.",
      },
    },
    {
      "@type": "Question",
      name: "Do I need to sign up to use DraftURL?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "No signup is required. You can create and share documents as a guest. Guest documents automatically expire after 24 hours. Log in with Google or GitHub to keep documents permanently.",
      },
    },
    {
      "@type": "Question",
      name: "What file formats does DraftURL support?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "DraftURL supports HTML (.html, .htm) and Markdown (.md, .markdown) documents up to 5MB in size.",
      },
    },
    {
      "@type": "Question",
      name: "How long are shared documents kept?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "Guest documents expire after 24 hours. Documents created by logged-in users are stored permanently.",
      },
    },
    {
      "@type": "Question",
      name: "Can I edit a document after sharing?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "Yes. Logged-in users can edit their documents at any time from the dashboard. Guest documents cannot be modified after sharing.",
      },
    },
    {
      "@type": "Question",
      name: "Is DraftURL free?",
      acceptedAnswer: {
        "@type": "Answer",
        text: "Yes, DraftURL is completely free to use. No credit card or signup required for basic usage.",
      },
    },
  ],
};

const howToSchema = {
  "@context": "https://schema.org",
  "@type": "HowTo",
  name: "How to share an HTML or Markdown document via URL",
  description:
    "Share HTML or Markdown documents instantly via a unique URL using DraftURL.",
  step: [
    {
      "@type": "HowToStep",
      position: 1,
      name: "Paste or Drag & Drop",
      text: "Paste HTML or Markdown from ChatGPT, Claude, or any LLM into the editor — or drag & drop an .html/.md file.",
    },
    {
      "@type": "HowToStep",
      position: 2,
      name: "Check the Preview",
      text: "See the result in the live preview on the right. Edit on the spot if needed.",
    },
    {
      "@type": "HowToStep",
      position: 3,
      name: "Get a Share URL",
      text: "Click Share and get a unique URL instantly. Send it to anyone — no account needed.",
    },
  ],
};

export default function EnPage() {
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
        <HomeStaticSections locale="en" />
      </HomePage>
    </>
  );
}
