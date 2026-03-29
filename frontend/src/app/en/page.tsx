import type { Metadata } from "next";
import HomePage from "@/components/home/HomePage";

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

export default function EnPage() {
  return <HomePage />;
}
