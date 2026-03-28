"use client";

import { useCallback, useState } from "react";
import { verifyDocumentPassword } from "@/lib/api/documents";
import { ApiError } from "@/lib/api/types";
import type { DocumentView } from "@/lib/api/types";
import DocumentViewPage from "./DocumentViewPage";

interface PasswordGatePageProps {
  slug: string;
  document: DocumentView;
}

export default function PasswordGatePage({
  slug,
  document,
}: PasswordGatePageProps) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [unlockedDoc, setUnlockedDoc] = useState<DocumentView | null>(() => {
    if (typeof window === "undefined") return null;
    const cached = sessionStorage.getItem(`doc_auth_${slug}`);
    if (!cached) return null;
    try {
      return JSON.parse(cached) as DocumentView;
    } catch {
      return null;
    }
  });

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!password.trim() || isLoading) return;

      setError(null);
      setIsLoading(true);

      try {
        const result = await verifyDocumentPassword(slug, password);
        sessionStorage.setItem(`doc_auth_${slug}`, JSON.stringify(result));
        setUnlockedDoc(result);
      } catch (err) {
        if (err instanceof ApiError && err.code === "INVALID_PASSWORD") {
          setError("비밀번호가 올바르지 않습니다.");
        } else {
          setError("오류가 발생했습니다. 다시 시도해주세요.");
        }
      } finally {
        setIsLoading(false);
      }
    },
    [slug, password, isLoading],
  );

  if (unlockedDoc) {
    return <DocumentViewPage document={unlockedDoc} />;
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 bg-bg-primary">
      <div className="w-full max-w-sm text-center">
        <div className="text-5xl mb-4">&#128274;</div>
        <h1 className="text-2xl font-bold text-text-primary">
          비밀번호로 보호된 문서
        </h1>
        {document.title && (
          <p className="mt-2 text-sm text-text-muted">{document.title}</p>
        )}
        <p className="mt-3 text-text-secondary text-sm">
          이 문서를 열람하려면 비밀번호를 입력하세요.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-3">
          <input
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setError(null);
            }}
            placeholder="비밀번호를 입력하세요"
            autoFocus
            className="h-11 w-full rounded-lg border border-border-dark bg-bg-secondary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors text-center"
          />

          {error && <p className="text-xs text-danger">{error}</p>}

          <button
            type="submit"
            disabled={isLoading || !password.trim()}
            className="h-11 w-full rounded-lg bg-accent text-white text-sm font-medium hover:bg-accent-hover transition-colors disabled:opacity-70 disabled:pointer-events-none cursor-pointer"
          >
            {isLoading ? "확인 중..." : "열기"}
          </button>
        </form>
      </div>
    </div>
  );
}
