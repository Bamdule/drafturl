# 개발서버 구축 계획

> 작성일: 2026-03-26
> 상태: 계획 중

---

## 1. 개요

현재 Mac 로컬 환경에서 Cloudflare Tunnel로 서비스 중인 drafturl.com을 전용 개발서버로 이전한다.
사용자 피드백을 수집/반영한 뒤, 운영 환경은 클라우드로 이전할 예정.

### 현재 환경

| 항목 | 현재 |
|------|------|
| 하드웨어 | MacBook (개인 개발 겸용) |
| 서비스 구성 | Docker Compose (6 컨테이너) |
| 외부 노출 | Cloudflare Tunnel → drafturl.com |
| DB | PostgreSQL 15 (Docker) |
| 파일 저장소 | MinIO (Docker, S3 호환) |
| 문제점 | Mac 절전/재부팅 시 서비스 중단, 개발 작업과 서비스가 같은 머신 |

### 목표 환경

| 항목 | 목표 |
|------|------|
| 하드웨어 | Galaxy Book 3 Pro (Ubuntu 24.04) |
| 역할 | 전용 개발서버 (24시간 가동) |
| 서비스 구성 | Docker Compose (동일 구조) |
| 외부 노출 | Cloudflare Tunnel (기존 터널 재활용 또는 신규) |

---

## 2. 하드웨어 및 OS 설정

### Galaxy Book 3 Pro 스펙 확인 필요

- [ ] RAM 용량 (Docker 6 컨테이너 기준 최소 8GB 권장)
- [ ] SSD 잔여 공간 (Docker 이미지 + DB + MinIO 데이터 고려, 최소 50GB 여유)
- [ ] WiFi 칩셋 확인 (Intel AX201 계열이면 Ubuntu 24.04 기본 지원)

### Ubuntu 24.04 LTS 설치

```
주의사항:
- WiFi 드라이버: Intel AX201은 24.04에서 기본 지원. 만약 인식 안 되면 `sudo apt install linux-firmware` 후 재부팅
- 전원 관리: 노트북 덮개 닫아도 절전 안 되게 설정 필요
- SSH 서버: 원격 관리를 위해 설치
```

### 서버 모드 필수 설정

```bash
# 덮개 닫아도 절전 안 함
sudo sed -i 's/#HandleLidSwitch=suspend/HandleLidSwitch=ignore/' /etc/systemd/logind.conf
sudo sed -i 's/#HandleLidSwitchDocked=ignore/HandleLidSwitchDocked=ignore/' /etc/systemd/logind.conf
sudo systemctl restart systemd-logind

# SSH 서버 설치
sudo apt install openssh-server
sudo systemctl enable ssh

# 자동 업데이트 (보안 패치만)
sudo apt install unattended-upgrades
sudo dpkg-reconfigure unattended-upgrades
```

---

## 3. 소프트웨어 설치

### 필수 소프트웨어

| 소프트웨어 | 용도 | 설치 방법 |
|-----------|------|----------|
| Docker + Compose | 컨테이너 런타임 | `apt install docker.io docker-compose-v2` |
| Cloudflare Tunnel (cloudflared) | 외부 노출 | Cloudflare 공식 deb 패키지 |
| Git | 소스 관리 | `apt install git` |
| Claude Code | AI 개발 도구 | `npm install -g @anthropic-ai/claude-code` |
| Node.js 20 | Claude Code 실행 | NodeSource PPA 또는 nvm |

### Docker 설치

```bash
# Docker
sudo apt install docker.io docker-compose-v2
sudo usermod -aG docker $USER
sudo systemctl enable docker

# Docker 자동 시작 + 컨테이너 자동 재시작
# docker-compose.yml에 restart: unless-stopped 추가
```

### Cloudflare Tunnel 설치

```bash
# cloudflared 설치
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb -o cloudflared.deb
sudo dpkg -i cloudflared.deb

# 기존 터널 인증 (Mac에서 cert.pem 복사 또는 재로그인)
cloudflared tunnel login

# 시스템 서비스로 등록 (부팅 시 자동 시작)
sudo cloudflared service install
sudo systemctl enable cloudflared
```

---

## 4. 개발 환경 구축 (Mac 미러링)

### 소스 코드 및 설정 이전

```bash
# 1. Git clone
git clone <repo-url> ~/drafturl

# 2. 환경 변수 파일 복사 (Mac에서)
scp .env.* user@galaxy-book:~/drafturl/

# 3. Cloudflare Tunnel 설정 복사 또는 재구성
scp ~/.cloudflared/config.yml user@galaxy-book:~/.cloudflared/
```

### Docker Compose 구조 (현재와 동일)

```
docker-compose.yml          ← 인프라 (DB + MinIO), 항상 가동
docker-compose.local.yml    ← 로컬 개발용 (필요 시)
docker-compose.dev.yml      ← 개발서버 (drafturl.com 서비스)
```

### 부팅 시 자동 시작 설정

```bash
# /etc/systemd/system/drafturl.service
[Unit]
Description=DraftURL Dev Server
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/user/drafturl
ExecStart=/usr/bin/docker compose -f docker-compose.yml up -d
ExecStart=/usr/bin/docker compose -f docker-compose.dev.yml up -d
ExecStop=/usr/bin/docker compose -f docker-compose.dev.yml down
ExecStop=/usr/bin/docker compose -f docker-compose.yml down

[Install]
WantedBy=multi-user.target
```

---

## 5. 파일 저장소 전략

### 선택지 비교

| 항목 | MinIO (로컬 Docker) | Cloudflare R2 | AWS S3 |
|------|---------------------|---------------|--------|
| 비용 | 무료 | 무료 티어 10GB/월 | 유료 (소량이면 거의 무료) |
| 지연 시간 | 최소 (같은 서버) | 낮음 (Cloudflare 엣지) | 보통 |
| 안정성 | 서버 의존 (디스크 장애 = 데이터 손실) | 높음 (분산 저장) | 매우 높음 |
| 백업 | 수동 구성 필요 | 자동 | 자동 |
| 운영 이전 | 마이그레이션 필요 | 그대로 사용 가능 | 그대로 사용 가능 |
| 설정 난이도 | 낮음 (현재와 동일) | 중간 | 중간 |

### 권장: 단계적 전환

```
[현재] MinIO (로컬)
  ↓ 개발서버 이전 시
[단기] MinIO (Galaxy Book) ← 현재와 동일, 빠르게 이전 가능
  ↓ 운영 전환 시
[장기] Cloudflare R2 ← drafturl.com이 이미 Cloudflare 위에 있으므로 자연스러운 선택
```

**근거:**
- 개발 단계에서는 로컬 MinIO가 가장 간단하고 비용 없음
- 운영 전환 시 R2로 이전하면 Cloudflare 생태계 안에서 일관성 유지
- MinIO는 S3 호환이므로 코드 변경 최소화 (엔드포인트 URL만 변경)
- R2 무료 티어: 10GB 저장 + 1천만 읽기/월 → DraftURL 초기 운영에 충분

### MinIO → R2 마이그레이션 상세

MinIO와 R2 모두 **S3 호환 API**이므로 전환 난이도가 매우 낮다.

#### 코드 변경 범위

백엔드 `R2Config`에서 **환경 변수 3개만 변경**:

| 항목 | MinIO (현재) | Cloudflare R2 |
|------|-------------|---------------|
| endpoint | `http://localhost:9000` | `https://<account-id>.r2.cloudflarestorage.com` |
| accessKey | `minioadmin` | R2 Access Key |
| secretKey | `minioadmin` | R2 Secret Key |

애플리케이션 코드(S3Client 호출부)는 **변경 없음**.

#### 데이터 마이그레이션

```bash
# rclone 설치
sudo apt install rclone

# rclone 설정 (minio + r2 리모트 추가)
rclone config

# 한 줄로 전체 데이터 복사
rclone sync minio:drafturl-documents r2:drafturl-documents
```

#### 예상 소요 시간

| 작업 | 소요 시간 | 비고 |
|------|----------|------|
| R2 버킷 생성 + API 키 발급 | 5분 | Cloudflare 대시보드 |
| 환경 변수 변경 | 5분 | endpoint, key 3개 |
| rclone 데이터 복사 | 5~10분 | 문서 수백~수천 개 기준 |
| 업로드/다운로드 테스트 | 10분 | |
| **합계** | **~30분** | 난이도: 낮음 |

#### 결론

개발 단계에서 로컬 MinIO를 쓰다가 운영 시 R2로 전환해도 전혀 부담 없는 수준.
오히려 개발 중에는 로컬 MinIO가 네트워크 지연 없이 더 빠르고 디버깅도 편하다.

---

## 6. 배포 전략

### 개발서버 배포 방식 선택지

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A. SSH + 수동 배포** | Mac에서 SSH로 접속하여 git pull + docker compose up | 단순함 | 매번 수동 작업 |
| **B. Git Hook 자동 배포** | push 시 서버에서 자동 빌드/배포 | 자동화 | 초기 설정 필요 |
| **C. GitHub Actions + SSH** | GitHub push → Actions → SSH deploy | CI/CD 통합 | GitHub 의존 |
| **D. Watchtower** | Docker 이미지 변경 감지 자동 배포 | 완전 자동 | 이미지 레지스트리 필요 |

### 권장: B안 (Git Hook 자동 배포)

개발 단계에서는 간단하면서도 자동화된 방식이 적합.

```bash
# Galaxy Book의 bare repo에 post-receive hook 설정
# ~/drafturl.git/hooks/post-receive

#!/bin/bash
TARGET="/home/user/drafturl"
GIT_DIR="/home/user/drafturl.git"

git --work-tree=$TARGET --git-dir=$GIT_DIR checkout -f

cd $TARGET
docker compose -f docker-compose.dev.yml up -d --build

echo ">>> 배포 완료: $(date)"
```

```bash
# Mac에서 리모트 추가
git remote add dev user@galaxy-book:~/drafturl.git

# 배포 = push
git push dev main
```

### 원격 자동 배포 (GitHub Actions 연동)

Galaxy Book은 가정용 WiFi(NAT) 뒤에 있어 외부에서 직접 SSH 접근이 불가능하다.
이를 해결하는 방법은 두 가지:

#### 방식 A: Cloudflare Tunnel로 SSH 노출

```yaml
# Galaxy Book의 ~/.cloudflared/config.yml에 SSH 추가
ingress:
  - hostname: drafturl.com
    service: http://localhost:3000
  - hostname: api.drafturl.com
    service: http://localhost:8080
  - hostname: ssh.drafturl.com          # SSH 터널 추가
    service: ssh://localhost:22
  - service: http_status:404
```

GitHub Actions에서 `cloudflared access`로 SSH 접속하여 배포.
단점: SSH 포트가 외부에 노출됨.

#### 방식 B: Self-hosted Runner (권장)

Galaxy Book에 GitHub Actions Runner를 설치하면, GitHub push 시 Galaxy Book이 직접 빌드/배포를 수행한다.
SSH 연결 자체가 필요 없음.

```
[Git Push] → [GitHub Actions 트리거] → [Galaxy Book Runner가 감지]
                                            ↓
                                    git pull + docker compose up --build
```

**장점:**
- NAT/방화벽 문제 없음 (Runner가 GitHub에 아웃바운드 연결)
- SSH 포트 노출 불필요 (보안적으로 우수)
- 설치 간단

**설치 방법:**

```bash
# 1. GitHub repo Settings → Actions → Runners → New self-hosted runner

# 2. Galaxy Book에서 실행
mkdir actions-runner && cd actions-runner
curl -o actions-runner-linux-x64.tar.gz -L https://github.com/actions/runner/releases/latest/download/actions-runner-linux-x64-2.321.0.tar.gz
tar xzf actions-runner-linux-x64.tar.gz
./config.sh --url https://github.com/Bamdule/drafturl --token <RUNNER_TOKEN>

# 3. 시스템 서비스로 등록 (부팅 시 자동 시작)
sudo ./svc.sh install
sudo ./svc.sh start
```

**GitHub Actions 워크플로우 예시:**

```yaml
# .github/workflows/deploy-dev.yml
name: Deploy to Dev Server

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: self-hosted    # Galaxy Book Runner에서 실행
    steps:
      - uses: actions/checkout@v4

      - name: Deploy
        run: |
          docker compose -f docker-compose.yml up -d
          docker compose -f docker-compose.dev.yml up -d --build
          echo "배포 완료: $(date)"
```

**권장: B안 (Self-hosted Runner)**
- 보안: SSH 노출 없음
- 간편: push만 하면 자동 배포
- 안정: Runner가 로컬에서 직접 실행하므로 네트워크 지연 없음

### 장기 (운영 전환 시)

```
GitHub Actions → Docker 이미지 빌드 → Container Registry → 클라우드 서버 배포
```

---

## 7. 네트워크 구성

### Cloudflare Tunnel 설정

```
[사용자] → [Cloudflare Edge] → [Cloudflare Tunnel] → [Galaxy Book]
                                                        ├── frontend:3000
                                                        └── backend:8080
```

**선택지:**
- **A. 기존 터널 이전**: Mac의 터널 설정을 Galaxy Book으로 이동 (cert.pem 복사)
- **B. 신규 터널 생성**: Galaxy Book에서 새 터널 생성 후 DNS 연결 변경

**권장: A안** — 기존 터널 ID/DNS 설정 유지, cert.pem과 config.yml만 복사하면 끝.

### 주의: 가정용 WiFi 한계

| 항목 | 고려 |
|------|------|
| IP 변경 | Cloudflare Tunnel이므로 문제 없음 (아웃바운드 연결) |
| WiFi 불안정 | cloudflared 자동 재연결 지원, systemd restart 설정 |
| 대역폭 | 개발서버 트래픽은 소량이므로 WiFi 충분 |
| 정전/재부팅 | systemd 서비스로 자동 복구 |

---

## 8. 마이그레이션 체크리스트

### Phase 1: Ubuntu 설치 및 기본 설정
- [ ] Ubuntu 24.04 LTS 설치
- [ ] WiFi 연결 확인
- [ ] SSH 서버 설치 + 키 기반 인증
- [ ] 덮개 닫아도 절전 안 함 설정
- [ ] 고정 로컬 IP 설정 (공유기에서 DHCP 예약)

### Phase 2: 소프트웨어 설치
- [ ] Docker + Docker Compose 설치
- [ ] Node.js 20 설치
- [ ] Claude Code 설치
- [ ] Git 설치 + SSH 키 생성 + GitHub 등록
- [ ] cloudflared 설치

### Phase 3: 서비스 이전
- [ ] 소스 코드 clone
- [ ] 환경 변수 파일 복사
- [ ] Docker Compose로 6개 컨테이너 가동
- [ ] Cloudflare Tunnel 설정 이전
- [ ] drafturl.com 접속 테스트

### Phase 4: 자동화 및 안정화
- [ ] systemd 서비스 등록 (Docker + cloudflared)
- [ ] Git Hook 배포 설정
- [ ] Mac에서 기존 dev 서비스 중단
- [ ] 24시간 안정성 모니터링

---

## 9. 운영서버 구축 전략

### 컴포넌트별 최적 선택

DraftURL 스택(Spring Boot + Next.js + PostgreSQL + S3 호환 스토리지) 기준으로,
초기 트래픽이 적은 단계에서 비용 대비 성능이 좋은 조합을 비교한다.

---

### 9-1. 애플리케이션 서버 (Backend + Frontend)

| 서비스 | 월 비용 | 특징 | 적합도 |
|--------|--------|------|--------|
| **Fly.io** | $0~5 (무료 3 shared VM) | Docker 배포, 자동 SSL, 서울 리전 없음(도쿄 있음) | ★★★★ |
| **Railway** | $5~ (사용량 과금) | Git push 배포, 간편함, 아시아 리전 제한적 | ★★★ |
| **Render** | $0~7 (무료 tier 있음) | 무료 인스턴스 15분 비활성 시 슬립, 유료 시 해제 | ★★★ |
| **DigitalOcean Droplet** | $6 (1vCPU/1GB) | VPS, 싱가포르 리전, Docker 직접 운영 | ★★★★ |
| **Hetzner VPS** | €4.5 (~$5, 2vCPU/4GB) | 가성비 최강, 독일/핀란드 리전 (아시아 없음) | ★★★ |
| **Oracle Cloud Free** | $0 (상시 무료) | ARM 4코어/24GB 무료, 서울 리전 | ★★★★★ |
| **AWS Lightsail** | $5 (1vCPU/1GB) | AWS 생태계, 서울 리전, 예측 가능한 비용 | ★★★ |
| **Cloudflare Pages** | $0 (프론트만) | Next.js SSR 지원, 무료, 글로벌 엣지 | 프론트 전용 |

#### 권장: 프론트/백엔드 분리 배포

```
프론트엔드: Cloudflare Pages (무료)
  - Next.js 빌드 → 글로벌 엣지 배포
  - drafturl.com 도메인 그대로 연결
  - 이미 Cloudflare DNS를 쓰고 있으므로 자연스러운 선택

백엔드: Fly.io 또는 Oracle Cloud Free Tier
  - Docker 이미지 배포
  - api.drafturl.com 연결
```

**왜 분리?**
- 프론트엔드는 정적 + SSR → CDN 엣지가 가장 빠르고 무료
- 백엔드는 JVM 상시 가동 필요 → 컨테이너/VM이 적합
- 분리하면 프론트 배포가 백엔드에 영향 없음 (독립 배포)

#### 비용 최적화 조합

| 조합 | 월 비용 | 장점 | 단점 |
|------|--------|------|------|
| **A. 올 무료** | $0 | Oracle Free(백) + CF Pages(프론트) | Oracle 계정 제한, ARM 빌드 필요 |
| **B. 최소 유료** | ~$5 | Fly.io(백) + CF Pages(프론트) | Fly.io 도쿄 리전 (한국에서 ~30ms) |
| **C. 안정 유료** | ~$12 | DO Droplet(백+프론트) + 관리형 DB | 직접 운영 부담 |

---

### 9-2. 데이터베이스 (PostgreSQL)

| 서비스 | 무료 티어 | 유료 시작 | 특징 | 적합도 |
|--------|----------|----------|------|--------|
| **Neon** | 0.5GB, 무제한 프로젝트 | $19/월 | 서버리스 PG, 자동 스케일, 브랜칭 | ★★★★★ |
| **Supabase** | 500MB, 2 프로젝트 | $25/월 | PG + Auth + Realtime 포함 | ★★★★ |
| **Railway** | $5 크레딧 내 | 사용량 과금 | 앱과 같은 플랫폼에서 관리 | ★★★ |
| **Fly.io Postgres** | 무료 VM 포함 | $0~ | Fly 앱과 같은 네트워크 | ★★★ |
| **Aiven** | 무료 (1 PG) | $19/월 | 관리형 PG, 멀티 클라우드 | ★★★ |
| **AWS RDS** | 없음 | $15~/월 | 안정적이나 비쌈 | ★★ |

#### 권장: Neon (서버리스 PostgreSQL)

```
장점:
- 무료 티어 0.5GB → DraftURL 초기에 충분 (메타데이터만 저장, 파일은 R2)
- 서버리스: 비활성 시 자동 스케일다운 → 비용 절약
- 브랜칭: dev/staging DB를 브랜치로 분리 가능
- 서울에서 가장 가까운 리전: 도쿄 (ap-southeast-1)

주의:
- Cold start 지연 (~1초): 비활성 후 첫 요청 시 발생
- 0.5GB 초과 시 유료 전환 필요
```

**DraftURL에 특히 적합한 이유:**
- 문서 본문은 R2에 저장 → DB에는 메타데이터(제목, slug, 사용자 정보)만 저장
- 0.5GB면 수만 건의 메타데이터 저장 가능
- Flyway 마이그레이션 그대로 사용 가능 (표준 PostgreSQL)

---

### 9-3. 파일 저장소

| 서비스 | 무료 티어 | 특징 | 적합도 |
|--------|----------|------|--------|
| **Cloudflare R2** | 10GB + 1천만 읽기/월 | **이그레스 무료**, S3 호환, CF 생태계 | ★★★★★ |
| **AWS S3** | 5GB (12개월) | 업계 표준, 이그레스 비용 있음 | ★★★ |
| **Backblaze B2** | 10GB | 저렴, CF와 대역폭 제휴 무료 | ★★★★ |

#### 권장: Cloudflare R2

이미 5장에서 상세 분석 완료. **이그레스 비용 $0**이 결정적 장점.
문서 공유 서비스 특성상 읽기(다운로드)가 압도적으로 많으므로, 이그레스 무료는 비용 예측을 매우 쉽게 만든다.

---

### 9-4. CDN / 엣지

**Cloudflare (현재 사용 중)** — 변경 불필요.

- 무료 플랜으로 충분
- DNS, SSL, DDoS 방어, 캐싱 모두 포함
- Pages + R2 + Tunnel 모두 같은 생태계

---

### 9-5. CI/CD

| 방식 | 비용 | 특징 |
|------|------|------|
| **GitHub Actions** | 무료 (2,000분/월) | 코드 push → 자동 빌드 → 배포 |
| **Fly.io CLI** | 무료 | `fly deploy` 한 줄 |
| **CF Pages 자동 배포** | 무료 | Git 연결 시 push → 자동 빌드 |

#### 권장 파이프라인

```
[Git Push]
    ├── Frontend: GitHub → Cloudflare Pages (자동 빌드/배포)
    └── Backend: GitHub Actions → Docker build → Fly.io deploy
```

---

### 9-6. 권장 최종 구성

```
┌─────────────────────────────────────────────────────┐
│                    Cloudflare                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │   DNS    │  │   CDN    │  │  Pages (Frontend) │  │
│  │  + SSL   │  │  캐싱    │  │  Next.js SSR      │  │
│  └──────────┘  └──────────┘  └──────────────────┘  │
│                                       │             │
│  ┌──────────────────┐                 │             │
│  │   R2 (파일저장)   │                 │             │
│  │   문서 본문 저장   │                 │             │
│  └──────────────────┘                 │             │
└───────────────────────────────────────┼─────────────┘
                                        │ API 호출
                                        ▼
                              ┌──────────────────┐
                              │   Fly.io (백엔드)  │
                              │   Spring Boot     │
                              │   api.drafturl.com│
                              └────────┬─────────┘
                                       │
                                       ▼
                              ┌──────────────────┐
                              │   Neon (DB)       │
                              │   PostgreSQL      │
                              │   메타데이터 저장   │
                              └──────────────────┘
```

### 월간 예상 비용

| 컴포넌트 | 서비스 | 월 비용 |
|----------|--------|--------|
| 프론트엔드 | Cloudflare Pages | $0 |
| 백엔드 | Fly.io (shared-cpu-1x, 256MB) | $0~3 |
| 데이터베이스 | Neon Free Tier | $0 |
| 파일 저장소 | Cloudflare R2 Free Tier | $0 |
| CDN + DNS | Cloudflare Free | $0 |
| 도메인 | Cloudflare Registrar | ~$10/년 |
| **합계** | | **$0~3/월** |

> 트래픽이 늘어 무료 티어를 초과하더라도, 가장 먼저 부딪히는 건 Fly.io 메모리.
> Spring Boot(JVM)는 최소 512MB 권장 → 유료 시 ~$5~7/월.
> 전체 **$10/월 이하**로 상당 규모까지 운영 가능.

---

## 10. 미결 사항

| # | 항목 | 상태 |
|---|------|------|
| 1 | Galaxy Book 3 Pro 정확한 스펙 (RAM, SSD, WiFi 칩셋) | 확인 필요 |
| 2 | 데이터 백업 전략 (DB 덤프 주기, MinIO 데이터) | 결정 필요 |
| 3 | 모니터링/알림 (서비스 다운 시 알림) | 결정 필요 |
| 4 | 도메인 구조 (dev.drafturl.com 분리 여부) | 결정 필요 |
