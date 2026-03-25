# 인프라 QA

> 트리거: `docker-compose*`, `SecurityConfig`, `RateLimitFilter`, `cloudflared`

---

## CORS

### TC-INFRA-01: CORS 프리플라이트 (OPTIONS 200)

**사전 조건**: 백엔드 서버 실행 중
**테스트 단계**:
1. 터미널에서 OPTIONS 요청 전송:
   ```bash
   curl -v -X OPTIONS http://localhost:8080/api/v1/documents \
     -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type"
   ```
**기대 결과**:
- 응답 상태 200
- `Access-Control-Allow-Origin: http://localhost:3000` 헤더 존재
- `Access-Control-Allow-Methods`에 POST 포함
- `Access-Control-Allow-Headers`에 Content-Type 포함
**자동화**: 가능

---

## Rate Limiting

### TC-INFRA-02: Rate Limiting (비로그인 분당 10회 초과)

**사전 조건**: 백엔드 서버 실행 중, 로그아웃 상태
**테스트 단계**:
1. 터미널에서 문서 생성 API를 빠르게 11회 호출:
   ```bash
   for i in $(seq 1 11); do
     curl -s -o /dev/null -w "%{http_code}\n" \
       -X POST http://localhost:8080/api/v1/documents \
       -H "Content-Type: application/json" \
       -d '{"content":"<p>test</p>","type":"html"}'
   done
   ```
**기대 결과**: 처음 10회는 201 응답, 11회째부터 429 (Too Many Requests) 응답, `Retry-After` 헤더 포함
**자동화**: 가능

### TC-INFRA-03: Rate Limiting (로그인 사용자 분당 30회)

**사전 조건**: 백엔드 서버 실행 중, 로그인 상태 (Access Token 확보)
**테스트 단계**:
1. 인증 헤더와 함께 API를 빠르게 31회 호출
**기대 결과**: 처음 30회는 정상 응답, 31회째부터 429 응답
**자동화**: 가능

---

## 로컬 서버 접근

### TC-INFRA-04: 로컬 서버 접근

**사전 조건**: Docker 실행, 백엔드/프론트엔드 서버 실행
**테스트 단계**:
1. 프론트엔드 접근: `http://localhost:3000`
2. 백엔드 API 접근: `http://localhost:8080/api/v1/documents` (GET)
3. MinIO 콘솔: `http://localhost:9001`
**기대 결과**: 각 서비스가 정상 응답
**자동화**: 가능

---

## 개발 서버 접근

### TC-INFRA-05: 개발 서버 접근

**사전 조건**: Cloudflare Tunnel 설정 완료, 개발 서버 배포 완료
**테스트 단계**:
1. 프론트엔드 접근: `https://drafturl.com`
2. 백엔드 API 접근: `https://api.drafturl.com/api/v1/documents` (GET)
**기대 결과**: 각 서비스가 정상 응답, HTTPS 인증서 유효
**자동화**: 가능

---

## Cloudflare Tunnel

### TC-INFRA-06: Cloudflare Tunnel 연결 상태

**사전 조건**: cloudflared 데몬 실행 중
**테스트 단계**:
1. 터미널에서 확인:
   ```bash
   cloudflared tunnel info
   ```
2. 또는 Cloudflare Zero Trust 대시보드에서 터널 상태 확인
**기대 결과**: 터널 상태 "healthy" 또는 "active", 연결된 커넥터 1개 이상
**자동화**: 가능

---

## Docker 상태

### TC-INFRA-07: Docker 컨테이너 상태 확인

**사전 조건**: `docker compose up -d` 실행 완료
**테스트 단계**:
1. 터미널에서 확인:
   ```bash
   docker compose ps
   ```
**기대 결과**:
- `drafturl-db` (PostgreSQL): healthy
- `drafturl-storage` (MinIO): healthy
**자동화**: 가능

---

## 데이터베이스

### TC-INFRA-08: 데이터베이스 마이그레이션 확인

**사전 조건**: 백엔드 서버 최초 실행
**테스트 단계**:
1. 백엔드 서버 시작 로그에서 Flyway 마이그레이션 확인
2. 또는 DB에 직접 접속하여 테이블 확인:
   ```bash
   docker exec -it drafturl-db psql -U drafturl -d drafturl -c '\dt'
   ```
**기대 결과**: `users`, `documents`, `storage_usage`, `refresh_tokens` 테이블 존재, Flyway 버전 이력 테이블(`flyway_schema_history`) 존재
**자동화**: 가능
