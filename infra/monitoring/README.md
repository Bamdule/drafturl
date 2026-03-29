# DraftURL 모니터링

Uptime Kuma (서비스 상태) + Umami (사용자 분석)

---

## 1단계: 컨테이너 실행

Galaxy Book에서 실행:

```bash
cd ~/my-ai-workspaces/drafturl/infra/monitoring
docker compose up -d
```

실행 확인:

```bash
docker compose ps
```

3개 컨테이너가 모두 `running` 상태인지 확인:
- `drafturl-uptime-kuma`
- `drafturl-umami`
- `drafturl-umami-db`

---

## 2단계: Cloudflare Tunnel 설정 (외부 접근)

Galaxy Book의 Cloudflare Tunnel 설정 파일(`~/.cloudflared/config.yml`)에 아래 내용을 추가한다:

```yaml
# 기존 ingress 항목들 아래에 추가
- hostname: monitor.drafturl.com
  service: http://localhost:3002
- hostname: analytics.drafturl.com
  service: http://localhost:3003
```

Cloudflare DNS에 레코드 추가:

| 타입 | 이름 | 내용 | 프록시 |
|------|------|------|--------|
| CNAME | monitor | `<터널-ID>.cfargotunnel.com` | O |
| CNAME | analytics | `<터널-ID>.cfargotunnel.com` | O |

터널 재시작:

```bash
sudo systemctl restart cloudflared
```

---

## 3단계: Uptime Kuma 초기 설정

**접속**: https://monitor.drafturl.com (또는 http://localhost:3002)

### 3-1. 관리자 계정 생성

최초 접속 시 관리자 아이디/비밀번호를 직접 설정한다.

### 3-2. 모니터 추가

"Add New Monitor" 클릭 후 아래 항목을 등록:

| 이름 | 타입 | URL | Heartbeat Interval |
|------|------|-----|-----|
| Frontend | HTTP(s) | https://drafturl.com | 60초 |
| Backend API | HTTP(s) | https://api.drafturl.com/api/v1/health | 60초 |

### 3-3. 알림 설정

Settings → Notifications에서 알림 채널을 추가한다:

- **Discord**: Webhook URL 입력
- **Telegram**: Bot Token + Chat ID 입력
- **Email**: SMTP 설정

서비스 다운 시 등록된 채널로 즉시 알림이 발송된다.

### 3-4. 상태 페이지 (선택)

Status Pages → "New Status Page" → 위 모니터들을 추가하면 외부 공개용 상태 페이지가 생성된다.

---

## 4단계: Umami 초기 설정

**접속**: https://analytics.drafturl.com (또는 http://localhost:3003)

### 4-1. 로그인 및 비밀번호 변경

- 기본 계정: `admin` / `umami`
- 로그인 후 **반드시 비밀번호를 변경**한다 (Settings → Profile)

### 4-2. 웹사이트 등록

Settings → Websites → "Add website":

- **Name**: DraftURL
- **Domain**: drafturl.com

### 4-3. 트래킹 코드 확인

웹사이트 추가 후 "Tracking code" 버튼을 클릭하면 아래와 같은 스크립트가 표시된다:

```html
<script defer src="https://analytics.drafturl.com/script.js" data-website-id="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"></script>
```

이 `data-website-id` 값을 프론트엔드에 적용해야 한다. (아래 5단계 참조)

---

## 5단계: 프론트엔드에 Umami 트래킹 적용

프론트엔드의 `src/app/layout.tsx`에 Umami 스크립트를 추가한다:

```tsx
<Script
  defer
  src="https://analytics.drafturl.com/script.js"
  data-website-id="Umami에서 발급받은 ID"
/>
```

이 작업은 Umami 설정 완료 후 별도로 진행한다.

---

## 관리 명령어

```bash
# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f uptime-kuma
docker compose logs -f umami

# 중지 (데이터는 볼륨에 보존됨)
docker compose down

# 업데이트
docker compose pull && docker compose up -d
```

---

## 포트 정리

| 서비스 | 내부 포트 | 외부 포트 | 외부 도메인 |
|--------|----------|----------|------------|
| Uptime Kuma | 3001 | 3002 | monitor.drafturl.com |
| Umami | 3000 | 3003 | analytics.drafturl.com |
| Umami DB | 5432 | - (내부 전용) | - |
