"use client";

import Link from "next/link";
import { useDict } from "@/components/i18n/DictProvider";

interface GoneContentProps {
  isExpired: boolean;
}

export default function GoneContent({ isExpired }: GoneContentProps) {
  const { dict } = useDict();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <div className="text-5xl mb-4">{isExpired ? "\u23F3" : "\uD83D\uDDD1\uFE0F"}</div>
        <h1 className="text-3xl font-bold text-text-primary">
          {isExpired ? dict.slug.docExpired : dict.slug.docDeleted}
        </h1>
        <p className="mt-4 text-text-secondary leading-relaxed">
          {isExpired
            ? dict.slug.expiredMessage
            : dict.slug.deletedMessage}
        </p>
        {isExpired && (
          <p className="mt-2 text-sm text-text-muted">
            {dict.slug.loginHint}
          </p>
        )}
        <div className="mt-6 flex items-center justify-center gap-3">
          <Link
            href="/"
            className="inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
          >
            {dict.slug.newDocument}
          </Link>
          {isExpired && (
            <Link
              href="/auth/login"
              className="inline-block rounded-md border border-border-dark px-4 py-2 text-sm font-medium text-text-secondary hover:bg-bg-tertiary transition-colors"
            >
              {dict.slug.login}
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
