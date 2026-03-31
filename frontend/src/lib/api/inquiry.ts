import { apiFetch } from "./client";

interface CreateInquiryData {
  type: string;
  email: string;
  name?: string;
  subject: string;
  message: string;
  documentId?: string;
}

interface InquiryResponse {
  id: string;
  type: string;
  createdAt: string;
}

export function createInquiry(
  data: CreateInquiryData,
): Promise<InquiryResponse> {
  return apiFetch<InquiryResponse>("/api/v1/inquiries", {
    method: "POST",
    body: data,
    auth: false,
  });
}
