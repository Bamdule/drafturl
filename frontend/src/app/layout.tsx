import type { Metadata, Viewport } from "next";
import { headers } from "next/headers";
import Script from "next/script";
import AuthInitializer from "@/components/auth/AuthInitializer";
import { DictProvider } from "@/components/i18n/DictProvider";
import { getDictionary } from "@/dictionaries";
import type { Locale } from "@/dictionaries/types";
import "./globals.css";

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "DraftURL — Share HTML & Markdown Instantly via URL",
    template: "%s | DraftURL",
  },
  description:
    "Paste HTML or Markdown and get a shareable URL in seconds. No signup required. A living document platform you can keep editing with AI.",
  keywords: [
    "HTML sharing",
    "Markdown sharing",
    "document sharing",
    "share URL",
    "paste and share",
    "HTML to URL",
    "Markdown to URL",
    "HTML 공유",
    "마크다운 공유",
    "문서 공유",
    "URL 공유",
  ],
  verification: {
    google: "1la5UC8V_Dkwq9Ar8cgGAYuO4wMe94FHhGqF24zmLfk",
  },
  icons: {
    icon: "/favicon.svg",
    apple: "/favicon.svg",
  },
  alternates: {
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
    url: siteUrl,
  },
  twitter: {
    card: "summary_large_image",
    title: "DraftURL — Share HTML & Markdown Instantly via URL",
    description:
      "Paste HTML or Markdown and get a shareable URL in seconds. No signup, no build step.",
  },
};

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebApplication",
  name: "DraftURL",
  url: siteUrl,
  description:
    "Paste HTML or Markdown and get a shareable URL in seconds. No signup required.",
  applicationCategory: "DeveloperApplication",
  operatingSystem: "All",
  offers: {
    "@type": "Offer",
    price: "0",
    priceCurrency: "USD",
  },
  inLanguage: ["en", "ko"],
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;
  const dict = await getDictionary(locale === "en" ? "en" : "ko");

  return (
    <html
      lang={locale}
      className="h-full antialiased dark"
    >
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className="min-h-full flex flex-col bg-bg-primary text-text-primary">
        <Script
          defer
          src="https://stats.drafturl.com/script.js"
          data-website-id="7e336802-2478-4f28-9e4b-06f849b031a3"
        />
        <DictProvider dict={dict} locale={locale}>
          <AuthInitializer />
          {children}
        </DictProvider>
      </body>
    </html>
  );
}
