# DraftURL -- 보안 설계

> 상태: active
> 작성일: 2026-03-21

---

## 요약

- 입력 검증(5MB 바이트 크기, doc_type 패턴)과 Rate Limiting(비로그인 10회/분, 로그인 30회/분)으로 기본 방어
- 사용자 HTML은 CDN 직접 서빙 + sandbox iframe(`allow-same-origin allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox`)으로 격리, Markdown은 완전 샌드박싱(`sandbox=""`)으로 격리
- JWT는 httpOnly+Secure+SameSite=Lax 쿠키에 저장하며, Refresh Token Rotation으로 탈취 감지/대응
- 서버 새니타이징(jsoup)으로 iframe/object/embed 태그와 javascript: URL 제거

---

## 1. 입력 검증

| 항목 | 검증 내용 | 구현 위치 |
|------|----------|-----------|
| content 크기 | 최대 5MB | 커스텀 validator로 바이트 크기를 검증한다. `@Size`는 문자열 길이 제한이므로 바이트 제한에는 적합하지 않다. Spring `maxRequestSize`와 함께 적용 |
| doc_type | `html` 또는 `markdown` | Bean Validation `@Pattern` |
| Rate Limiting | Rate Limiting 정책은 [api.md](api.md) 섹션 1 참조 | `RateLimitFilter` |

---

## 2. HTML 서빙 보안

| 위협 | 대응책 |
|------|--------|
| XSS (사용자 HTML 내 스크립트) | sandbox iframe 격리 -- 상세 정책은 섹션 5 참조 |
| 쿠키 탈취 | sandbox iframe의 origin 격리 -- 상세 정책은 섹션 5 참조 |
| 외부 데이터 유출 (iframe 내 fetch/XHR) | CSP 헤더로 외부 네트워크 요청 차단 -- 상세 정책은 섹션 5 참조 |
| 악성 HTML (iframe, object, embed) | 서버 새니타이징 -- 상세 정책은 섹션 5 참조 |
| javascript: URL 스킴 | 서버 새니타이징 -- 상세 정책은 섹션 5 참조 |

---

## 3. 인증 보안

| 위협 | 대응책 |
|------|--------|
| JWT 탈취 | `httpOnly` + `Secure` + `SameSite=Lax` 쿠키 저장 |
| CSRF (일반) | `SameSite=Lax` 쿠키 + JWT Bearer 토큰 이중 확인 |
| CSRF (OAuth2) | `state` 파라미터 생성/쿠키 저장/검증 ([auth.md](auth.md) 섹션 1 참조) |
| 리프레시 토큰 재사용 | Refresh Token Rotation: 갱신 시 이전 토큰 즉시 revoke + 새 토큰 발급 ([auth.md](auth.md) 섹션 3 참조). revoked 토큰 재사용 시 해당 사용자 전체 토큰 무효화 (탈취 감지) |
| 브루트포스 | Rate Limiting |

---

## 4. 비로그인 문서 보안

- 자동 만료 ([도메인 모델](../domain.md) DR-1 참조)
- slug가 nanoid 8자리 (경우의 수: 2.8조 개)로 URL 추측 불가
- 스케줄러로 만료 문서 주기적 삭제 (R2 파일 + DB 레코드)

---

## 5. 사용자 HTML 처리 정책

사용자가 업로드하는 HTML에는 임의의 JavaScript가 포함될 수 있으므로, 보안과 기능성 사이의 균형을 명확히 정의한다.

**핵심 원칙**:

1. **CDN 직접 서빙으로 origin 분리**: 문서 콘텐츠는 `files.drafturl.com`(Cloudflare R2 CDN)에서 직접 서빙되므로, 메인 도메인(`drafturl.com`)과 origin이 자동으로 분리된다. 이를 통해 쿠키 탈취 등의 위협이 원천 차단된다.

2. **HTML 문서 -- 기능성과 보안의 균형**: LLM 생성 HTML에는 차트(Chart.js), 인터랙티브 데모 등 스크립트가 빈번히 포함된다. iframe `sandbox="allow-same-origin allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox"` 속성으로 스크립트 실행과 폼/팝업을 허용하되, CDN origin 분리로 메인 도메인 쿠키에는 접근 불가하다. HTML은 CDN URL을 iframe `src`로 직접 로드한다.

3. **Markdown 문서 -- 완전 샌드박싱**: Markdown은 서버에서 원본 텍스트를 CDN에 저장하고, 프론트엔드가 `fetch(contentUrl)`로 로드한 뒤 unified(remark+rehype)로 HTML 변환하여 iframe `srcDoc`에 주입한다. `sandbox=""`(빈 값)로 완전 샌드박싱되어 스크립트 실행이 차단된다.

4. **서버 새니타이징으로 위험 태그 제거**: `<iframe>`, `<object>`, `<embed>` 태그와 `javascript:` URL 스킴은 서버에서 제거한다.

---

## 6. 기술적 리스크

| 리스크 | 심각도 | 완화 전략 |
|--------|--------|----------|
| **AI API 비용 급증** | 높음 | 플랜별 사용량 제한 + 캐싱 + 경량 모델(Haiku)을 간단한 수정에 라우팅. [서비스 기능 정의](../../product.md) 섹션 6의 비용 추정 참조 |
| **정적 파일 저장 비용** | 중간 | 비활성 문서 자동 아카이브, Free 플랜 7일 만료 정책으로 스토리지 증가 억제 |
| **악성 콘텐츠 호스팅** | 높음 | 콘텐츠 스캐닝, 신고 시스템, 이용약관 명시 |
| **XSS 등 보안 이슈** | 높음 | CDN origin 분리(`files.drafturl.com`) + sandbox iframe으로 격리. HTML은 `sandbox="allow-same-origin allow-scripts allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox"`, Markdown은 `sandbox=""`. 상세 정책은 위 "HTML 처리 정책" 참조 |
| **Rate Limiting 우회/DDoS** | 중간 | 상세는 [api.md](api.md) 참조 |

---

## 관련 문서

- [인증/인가 설계](auth.md) -- OAuth2 플로우, JWT, Refresh Token Rotation 상세
- [핵심 로직](logic.md) -- HTML 서빙 래퍼, sandbox iframe 구현
- [API 명세](api.md) -- 에러 코드 총괄
- [백엔드 설계 개요](README.md) -- 패키지 구조, 구현 주의사항
