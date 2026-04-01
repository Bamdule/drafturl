// Server Component — no "use client"
import Link from "next/link";

type Locale = "en" | "ko";

const footerContent = {
  en: {
    terms: "Terms of Service",
    privacy: "Privacy Policy",
    contact: "Contact",
  },
  ko: {
    terms: "이용약관",
    privacy: "개인정보 처리방침",
    contact: "문의",
  },
};

export default function HomeStaticSections({ locale }: { locale: Locale }) {
  const t = footerContent[locale];

  return (
    <footer className="mt-auto border-t border-border-dark">
      <div className="mx-auto flex max-w-screen-xl flex-col items-center justify-between gap-3 px-6 py-6 text-xs text-text-muted/60 sm:flex-row">
        <span>&copy; 2026 DraftURL</span>
        <div className="flex items-center gap-4">
          <Link
            href="/terms"
            className="transition-colors hover:text-text-muted"
          >
            {t.terms}
          </Link>
          <Link
            href="/privacy"
            className="transition-colors hover:text-text-muted"
          >
            {t.privacy}
          </Link>
          <Link
            href="/contact"
            className="transition-colors hover:text-text-muted"
          >
            {t.contact}
          </Link>
        </div>
      </div>
    </footer>
  );
}
