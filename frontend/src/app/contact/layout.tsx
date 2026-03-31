import type { Metadata } from "next";
import Header from "@/components/layout/Header";

export const metadata: Metadata = {
  title: "문의하기 - DraftURL",
  description: "DraftURL에 문의하세요.",
  robots: "noindex, nofollow",
};

export default function ContactLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      <Header />
      <main className="flex-1">{children}</main>
    </div>
  );
}
