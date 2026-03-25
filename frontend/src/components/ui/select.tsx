import * as React from "react";
import { cn } from "@/lib/utils";

const Select = React.forwardRef<
  HTMLSelectElement,
  React.SelectHTMLAttributes<HTMLSelectElement>
>(({ className, children, ...props }, ref) => {
  return (
    <select
      ref={ref}
      className={cn(
        "flex h-10 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 ring-offset-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-950 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-50 dark:ring-offset-zinc-950 dark:focus-visible:ring-zinc-300",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
});
Select.displayName = "Select";

export { Select };
