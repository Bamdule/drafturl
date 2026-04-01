import { headers } from "next/headers";
import Header from "@/components/layout/Header";
import HomeStaticSections from "@/components/home/HomeStaticSections";
import type { Locale } from "@/dictionaries/types";

export default async function GuideLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const headersList = await headers();
  const locale = (headersList.get("x-locale") ?? "ko") as Locale;

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      <Header />
      <div className="flex-1">{children}</div>
      <HomeStaticSections locale={locale} />
    </div>
  );
}
