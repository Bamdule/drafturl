# 자체 리뷰: OAuth2 State HMAC 서명 기반 CSRF 방어

> 작성일: 2026-03-23
> 대상: C-1 OAuth2 state 검증 CSRF 방어 피드백 반영

---

## 변경 사항 요약

| 파일 | 변경 내용 |
|------|-----------|
| `backend/.../global/auth/OAuthStateProvider.java` | **신규** - HMAC-SHA256 서명 기반 state 생성/검증 컴포넌트 |
| `backend/.../domain/user/controller/AuthController.java` | state 생성 API 엔드포인트 추가 (`GET /api/v1/auth/oauth2/state`) |
| `backend/.../domain/user/controller/response/OAuthStateResponse.java` | **신규** - state 응답 DTO |
| `backend/.../domain/user/usecase/OAuthLoginUseCase.java` | `validateState()` 제거, `OAuthStateProvider.validateState()` 위임 |
| `frontend/src/lib/api/auth.ts` | `generateOAuthState()` API 함수 추가 |
| `frontend/src/lib/api/types.ts` | `OAuthStateResponse` 타입 추가 |
| `frontend/src/app/auth/login/page.tsx` | 프론트 자체 state 생성 -> 백엔드 API 호출로 변경 |
| `docs/design/backend/auth.md` | State 흐름 설명을 HMAC 이중 검증 구조로 갱신 |
| `backend/.../global/auth/OAuthStateProviderTest.java` | **신규** - 단위 테스트 (8개 케이스) |

---

## 리뷰 체크리스트

### 보안

- [x] **HMAC 서명 검증**: state에 포함된 서명을 서버 비밀키로 재계산하여 비교 -- 공격자가 임의의 state를 생성할 수 없음
- [x] **Timing attack 방어**: `constantTimeEquals()` 상수 시간 비교 사용
- [x] **Replay 완화**: 5분 TTL로 만료된 state 거부. nonce로 동일 timestamp에서의 중복 방지
- [x] **이중 검증**: 프론트엔드(쿠키 비교) + 백엔드(HMAC 서명) 2중 방어
- [x] **Secret 재사용**: JWT secret을 HMAC key로 재사용 -- 별도 secret 관리 부담 없음

### 아키텍처

- [x] **Stateless 유지**: 서버 측 세션/DB 저장 없이 검증 가능
- [x] **단일 책임**: `OAuthStateProvider`가 state 생성/검증을 전담
- [x] **기존 프론트엔드 콜백 흐름 유지**: 콜백 페이지의 쿠키 비교 로직은 그대로 동작

### 잠재적 개선 사항 (향후)

1. **state 일회성 보장**: 현재는 5분 내 동일 state를 재사용할 수 있음. Redis 등으로 사용된 nonce를 추적하면 완전한 replay 방지가 가능하지만 Stateless 원칙과 충돌하므로 MVP에서는 TTL로 충분
2. **JWT secret과 state secret 분리**: 현재는 동일 secret을 공유. 보안 심화 시 별도 `app.oauth-state.secret` 프로퍼티 분리 검토
3. **Rate limiting**: state 생성 API에 대한 rate limit 적용 검토 (기존 RateLimitFilter가 있다면 자동 적용됨)

### 판정

PASS -- CSRF 취약점이 해결되었고, Stateless 아키텍처 원칙을 유지하며, 프론트/백엔드 이중 검증으로 방어 계층이 강화되었다.
