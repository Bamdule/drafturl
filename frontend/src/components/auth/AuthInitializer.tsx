"use client";

import { useEffect, useRef } from "react";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { getMe } from "@/lib/api/auth";

/**
 * 앱 초기화 시 /api/auth/me를 호출하여 Auth store를 복원한다.
 * httpOnly 쿠키가 존재하면 서버에서 인증 상태를 확인한다.
 */
export default function AuthInitializer() {
  const { login, isAuthenticated } = useAuthStore();
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current || isAuthenticated) return;
    initialized.current = true;

    getMe()
      .then((data) => {
        if (data) login(data);
        // data가 null이면 비로그인 상태 — 아무것도 하지 않음
      })
      .catch(() => {
        // 네트워크 에러 등 — 로그인 안 된 상태 유지
      });
  }, [login, isAuthenticated]);

  return null;
}
