"use client";

import Link from "next/link";
import { useDict } from "@/components/i18n/DictProvider";

interface ExpiredContentProps {
  slug?: string;
}

export default function ExpiredContent({ slug }: ExpiredContentProps) {
  const { dict } = useDict();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <h1 className="text-4xl font-bold text-text-primary">
          {dict.expired.title}
        </h1>
        <p className="mt-4 text-text-secondary">
          {dict.expired.message}
        </p>
        {slug && (
          <p className="mt-2 text-sm text-text-muted">{dict.expired.docId} {slug}</p>
        )}
        <p className="mt-4 text-sm text-text-secondary">
          {dict.expired.loginHint}
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Link
            href="/"
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
          >
            {dict.expired.newDocument}
          </Link>
          <Link
            href="/auth/login"
            className="rounded-md border border-border-dark px-4 py-2 text-sm font-medium text-text-primary hover:bg-bg-tertiary transition-colors"
          >
            {dict.expired.login}
          </Link>
        </div>
      </div>
    </div>
  );
}
