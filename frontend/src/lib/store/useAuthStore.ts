"use client";

import { create } from "zustand";
import type { User } from "@/lib/api/types";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  login: (user: User) => void;
  logout: () => void;
  setUser: (user: User) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,

  login: (user) =>
    set({
      user,
      isAuthenticated: true,
    }),

  logout: () =>
    set({
      user: null,
      isAuthenticated: false,
    }),

  setUser: (user) =>
    set({
      user,
      isAuthenticated: true,
    }),
}));
