# DraftURL MCP 서버 배포 계획

## 현재 상태
- DraftURL MCP 서버: Remote HTTP + OAuth2 방식 구현 완료
- 엔드포인트: `https://drafturl.com/mcp/transport`
- OAuth2 디스커버리: `https://drafturl.com/.well-known/oauth-authorization-server`
- MCP 도구 5개: create_document, get_document, update_document, delete_document, list_documents

## 사용자 등록 명령어
```bash
claude mcp add --transport http drafturl https://drafturl.com/mcp/transport
```
- 로컬 설치 불필요, OAuth2 인증 자동 진행
- Sentry MCP와 동일한 Remote HTTP 방식

## 배포 단계

### 1단계: 랜딩 페이지 안내
- DraftURL 웹사이트에 MCP 연동 가이드 섹션 추가
- `claude mcp add` 명령어 + 사용 예시 안내

### 2단계: MCP Registry 등록
- 공식 레지스트리: https://registry.modelcontextprotocol.io/
- `mcp-publisher` CLI 설치: `brew install mcp-publisher`
- 등록 절차:
  1. `mcp-publisher init` → `server.json` 메타데이터 생성
  2. `mcp-publisher login github` → GitHub OAuth 인증
  3. `mcp-publisher publish` → 레지스트리에 등록

### 3단계: 마켓플레이스 노출 (선택)
- mcp.so — 19,000+ 서버 등록된 서드파티 마켓플레이스
- Glama (glama.ai/mcp/servers) — 18,000+ 서버
- LobeHub (lobehub.com/mcp) — 커뮤니티 기반

## 참고: MCP 서버 배포 방식 비교

| 방식 | 예시 | 장점 | 단점 |
|------|------|------|------|
| Remote HTTP | Sentry, DraftURL | 설치 불필요, OAuth2 인증 | 서버 운영 필요 |
| npm/PyPI 패키지 | GitHub, Linear | 로컬 실행, 오프라인 가능 | 사용자가 직접 설치/설정 |
| MCP Registry | 메타데이터 등록 | 검색 노출, 발견성 향상 | 별도 등록 절차 필요 |

## 참고: Claude Code .mcp.json 설정
- 프로젝트 `.mcp.json`에 등록 시 `"type": "http"` 사용 (`"streamable-http"` 아님)
- 프로젝트 레벨 MCP는 `hasTrustDialogAccepted` 승인 필요
- 글로벌 등록(`claude mcp add`)은 trust 불필요
