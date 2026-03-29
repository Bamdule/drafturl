import type { Metadata } from "next";
import Header from "@/components/layout/Header";

export const metadata: Metadata = {
  title: "내 정보 - DraftURL",
  description: "계정 정보를 확인하고 관리하세요.",
  robots: "noindex, nofollow",
};

export default function AccountLayout({
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
