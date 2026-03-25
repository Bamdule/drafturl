import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "로그인 - DraftURL",
  description: "Google 또는 GitHub 계정으로 로그인하여 문서를 영구 보관하세요.",
};

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
