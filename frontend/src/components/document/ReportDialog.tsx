"use client";

import { useCallback, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { useDict } from "@/components/i18n/DictProvider";
import { createInquiry } from "@/lib/api/inquiry";
import { ApiError } from "@/lib/api/types";

interface ReportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  documentId: string;
  onSubmitted: () => void;
}

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function ReportDialog({
  open,
  onOpenChange,
  documentId,
  onSubmitted,
}: ReportDialogProps) {
  const { dict } = useDict();
  const t = dict.report;

  const [email, setEmail] = useState("");
  const [reason, setReason] = useState("");
  const [emailError, setEmailError] = useState<string | null>(null);
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const handleSubmit = useCallback(async () => {
    setEmailError(null);
    setReasonError(null);
    setServerError(null);

    let hasError = false;

    if (!email.trim() || !validateEmail(email)) {
      setEmailError(t.emailRequired);
      hasError = true;
    }

    if (!reason.trim()) {
      setReasonError(t.reasonRequired);
      hasError = true;
    }

    if (hasError) return;
    if (isSubmitting) return;

    setIsSubmitting(true);
    try {
      await createInquiry({
        type: "CONTENT_REPORT",
        email: email.trim(),
        subject: "콘텐츠 신고: " + documentId,
        message: reason.trim(),
        documentId,
      });
      setEmail("");
      setReason("");
      onOpenChange(false);
      onSubmitted();
    } catch (err) {
      if (err instanceof ApiError) {
        setServerError(err.message);
      } else {
        setServerError("An error occurred.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }, [email, reason, isSubmitting, documentId, onOpenChange, onSubmitted, t]);

  const inputClass =
    "flex h-10 w-full rounded-lg border border-border-dark bg-bg-primary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t.title}</DialogTitle>
          <DialogDescription>{t.description}</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-3 mt-2">
          <div>
            <label
              htmlFor="report-email"
              className="block text-sm font-medium text-text-secondary mb-1.5"
            >
              {t.email}
            </label>
            <input
              id="report-email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (emailError) setEmailError(null);
              }}
              placeholder={t.emailPlaceholder}
              className={inputClass}
            />
            {emailError && (
              <p className="mt-1 text-xs text-danger">{emailError}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="report-reason"
              className="block text-sm font-medium text-text-secondary mb-1.5"
            >
              {t.reason}
            </label>
            <textarea
              id="report-reason"
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                if (reasonError) setReasonError(null);
              }}
              placeholder={t.reasonPlaceholder}
              rows={4}
              className={`${inputClass} h-auto resize-none`}
            />
            {reasonError && (
              <p className="mt-1 text-xs text-danger">{reasonError}</p>
            )}
          </div>

          {serverError && (
            <div className="rounded-lg bg-danger/10 border border-danger/20 px-3 py-2.5 text-sm text-danger">
              {serverError}
            </div>
          )}
        </div>

        <DialogFooter className="mt-4">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
          >
            {t.cancel}
          </Button>
          <Button
            variant="destructive"
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? t.submitting : t.submit}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
