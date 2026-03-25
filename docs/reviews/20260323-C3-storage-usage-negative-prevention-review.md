# C-3 StorageUsage 음수 방지 - 자체 리뷰

**작성일**: 2026-03-23
**대상 파일**:
- `backend/src/main/java/com/drafturl/api/domain/document/service/DocumentTransactionService.java`
- `backend/src/main/java/com/drafturl/api/domain/storage/repository/StorageUsageRepository.java`
- `backend/src/main/resources/db/migration/V2__add_storage_usage_check_constraints.sql`

## 변경 요약

### 1. JPQL GREATEST() 적용 (기존 완료 확인)
`StorageUsageRepository.java`의 `decrementUsage()` 및 `incrementUsage()` 쿼리에 `GREATEST(..., 0)` 함수가 이미 적용되어 있었다. 추가 수정 불필요.

### 2. Flyway 마이그레이션 - CHECK 제약 조건 추가 (신규)
`V2__add_storage_usage_check_constraints.sql`을 추가하여 DB 레벨에서 `total_bytes >= 0`, `document_count >= 0` 제약 조건을 설정했다. GREATEST() 함수가 애플리케이션 레벨에서 방어하더라도, 직접 SQL이나 다른 경로로 데이터가 변경될 가능성에 대비한 이중 방어선이다.

### 3. 이중 처리 방지 guard 조건 추가 (신규)
- **`expireSingleDocument()`**: 기존에는 `DocumentCleanupService`에서 전달받은 detached Document 객체를 직접 사용했다. 스케줄러 실행 시점과 실제 처리 시점 사이에 사용자가 문서를 삭제할 수 있으므로, DB에서 최신 상태를 재조회하여 ACTIVE가 아닌 문서는 건너뛴다.
- **`softDeleteDocument()`**: 사용자 삭제 요청과 스케줄러 만료 처리가 동시 실행되는 경우를 대비하여, ACTIVE 상태가 아닌 문서는 StorageUsage 감소 없이 즉시 반환한다.

## 리뷰 체크리스트

| 항목 | 판정 | 비고 |
|------|------|------|
| JPQL에서 음수 방지 | OK | GREATEST() 이미 적용됨 |
| DDL CHECK 제약 조건 | OK | V2 마이그레이션 추가 |
| 이중 처리 방지 guard | OK | expireSingleDocument, softDeleteDocument 모두 적용 |
| 기존 상태 전이 로직과의 호환성 | OK | Document.markDeleted()와 expire()의 ACTIVE 상태 검증이 guard 이후에만 호출됨 |
| 로깅 | OK | guard 조건 진입 시 info 레벨 로그 추가 |
| 컴파일 | OK | `./gradlew compileJava` 성공 |

## 잠재적 개선 사항 (Phase 2)

- `expireSingleDocument()`에서 `SELECT ... FOR UPDATE` 비관적 락을 사용하면 완벽한 동시성 보장이 가능하나, 현재 GREATEST() + CHECK 제약 조건 + guard 조건의 3중 방어로 MVP에서는 충분하다.
- 테스트: 동시성 시나리오에 대한 통합 테스트를 추가하면 회귀 방지에 도움이 된다.
