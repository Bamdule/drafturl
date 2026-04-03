# DraftURL — 사용자 획득 전략

> 작성일: 2026-04-01
> 최종 수정일: 2026-04-03
> 상태: active

---

## 현황 진단

| 항목 | 상태 |
|------|------|
| MCP Registry 등록 | ✅ 완료 (2026-04-02, active) |
| GSC 등록 | ✅ 완료 — canonical 버그 수정 후 색인 재요청 필요 |
| SEO 콘텐츠 | ❌ 0개 |
| 커뮤니티 런칭 | ❌ 0회 |
| X(Twitter) 계정 | ❌ 미생성 |
| 신규 회원 | ❌ 없음 |

**핵심 문제**: 기능은 충분한데 "발견"되지 않고 있음

---

## Phase 1: 발견되게 만들기 (1~2주)

### 1. X(Twitter) 데모 포스팅 ← 지금 당장

**왜 먼저?** AI 도구 커뮤니티가 가장 활발한 채널. 비용 0, 시간 30분, 타겟 정확.

**계정 전략**
- 개인 계정으로 시작 (서비스 계정은 팔로워 0에서 반응 없음)
- 핸들: `@drafturl` 가능 여부 확인 후 선점

**포스팅 형식**
```
[GIF: Claude 출력 붙여넣기 → 3초 → URL 복사]

Claude가 만든 페이지를 바로 공유하고 싶었는데
Artifacts는 URL이 없고, GitHub Pages는 너무 복잡해서 만들었습니다.

붙여넣기 → URL. 그게 전부입니다.
drafturl.com

#ClaudeAI #cursor #MCP
```

**GIF 녹화**: Kap (맥 무료) 으로 실제 사용 화면 30초 녹화

---

### 2. Reddit 타겟 포스팅

**타겟 서브레딧**

| 서브레딧 | 각도 | 주의 |
|---------|------|------|
| `r/ClaudeAI` | "Share your Claude artifacts as a real URL" | 셀프프로모 규칙 확인 |
| `r/ChatGPT` | "Stop copy-pasting to Notion, just get a URL" | 피드백 요청 프레이밍 |
| `r/cursor` | MCP 연동 시연 | 개발자 대상, 기술적 내용 |

**프레이밍**: "나 이거 만들었는데 피드백 줘" → 홍보글보다 훨씬 잘 받힘

---

### 3. GSC 색인 재요청

- v0.9.3 배포 후 `drafturl.com/ko`, `drafturl.com/en` 색인 재요청
- canonical 버그 수정됨 → 이제 재요청해도 의미 있음
- FAQ/HowTo 리치 스니펫 반영 확인

---

### 4. SEO 블로그 콘텐츠 (3~5개)

**왜?** Tiiny Host는 SEO 블로그로 $20K MRR 달성. 장기 무비용 유입.

**우선순위 주제** (검색 의도 명확한 것부터)

| 주제 | 타겟 키워드 | 언어 |
|------|------------|------|
| Claude Artifacts URL 공유하는 방법 | "claude artifacts share url" | EN 우선 |
| Claude가 만든 HTML 즉시 배포하기 | "claude html 배포" | KO |
| ChatGPT 출력을 웹페이지로 공유하기 | "chatgpt output share webpage" | EN |
| DraftURL vs Tiiny Host 비교 | "tiiny host alternative" | EN |
| MCP로 AI가 직접 웹페이지를 만드는 방법 | "mcp deploy webpage" | EN/KO |

**블로그 위치**: 별도 서브도메인(`blog.drafturl.com`) 또는 `/blog` 경로

---

### 5. Product Hunt 런칭

- **타이밍**: 블로그 2~3개 게시 후 (백링크 + 신뢰도 확보 뒤)
- **준비물**: 스크린샷, GIF, 한 줄 소개, 메이커 코멘트
- **효과**: 초기 트래픽 + 백링크 + 커뮤니티 피드백

---

## Phase 2: 사용자 반응 관찰 (2~4주)

- Umami 데이터: 유입 경로, 이탈 지점, 전환율
- GSC 성과: 노출 키워드, CTR
- 문의 확인: Resend 알림으로 실제 피드백 수집
- MCP 사용량: 도구 호출 빈도

---

## Phase 3: 반응에 따라 방향 결정

| 반응 | 방향 |
|------|------|
| MCP 사용자가 많다 | MCP 기능 강화 (버전 히스토리, AI 수정) |
| 블로그 유입이 많다 | 콘텐츠 마케팅 확대, 유료 플랜 도입 |
| 특정 키워드 유입 집중 | 해당 키워드 중심으로 콘텐츠 확장 |
| 반응이 없다 | 타겟 재정의 또는 피벗 고려 |

---

## 원칙

- **기능 개발보다 사용자 획득 우선**: 쓰는 사람이 없는 기능은 의미 없음
- **작은 실험 → 데이터 → 판단**: 가설 검증 후 투자 결정
- **코드보다 글쓰기**: 현 단계에서 블로그 1편이 기능 1개보다 가치 있음
