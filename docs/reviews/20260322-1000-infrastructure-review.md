# infrastructure.md 검토 결과

> 검토일: 2026-03-22
> 검토 대상: `docs/architecture/infrastructure.md`
> 참조 문서: `backend.md`, `product.md`

---

## 요약

infrastructure.md는 인프라/배포 문서로서 **전반적으로 완성도가 높다**. 기술 스택 선정 근거, 비용 추산, CI/CD 파이프라인, 개발 환경 구성이 구현에 충분한 수준으로 기술되어 있다. 다만 환경변수 불일치(3건), backend.md와의 중복(2건), Docker Compose 섹션의 실행 가이드 부재, Sentry 환경변수 누락 등 실질적인 개선 사항이 존재한다. Critical 이슈는 없으며, Major 5건, Minor 4건을 식별했다.

---

## 잘된 점

1. **개발 환경 Docker Compose 섹션(섹션 2)이 체계적**: MinIO를 R2 대체로 사용하는 이유, Flyway 마이그레이션 동기화, Cloudflare Tunnel 활용법까지 로컬 개발에 필요한 내용이 빠짐없이 정리되어 있다.

2. **비용 추산의 단계별 구분이 명확**: MVP($5/월)와 스케일($65/월) 시나리오를 분리하고 R2 무료 티어 한도까지 구체적으로 명시하여 의사결정에 도움이 된다.

3. **배포 아키텍처 다이어그램과 CI/CD 워크플로우가 구현 가능한 수준**: paths 필터, PR 체크 조건 등 실제 GitHub Actions 설정에 바로 적용할 수 있는 수준이다.

4. **설계 결정 및 트레이드오프 기록**: Railway 선택 이유와 대안 비교가 명확하며, 다른 문서로의 참조 링크도 적절하다.

5. **확장성 계획(섹션 10)의 MVP vs 확장 대비가 간결**: Rate Limiting(ConcurrentHashMap -> Redis), 스케줄러(@Scheduled -> ShedLock) 전환 경로가 명시되어 있다.

---

## Major (주요 개선사항)

### M-1. 환경변수 불일치 -- infrastructure.md vs backend.md

**문제**: infrastructure.md 섹션 7의 프로덕션 환경변수 목록에 `DATABASE_USER`, `DATABASE_PASSWORD`가 없다. 그러나 backend.md 섹션 11의 `application.yml`에서는 `${DATABASE_USER}`, `${DATABASE_PASSWORD}`를 별도로 참조한다.

infrastructure.md 섹션 7:
```
| DATABASE_URL | Railway | PostgreSQL 연결 |
```

backend.md 섹션 11:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
```

**영향**: Railway 배포 시 `DATABASE_USER`, `DATABASE_PASSWORD` 환경변수를 설정하지 않아 Spring Boot가 기동 실패할 수 있다.

**제안**: 두 가지 방안 중 하나를 선택하여 통일:
- (A) `DATABASE_URL`에 JDBC URL 형태로 사용자/비밀번호를 포함하고 (`jdbc:postgresql://...?user=...&password=...`), `application.yml`에서 `DATABASE_USER`/`PASSWORD`를 제거
- (B) infrastructure.md 환경변수 표에 `DATABASE_USER`, `DATABASE_PASSWORD`를 추가

### M-2. SENTRY_DSN 환경변수 누락

**문제**: backend.md 섹션 12에서 `SENTRY_DSN` 환경변수로 Sentry를 활성화한다고 명시하나, infrastructure.md 섹션 7 환경변수 표에 `SENTRY_DSN`이 없다.

**영향**: Railway 배포 시 Sentry가 비활성화 상태로 운영되어 에러 모니터링이 작동하지 않을 수 있다.

**제안**: 섹션 7 환경변수 표에 `SENTRY_DSN | Railway | Sentry 에러 추적 (프로덕션)` 행을 추가한다.

### M-3. Docker Compose 실행 가이드 부재

**문제**: 섹션 2에 서비스 구성과 환경변수는 상세히 기술되어 있으나, 실제 `docker-compose.yml` 파일의 위치나 실행 명령(`docker compose up -d`), 초기 설정 절차(MinIO 버킷 생성 등)가 없다.

**영향**: 새로운 개발자가 로컬 환경을 구축할 때 시행착오가 발생할 수 있다.

**제안**: 다음 항목을 추가:
- `docker-compose.yml` 파일의 위치 (프로젝트 루트)
- 최초 실행 시 필요한 사전 조건 (Docker Desktop 설치 등)
- MinIO 최초 실행 시 버킷 자동 생성 방법 (init 스크립트 또는 `mc mb` 명령)
- `docker compose up -d` 이후 서비스별 헬스체크 확인법

### M-4. Dockerfile에 멀티스테이지 빌드 미적용

**문제**: 섹션 6의 Dockerfile이 `COPY build/libs/*.jar app.jar` 방식으로 호스트에서 빌드한 JAR를 복사한다. CI/CD 워크플로우(섹션 5)에서도 "Gradle 빌드 후 Docker 이미지 빌드"라고 설명하는데, 이 경우 CI 환경에서의 JDK 버전과 Dockerfile의 JRE 버전이 불일치할 가능성이 있다.

**제안**: 멀티스테이지 빌드로 전환하면 CI와 Dockerfile 간 JDK 버전 일관성을 보장할 수 있다. 다만, CI에서 빌드 -> Docker 이미지로 복사하는 현재 전략도 유효하므로, 이 경우 CI의 JDK 21과 Dockerfile의 `eclipse-temurin:21-jre-alpine`이 호환되어야 한다는 전제를 명시적으로 문서화하면 된다.

### M-5. 로컬 환경변수 표에 OAuth2 관련 변수 누락

**문제**: 섹션 2의 `.env.local` 표에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`이 없다. 로컬에서 소셜 로그인 테스트가 필요한 경우(특히 Cloudflare Tunnel로 OAuth 콜백 테스트 시) 이 변수들이 필요하다.

**제안**: `.env.local` 표에 OAuth2 변수를 추가하되, 로컬 전용 값이 아니라 개발용 OAuth App의 credential을 사용해야 한다는 점을 비고에 명시한다. 또는, 로컬에서는 OAuth2를 테스트하지 않는다는 결정이라면 그 점을 명시한다.

---

## Minor (사소한 개선사항)

### m-1. backend.md와의 중복 -- 캐싱 전략

**문제**: infrastructure.md 섹션 9의 "캐싱 전략" 내용이 backend.md 섹션 7.2의 `Cache-Control` 헤더 설명과 중복된다. 두 문서 모두 `Cache-Control: public, max-age=300, s-maxage=3600, stale-while-revalidate=86400` 수준의 정보를 담고 있다.

**제안**: infrastructure.md에서는 캐싱의 인프라적 관점(ISR 설정값, revalidate 전략)만 간략히 언급하고, 상세 헤더 설정은 backend.md로 참조 링크를 걸어 중복을 줄인다.

### m-2. backend.md와의 중복 -- 아키텍처 다이어그램의 서비스 컴포넌트

**문제**: infrastructure.md 섹션 4의 아키텍처 다이어그램이 `DocumentService`, `R2StorageService`, `DocumentCleanupScheduler` 같은 백엔드 내부 컴포넌트까지 포함한다. 이는 backend.md 섹션 10(패키지 구조)의 영역과 겹친다.

**제안**: infrastructure.md의 다이어그램은 인프라 배포 단위(Vercel, Railway, Supabase, R2)와 서비스 간 통신 흐름에 집중하고, 백엔드 내부 컴포넌트 상세는 backend.md에 위임한다.

### m-3. 스케일 비용 추산에 사용자 수-트래픽 가정 누락

**문제**: 섹션 8의 스케일 시나리오가 "사용자 1,000명"이라는 가정만 있고, 예상 문서 생성 수, API 호출 수, R2 저장량 등 구체적인 트래픽 가정이 없다. MVP 시나리오("일 100개 문서")에 비해 스케일 시나리오의 근거가 약하다.

**제안**: 사용자 1,000명 시 예상되는 일일 문서 생성 수, 월간 API 호출 수, 총 저장량 등 주요 수치를 가정에 포함하여 비용 추산의 설득력을 높인다.

### m-4. 주의사항 섹션이 빈약

**문제**: 섹션 11(주의사항)에 Spring Boot 콜드 스타트와 API 버전 2개 항목만 있다. 인프라 운영 관점에서 중요한 주의사항(예: Supabase Free 티어 제한, Railway 배포 실패 시 롤백, Vercel 빌드 타임아웃)이 누락되어 있다.

**제안**: 인프라 운영에 특화된 주의사항을 추가:
- Supabase Free: 500MB/50K행 한도 근접 시 알림 설정 방법
- Railway: 배포 실패 시 이전 버전으로 자동 롤백되는지 여부
- Vercel: 빌드 캐시 설정, 환경변수 관리 주의점

---

## 추가 고려사항

1. **SPRING_PROFILES_ACTIVE 환경변수**: Railway에서 `application-prod.yml`을 활성화하기 위한 `SPRING_PROFILES_ACTIVE=prod` 환경변수가 섹션 7에 없다. backend.md에서 `application-prod.yml`을 별도로 관리한다고 했으므로 프로필 활성화 방법을 명시해야 한다.

2. **Cloudflare Tunnel 상시 운영 여부**: 섹션 2에서 Cloudflare Tunnel을 개발 환경 외부 접근용으로 설명하는데, 프로덕션에서도 Cloudflare를 DNS/CDN으로 사용한다면 Tunnel이 아닌 일반 DNS 설정을 사용할 것이다. 개발 환경 전용이라는 점을 더 명확히 할 수 있다.

3. **모니터링 도구 상세 설정 누락**: 섹션 1에서 Sentry + Vercel Analytics를 모니터링으로 선택했으나, 구체적인 설정 가이드(알림 규칙, 대시보드 구성 등)는 없다. MVP 단계에서는 과할 수 있으나, 운영 시작 전에는 필요할 것이다.

4. **비밀 관리 전략**: 환경변수를 Railway/Vercel UI에서 관리한다고 암시하지만, 비밀 관리 전략(누가 접근 가능한지, 로테이션 주기, 비밀번호 유출 시 대응 등)이 명시되지 않았다. MVP에서는 과하지만 확장 시 필요하다.

---

## 검토 기준별 판정

| 기준 | 판정 | 비고 |
|------|------|------|
| 인프라/배포 완성도 | 양호 | Docker Compose 실행 가이드만 보강하면 구현에 충분 |
| 중복 | 경미 | 캐싱 전략, 아키텍처 다이어그램 2건 -- 정보 불일치는 아니므로 우선순위 낮음 |
| 논리적 일관성 | 환경변수 불일치 있음 | M-1(DATABASE_USER/PASSWORD), M-2(SENTRY_DSN), M-5(OAuth2 변수) -- 배포 실패 원인이 될 수 있어 반드시 수정 필요 |
| 누락 | 경미 | Docker Compose 실행 절차, 스케일 트래픽 가정 |
| 불필요한 내용 | 없음 | 모든 섹션이 인프라 문서로서 적절한 범위 내에 있음 |
