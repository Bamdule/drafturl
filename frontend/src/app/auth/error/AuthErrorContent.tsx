"use client";

import Link from "next/link";
import { useDict } from "@/components/i18n/DictProvider";

export default function AuthErrorContent() {
  const { dict } = useDict();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="max-w-md text-center">
        <h1 className="text-2xl font-bold text-text-primary">
          {dict.auth.authError.title}
        </h1>
        <p className="mt-4 text-text-secondary">
          {dict.auth.authError.message}
        </p>
        <p className="mt-2 text-sm text-text-muted">
          {dict.auth.authError.hint}
        </p>
        <Link
          href="/auth/login"
          className="mt-6 inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-hover transition-colors"
        >
          {dict.auth.authError.retry}
        </Link>
      </div>
    </div>
  );
}
