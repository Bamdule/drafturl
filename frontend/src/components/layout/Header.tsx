"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { logout as logoutApi } from "@/lib/api/auth";
import { useDict } from "@/components/i18n/DictProvider";

export default function Header() {
  const { dict } = useDict();
  const router = useRouter();
  const { user, isAuthenticated, logout: logoutStore } = useAuthStore();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = async () => {
    setMenuOpen(false);
    try {
      await logoutApi();
    } catch {
      // 로그아웃 API 실패해도 클라이언트 상태는 정리
    } finally {
      logoutStore();
      router.push("/");
    }
  };

  return (
    <header className="sticky top-0 z-50 flex items-center justify-between px-4 sm:px-6 h-14 bg-bg-primary/85 backdrop-blur-xl border-b border-border-dark">
      <Link
        href="/"
        className="flex items-center gap-2 text-lg font-bold text-text-primary no-underline"
      >
        <div className="w-7 h-7 bg-gradient-to-br from-accent to-[#a78bfa] rounded-lg flex items-center justify-center text-sm text-white font-bold">
          D
        </div>
        DraftURL
      </Link>

      {/* Desktop nav */}
      <nav className="hidden sm:flex items-center gap-2">
        <Link
          href="/guide"
          className="inline-flex items-center justify-center px-4 py-2 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors"
        >
          {dict.header.guide}
        </Link>
        {isAuthenticated ? (
          <>
            <Link
              href="/dashboard"
              className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors"
            >
              {dict.header.myDocuments}
            </Link>
            <Link
              href="/account"
              className="flex items-center gap-2 px-3 py-1 pl-1 rounded-full bg-bg-tertiary border border-border-dark hover:border-border-dark-hover transition-colors"
            >
              <div className="w-7 h-7 rounded-full bg-gradient-to-br from-accent to-[#60a5fa] flex items-center justify-center text-xs font-bold text-white">
                {user?.name?.[0] ?? "U"}
              </div>
              <span className="text-sm font-medium text-text-primary">
                {user?.name}
              </span>
            </Link>
            <button
              onClick={handleLogout}
              className="inline-flex items-center justify-center px-4 py-2 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors cursor-pointer"
            >
              {dict.header.logout}
            </button>
          </>
        ) : (
          <Link
            href="/auth/login"
            className="inline-flex items-center justify-center px-4 py-2 rounded-md text-sm font-medium text-text-primary border border-border-dark hover:border-border-dark-hover hover:bg-bg-tertiary transition-colors"
          >
            {dict.header.login}
          </Link>
        )}
      </nav>

      {/* Mobile hamburger */}
      <button
        onClick={() => setMenuOpen(!menuOpen)}
        className="sm:hidden flex flex-col gap-1.5 p-2 cursor-pointer"
        aria-label={dict.header.menu}
      >
        <span className={`block w-5 h-0.5 bg-text-primary transition-transform ${menuOpen ? "rotate-45 translate-y-2" : ""}`} />
        <span className={`block w-5 h-0.5 bg-text-primary transition-opacity ${menuOpen ? "opacity-0" : ""}`} />
        <span className={`block w-5 h-0.5 bg-text-primary transition-transform ${menuOpen ? "-rotate-45 -translate-y-2" : ""}`} />
      </button>

      {/* Mobile dropdown */}
      {menuOpen && (
        <div className="sm:hidden absolute top-14 left-0 right-0 bg-bg-primary border-b border-border-dark px-4 py-3 flex flex-col gap-1">
          <Link
            href="/guide"
            onClick={() => setMenuOpen(false)}
            className="px-3 py-2 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors"
          >
            {dict.header.guide}
          </Link>
          {isAuthenticated ? (
            <>
              <Link
                href="/account"
                onClick={() => setMenuOpen(false)}
                className="flex items-center gap-2 px-3 py-2 rounded-md hover:bg-bg-tertiary transition-colors"
              >
                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-accent to-[#60a5fa] flex items-center justify-center text-xs font-bold text-white shrink-0">
                  {user?.name?.[0] ?? "U"}
                </div>
                <span className="text-sm font-medium text-text-primary">
                  {user?.name}
                </span>
              </Link>
              <Link
                href="/dashboard"
                onClick={() => setMenuOpen(false)}
                className="px-3 py-2 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors"
              >
                {dict.header.myDocuments}
              </Link>
              <button
                onClick={handleLogout}
                className="px-3 py-2 rounded-md text-sm font-medium text-text-secondary hover:bg-bg-tertiary hover:text-text-primary transition-colors cursor-pointer text-left"
              >
                {dict.header.logout}
              </button>
            </>
          ) : (
            <Link
              href="/auth/login"
              onClick={() => setMenuOpen(false)}
              className="px-3 py-2 rounded-md text-sm font-medium text-text-primary hover:bg-bg-tertiary transition-colors"
            >
              {dict.header.login}
            </Link>
          )}
        </div>
      )}
    </header>
  );
}
