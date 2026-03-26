# Changelog

이 문서는 DraftURL의 모든 주요 변경사항을 기록합니다.
형식은 [Keep a Changelog](https://keepachangelog.com)를 따릅니다.

---

## [v0.1.0] - 2026-03-27

MVP 최초 배포.

### Added
- HTML/Markdown 붙여넣기 즉시 배포 (nanoid 8자리 slug URL)
- Monaco Editor 기반 실시간 미리보기
- Markdown → HTML 자동 렌더링
- Google / GitHub OAuth2 소셜 로그인
- JWT 인증 (httpOnly 쿠키 BFF 프록시 패턴)
- 대시보드 문서 목록 조회, 수정, 삭제
- 비로그인 24시간 만료 / 로그인 영구 보관 정책
- Cloudflare R2 파일 저장소 (CDN 직접 서빙)
- Docker Compose 기반 개발서버 (Galaxy Book 3 Pro)
- Cloudflare Tunnel 외부 노출

### Fixed
- 로그인 성공 후 대시보드 리다이렉트 안 되는 문제 수정

### Changed
- MinIO → Cloudflare R2로 파일 저장소 전환
- 문서 조회를 CDN 직접 서빙 방식으로 변경
