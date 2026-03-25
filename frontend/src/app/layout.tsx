import type { Metadata } from "next";
import AuthInitializer from "@/components/auth/AuthInitializer";
import "./globals.css";

export const metadata: Metadata = {
  title: "DraftURL - HTML/MD 문서를 URL로 즉시 공유",
  description:
    "HTML이나 Markdown 문서를 붙여넣고 공유 URL을 즉시 생성하세요. AI로 계속 수정할 수 있는 살아있는 문서 플랫폼.",
  icons: {
    icon: "/favicon.svg",
  },
  openGraph: {
    title: "DraftURL - HTML/MD 문서를 URL로 즉시 공유",
    description:
      "HTML이나 Markdown 문서를 붙여넣고 공유 URL을 즉시 생성하세요.",
    type: "website",
    siteName: "DraftURL",
    locale: "ko_KR",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      className="h-full antialiased dark"
    >
      <body className="min-h-full flex flex-col bg-bg-primary text-text-primary">
        <AuthInitializer />
        {children}
      </body>
    </html>
  );
}
