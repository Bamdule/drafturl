"use client";

import { useCallback, useState } from "react";
import Link from "next/link";
import { createInquiry } from "@/lib/api/inquiry";
import { useAuthStore } from "@/lib/store/useAuthStore";
import { useDict } from "@/components/i18n/DictProvider";
import { ApiError } from "@/lib/api/types";

interface FormErrors {
  email?: string;
  subject?: string;
  message?: string;
}

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function ContactPage() {
  const { dict } = useDict();
  const t = dict.contact;
  const { user } = useAuthStore();

  const [type, setType] = useState("CONTACT");
  const [email, setEmail] = useState(user?.email ?? "");
  const [name, setName] = useState(user?.name ?? "");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);

  const validate = useCallback((): boolean => {
    const newErrors: FormErrors = {};

    if (!email.trim()) {
      newErrors.email = t.emailRequired;
    } else if (!validateEmail(email)) {
      newErrors.email = t.emailInvalid;
    }

    if (!subject.trim()) {
      newErrors.subject = t.subjectRequired;
    }

    if (!message.trim()) {
      newErrors.message = t.messageRequired;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [email, subject, message, t]);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setServerError(null);

      if (!validate()) return;
      if (isLoading) return;

      setIsLoading(true);
      try {
        await createInquiry({
          type,
          email: email.trim(),
          name: name.trim() || undefined,
          subject: subject.trim(),
          message: message.trim(),
        });
        setIsSubmitted(true);
      } catch (err) {
        if (err instanceof ApiError) {
          setServerError(err.message);
        } else {
          setServerError("An error occurred. Please try again.");
        }
      } finally {
        setIsLoading(false);
      }
    },
    [validate, isLoading, type, email, name, subject, message],
  );

  const handleNewInquiry = useCallback(() => {
    setType("CONTACT");
    setEmail(user?.email ?? "");
    setName(user?.name ?? "");
    setSubject("");
    setMessage("");
    setErrors({});
    setServerError(null);
    setIsSubmitted(false);
  }, [user]);

  const inputClass =
    "flex h-10 w-full rounded-lg border border-border-dark bg-bg-primary px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-colors";

  if (isSubmitted) {
    return (
      <div className="flex-1 flex items-center justify-center px-4 py-8">
        <div className="w-full max-w-[420px]">
          <div className="bg-bg-secondary border border-border-dark rounded-2xl p-8 shadow-lg shadow-black/20 text-center">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center mx-auto mb-4 text-xl font-bold text-white">
              D
            </div>
            <h1 className="text-xl font-bold text-text-primary mb-2">
              {t.successTitle}
            </h1>
            <p className="text-sm text-text-secondary mb-6">
              {t.successMessage}
            </p>
            <div className="flex flex-col gap-3">
              <button
                type="button"
                onClick={handleNewInquiry}
                className="h-10 w-full rounded-lg bg-gradient-to-r from-accent to-[#6a48e8] text-white text-sm font-medium hover:opacity-90 transition-opacity cursor-pointer"
              >
                {t.newInquiry}
              </button>
              <Link
                href="/"
                className="h-10 w-full rounded-lg border border-border-dark bg-bg-primary text-text-secondary text-sm font-medium hover:bg-bg-secondary transition-colors flex items-center justify-center"
              >
                {t.backToHome}
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-[420px]">
        {/* Card */}
        <div className="bg-bg-secondary border border-border-dark rounded-2xl p-8 shadow-lg shadow-black/20">
          {/* Header */}
          <div className="text-center mb-5">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-[#a78bfa] flex items-center justify-center mx-auto mb-4 text-xl font-bold text-white">
              D
            </div>
            <h1 className="text-xl font-bold text-text-primary">
              {t.title}
            </h1>
          </div>

          {/* Contact Form */}
          <form onSubmit={handleSubmit} className="flex flex-col gap-3 text-left">
            <div>
              <label
                htmlFor="type"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                {t.typeLabel}
              </label>
              <select
                id="type"
                value={type}
                onChange={(e) => setType(e.target.value)}
                className={inputClass}
              >
                <option value="CONTACT">{t.typeContact}</option>
                <option value="BUG_REPORT">{t.typeBug}</option>
              </select>
            </div>

            <div>
              <label
                htmlFor="email"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                {t.email}
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (errors.email)
                    setErrors((prev) => ({ ...prev, email: undefined }));
                }}
                placeholder={t.emailPlaceholder}
                className={inputClass}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-danger">{errors.email}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="name"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                {t.name}
              </label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t.namePlaceholder}
                className={inputClass}
              />
            </div>

            <div>
              <label
                htmlFor="subject"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                {t.subject}
              </label>
              <input
                id="subject"
                type="text"
                value={subject}
                onChange={(e) => {
                  setSubject(e.target.value);
                  if (errors.subject)
                    setErrors((prev) => ({ ...prev, subject: undefined }));
                }}
                placeholder={t.subjectPlaceholder}
                className={inputClass}
              />
              {errors.subject && (
                <p className="mt-1 text-xs text-danger">{errors.subject}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="message"
                className="block text-sm font-medium text-text-secondary mb-1.5"
              >
                {t.message}
              </label>
              <textarea
                id="message"
                value={message}
                onChange={(e) => {
                  setMessage(e.target.value);
                  if (errors.message)
                    setErrors((prev) => ({ ...prev, message: undefined }));
                }}
                placeholder={t.messagePlaceholder}
                maxLength={5000}
                rows={5}
                className={`${inputClass} h-auto resize-none`}
              />
              {errors.message && (
                <p className="mt-1 text-xs text-danger">{errors.message}</p>
              )}
            </div>

            {serverError && (
              <div className="rounded-lg bg-danger/10 border border-danger/20 px-3 py-2.5 text-sm text-danger">
                {serverError}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="h-10 w-full rounded-lg bg-gradient-to-r from-accent to-[#6a48e8] text-white text-sm font-medium hover:opacity-90 transition-opacity disabled:opacity-70 disabled:pointer-events-none cursor-pointer mt-1"
            >
              {isLoading ? t.submitting : t.submit}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
