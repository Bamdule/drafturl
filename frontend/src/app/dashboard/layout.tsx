import type { Metadata } from "next";
import Header from "@/components/layout/Header";

export const metadata: Metadata = {
  title: "내 문서 - DraftURL",
  description: "내가 만든 문서를 관리하고 편집하세요.",
};

export default function DashboardLayout({
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
