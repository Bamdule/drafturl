"use client";

import { createContext, useContext } from "react";
import type { Dict, Locale } from "@/dictionaries/types";

interface DictContextValue {
  dict: Dict;
  locale: Locale;
}

const DictContext = createContext<DictContextValue | null>(null);

export function DictProvider({
  dict,
  locale,
  children,
}: {
  dict: Dict;
  locale: Locale;
  children: React.ReactNode;
}) {
  return (
    <DictContext.Provider value={{ dict, locale }}>
      {children}
    </DictContext.Provider>
  );
}

export function useDict(): DictContextValue {
  const ctx = useContext(DictContext);
  if (!ctx) {
    throw new Error("useDict must be used within a DictProvider");
  }
  return ctx;
}
