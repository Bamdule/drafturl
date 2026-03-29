import type { Metadata } from "next";
import AuthErrorContent from "./AuthErrorContent";

export const metadata: Metadata = {
  title: "인증 오류 - DraftURL",
  description: "인증 과정에서 문제가 발생했습니다.",
  robots: "noindex, nofollow",
};

export default function AuthErrorPage() {
  return <AuthErrorContent />;
}
