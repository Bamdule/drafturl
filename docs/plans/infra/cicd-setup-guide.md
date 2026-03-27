# CI/CD 설정 가이드 (개발서버)

> 작성일: 2026-03-27

GitHub Actions + Self-hosted Runner로 main push 시 갤럭시 북에 자동 배포.

---

## 1. GitHub Environment + Secrets 설정 (완료)

환경별로 Secrets를 분리 관리한다. repo 레벨 Secret은 사용하지 않는다.

### 현재 상태

| 환경 | 상태 | 용도 |
|------|------|------|
| `dev` | **등록 완료** | 갤럭시 북 개발서버 |
| `production` | 미생성 | 운영 서버 (나중에 추가) |

### dev 환경 Secrets (등록 완료)

| Secret 이름 | 비고 |
|---|---|
| `R2_ENDPOINT` | Cloudflare R2 엔드포인트 |
| `R2_ACCESS_KEY` | R2 Access Key |
| `R2_SECRET_KEY` | R2 Secret Key |
| `R2_BUCKET` | R2 버킷 이름 |

### production 환경 추가 시

```bash
gh api repos/Bamdule/drafturl/environments/production -X PUT
gh secret set R2_ENDPOINT --env production --body "<운영 R2 엔드포인트>"
gh secret set R2_ACCESS_KEY --env production --body "<운영 R2 Access Key>"
# ...
```

워크플로우에서 `environment: production`으로 참조하면 해당 환경의 Secrets를 사용한다.

## 3. Self-hosted Runner 설치 (갤럭시 북)

### 3-1. GitHub에서 토큰 발급

GitHub repo → Settings → Actions → Runners → New self-hosted runner → Linux x64

표시되는 토큰을 복사.

### 3-2. 갤럭시 북에서 실행

```bash
# Runner 디렉토리 생성
mkdir ~/actions-runner && cd ~/actions-runner

# 다운로드 (GitHub에서 표시하는 최신 버전 URL 사용)
curl -o actions-runner-linux-x64.tar.gz -L \
  https://github.com/actions/runner/releases/latest/download/actions-runner-linux-x64-2.321.0.tar.gz
tar xzf actions-runner-linux-x64.tar.gz

# 설정 (GitHub에서 복사한 토큰 사용)
./config.sh --url https://github.com/Bamdule/drafturl --token <RUNNER_TOKEN>

# 시스템 서비스로 등록 (부팅 시 자동 시작)
sudo ./svc.sh install
sudo ./svc.sh start
```

### 3-3. 확인

```bash
# Runner 상태 확인
sudo ./svc.sh status

# GitHub에서 확인
# repo → Settings → Actions → Runners → "Idle" 상태면 정상
```

## 4. 필수 의존성 확인 (갤럭시 북)

Runner가 실행하는 스크립트에 필요한 도구:

```bash
# Docker
docker --version
docker compose version

# Java 21 (setup-java Action이 설치하지만, 캐시 위해 미리 설치 권장)
java -version

# Git
git --version
```

## 5. 테스트

```bash
# Mac에서 push
git push origin main

# GitHub → Actions 탭에서 "Deploy to Dev Server" 워크플로우 확인
# 갤럭시 북에서 컨테이너 상태 확인
docker ps
```

## 6. 배포 흐름

```
git push main
    ↓
GitHub Actions 트리거
    ↓
갤럭시 북 Self-hosted Runner 감지
    ↓
1. checkout
2. ./gradlew build -x test (백엔드 jar 빌드)
3. .env 파일 생성 (GitHub Secrets)
4. docker compose up -d --build
5. 헬스체크 (GET /api/v1/health)
6. .env 파일 삭제
    ↓
배포 완료
```
