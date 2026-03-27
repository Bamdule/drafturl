"use client";

import { create } from "zustand";
import type { DocType } from "@/lib/constants";

const SAMPLE_HTML = `<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>주간 보고서</title>
  <style>
    body { font-family: system-ui; max-width: 680px; margin: 40px auto; padding: 0 20px; color: #1a1a2e; }
    h1 { color: #7c5cfc; border-bottom: 2px solid #e8e8f0; padding-bottom: 8px; }
    table { width: 100%; border-collapse: collapse; margin: 20px 0; }
    th, td { border: 1px solid #ddd; padding: 10px 14px; text-align: left; }
    th { background: #f5f5ff; font-weight: 600; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
    .done { background: #d1fae5; color: #065f46; }
    .progress { background: #fef3c7; color: #92400e; }
  </style>
</head>
<body>
  <h1>2026년 3월 주간 보고서</h1>
  <p>AI 개발팀 — 작성자: 김개발</p>

  <h2>이번 주 성과</h2>
  <table>
    <tr><th>태스크</th><th>상태</th><th>비고</th></tr>
    <tr><td>API 엔드포인트 구현</td><td><span class="badge done">완료</span></td><td>문서 CRUD 전체</td></tr>
    <tr><td>미리보기 기능</td><td><span class="badge done">완료</span></td><td>HTML + MD 지원</td></tr>
    <tr><td>인증 시스템</td><td><span class="badge progress">진행중</span></td><td>OAuth 연동 중</td></tr>
  </table>

  <h2>다음 주 계획</h2>
  <ul>
    <li>인증 시스템 완료 및 테스트</li>
    <li>대시보드 UI 개발</li>
    <li>CDN 캐싱 설정</li>
  </ul>
</body>
</html>`;

const SAMPLE_MARKDOWN = `# API 문서 — DraftURL REST API

> v1.0 | 최종 수정: 2026-03-24

## 인증

모든 API 요청에는 \`Authorization\` 헤더가 필요합니다.

\`\`\`
Authorization: Bearer YOUR_API_KEY
\`\`\`

## 엔드포인트

### 문서 생성

\`\`\`
POST /api/documents
\`\`\`

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| content | string | O | HTML 또는 Markdown 내용 |
| type | string | O | \`html\` 또는 \`markdown\` |
| title | string | X | 문서 제목 |

**응답 예시:**

\`\`\`json
{
  "url": "https://drafturl.com/xK9mP2nQ",
  "slug": "xK9mP2nQ",
  "expiresAt": "2026-03-25T15:30:00Z"
}
\`\`\`

### 문서 조회

\`\`\`
GET /api/documents/:slug
\`\`\`

---

*이 문서는 DraftURL로 공유되었습니다.*`;

interface EditorState {
  content: string;
  docType: DocType;
  title: string;
  isDemo: boolean;
  setContent: (content: string) => void;
  setDocType: (docType: DocType) => void;
  setTitle: (title: string) => void;
  setIsDemo: (isDemo: boolean) => void;
  reset: () => void;
  loadDocument: (content: string, docType: DocType, title: string) => void;
}

const initialState = {
  content: "",
  docType: "html" as DocType,
  title: "",
  isDemo: false,
};

export const useEditorStore = create<EditorState>((set) => ({
  ...initialState,

  setContent: (content) => set({ content }),
  setDocType: (docType) => set({ docType }),
  setTitle: (title) => set({ title }),
  setIsDemo: (isDemo) => set({ isDemo }),
  reset: () => set({ ...initialState }),
  loadDocument: (content, docType, title) =>
    set({ content, docType, title, isDemo: false }),
}));

export { SAMPLE_HTML, SAMPLE_MARKDOWN };
