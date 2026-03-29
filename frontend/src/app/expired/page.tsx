import type { Metadata } from "next";
import ExpiredContent from "./ExpiredContent";

export const metadata: Metadata = {
  title: "문서 만료 - DraftURL",
  description: "이 문서는 만료 기간이 지나 더 이상 볼 수 없습니다.",
  robots: "noindex, nofollow",
};

interface ExpiredPageProps {
  searchParams: Promise<{ slug?: string }>;
}

export default async function ExpiredPage({
  searchParams,
}: ExpiredPageProps) {
  const { slug } = await searchParams;

  return <ExpiredContent slug={slug} />;
}
