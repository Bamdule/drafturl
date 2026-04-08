# DraftURL -- 운영 (비용 & 성능)

> 상태: active
> 작성일: 2026-03-21

---

## 요약

- MVP 월 ~$5(Railway BE만 유료), 스케일 시 ~$65/월(Vercel Pro + Railway Pro + Supabase Pro)
- 문서 서빙 < 800ms(캐시 미스), < 100ms(캐시 히트), 문서 생성 < 1.5s 등 성능 목표 설정
- 문서 서빙 API에 Cache-Control 헤더 + Next.js ISR/fetch 캐시 + revalidatePath로 캐싱 전략 적용

---

## 1. 비용 추산

### MVP 출시 후 1~2개월 (일 100개 문서 가정)

| 항목 | 비용 | 비고 |
|------|------|------|
| **Vercel (FE)** | **$0** | Hobby 플랜 |
| **Railway (BE)** | **$5/월** | Hobby 플랜 (512MB RAM, 상시 가동) |
| **Supabase PostgreSQL** | **$0** | Free 티어 (500MB DB, 50K 행) |
| **Cloudflare R2 저장** | **$0** | 월 ~150MB (일 100개 x 50KB 가정, 비로그인 24h 만료 + 삭제를 고려하면 실 사용량은 더 낮음) << 10GB 무료 |
| **R2 쓰기 (Class A)** | **$0** | 월 ~3,000회 << 1M 무료 |
| **R2 읽기 (Class B)** | **$0** | 월 ~10,000회 << 10M 무료 |
| **R2 이그레스** | **$0** | 항상 무료 |
| **도메인** | **~$10/년** | .com 도메인 |
| **총합** | **~$5/월** | 백엔드 호스팅 비용만 발생 |

### 스케일 시 (사용자 1,000명, 프로덕션)

| 항목 | 비용 |
|------|------|
| Vercel Pro (FE) | $20/월 |
| Railway Pro (BE) | $20/월 (1GB RAM, 자동 스케일) |
| Supabase Pro (DB) | $25/월 |
| R2 | $0 (10GB 미만) |
| **총합** | **~$65/월** |

---

## 2. 성능 목표

| 항목 | 목표 | 전략 |
|------|------|------|
| 문서 서빙 응답 시간 | < 800ms (캐시 미스), < 100ms (Next.js 캐시 히트) | Spring Boot 응답 캐싱 헤더 + Next.js ISR/fetch 캐시 |
| 문서 생성 응답 시간 | < 1.5s | 2단계 커밋이지만 단순 연산이므로 충분 |
| 에디터 로딩 | < 2s | Monaco Editor 동적 import + 코드 스플리팅 |
| 미리보기 갱신 | < 300ms (타이핑 후) | debounce 300ms, 클라이언트 렌더링 (API 호출 불필요) |
| Spring Boot 콜드 스타트 | < 5s | Railway 상시 가동, Spring Boot 최적화 |

---

## 3. 캐싱 전략

- 문서 서빙 API(`/view`) 응답에 `Cache-Control` 헤더 설정 (상세 헤더 값은 [백엔드 API 명세](../backend/api.md)의 문서 서빙 API 참조)
- Next.js 서버 컴포넌트에서 `fetch` 호출 시 `next: { revalidate: 300 }` 옵션으로 ISR 캐싱
- 문서 수정 시: `revalidatePath('/${slug}')` 호출
- 문서 삭제 시: `revalidatePath('/${slug}')` 호출

---

## 관련 문서

- [인프라 설계 개요](README.md) -- 기술 스택, 확장성 계획
- [배포](deployment.md) -- 호스팅 선택 근거
- [백엔드 API 명세](../backend/api.md) -- Cache-Control 헤더 설정
