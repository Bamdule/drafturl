"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { deleteAccount } from "@/lib/api/auth";
import DeleteAccountDialog from "@/components/auth/DeleteAccountDialog";
import { useDict } from "@/components/i18n/DictProvider";

export default function AccountPage() {
  const { dict } = useDict();
  const router = useRouter();
  const { user, logout: logoutStore } = useAuthStore();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDeleteAccount = async () => {
    setIsDeleting(true);
    try {
      await deleteAccount();
    } catch {
      setIsDeleting(false);
      return;
    }
    logoutStore();
    router.push("/");
  };

  return (
    <div className="max-w-[600px] mx-auto px-6 py-8">
      <h1 className="text-[22px] font-bold text-text-primary mb-8">
        {dict.account.title}
      </h1>

      {/* 계정 정보 */}
      <section className="bg-bg-secondary border border-border-dark rounded-xl p-6 mb-6">
        <h2 className="text-sm font-medium text-text-muted uppercase tracking-wider mb-4">
          {dict.account.info}
        </h2>
        <div className="space-y-4">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-full bg-gradient-to-br from-accent to-[#60a5fa] flex items-center justify-center text-lg font-bold text-white shrink-0">
              {user?.name?.[0] ?? "U"}
            </div>
            <div>
              <div className="text-base font-medium text-text-primary">
                {user?.name}
              </div>
              <div className="text-sm text-text-muted">
                {user?.email}
              </div>
            </div>
          </div>
          <div className="flex items-center justify-between pt-3 border-t border-border-dark">
            <span className="text-sm text-text-secondary">{dict.account.plan}</span>
            <span className="text-sm font-medium text-text-primary capitalize">
              {user?.plan ?? "free"}
            </span>
          </div>
        </div>
      </section>

      {/* 위험 구역 */}
      <section className="border border-border-dark rounded-xl p-6">
        <h2 className="text-sm font-medium text-text-muted uppercase tracking-wider mb-3">
          {dict.account.dangerZone}
        </h2>
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm font-medium text-text-primary">
              {dict.deleteAccount.title}
            </div>
            <div className="text-xs text-text-muted mt-0.5">
              {dict.account.deleteHint}
            </div>
          </div>
          <button
            onClick={() => setDeleteDialogOpen(true)}
            className="px-4 py-2 rounded-md text-sm font-medium text-danger border border-danger/30 hover:bg-danger/10 transition-colors cursor-pointer shrink-0"
          >
            {dict.deleteAccount.confirm}
          </button>
        </div>
      </section>

      <DeleteAccountDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        onConfirm={handleDeleteAccount}
        isDeleting={isDeleting}
      />
    </div>
  );
}
