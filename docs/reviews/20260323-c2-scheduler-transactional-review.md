# C-2 스케줄러 메서드 @Transactional 부재 -- 수정 리뷰

**날짜:** 2026-03-23
**대상 파일:**
- `backend/src/main/java/com/drafturl/api/domain/document/service/DocumentCleanupService.java`
- `backend/src/main/java/com/drafturl/api/domain/document/service/DocumentTransactionService.java`

## 수정 요약

### 문제
`DocumentCleanupService`의 두 곳에서 `@Transactional` 없이 DB 쓰기를 직접 수행하고 있었다.

| 메서드 | 문제 | 위험도 |
|--------|------|--------|
| `cleanupOrphanDocuments()` | `documentRepository.save()` 직접 호출 | 낮음 (StorageUsage 변경 없음) |
| `purgeDeletedDocuments()` | `documentRepository.deleteAllInBatch()` 직접 호출 | 중간 (벌크 삭제의 원자성 미보장) |

### 수정 내용

1. **`DocumentTransactionService`에 2개 메서드 추가:**
   - `setOrphanExpiry(Document, LocalDateTime)` -- 고아 문서 개별 처리용 `@Transactional`
   - `purgeDocuments(List<Document>)` -- 벌크 물리 삭제용 `@Transactional`

2. **`DocumentCleanupService`에서 직접 repository 호출 제거:**
   - `cleanupOrphanDocuments()` -> `transactionService.setOrphanExpiry()` 호출
   - `purgeDeletedDocuments()` -> `transactionService.purgeDocuments()` 호출
   - 결과: `DocumentCleanupService`에서 `documentRepository.save()`/`delete()` 직접 호출이 0건

### 기존 코드 (이미 올바르게 구현된 부분)

아래 메서드들은 이미 `DocumentTransactionService`를 통해 트랜잭션이 적용되어 있었다:
- `processExpiredDocuments()` -> `transactionService.expireSingleDocument()`
- `cleanupPendingDocuments()` -> `transactionService.deletePendingSingleDocument()`

`expireSingleDocument()`에는 linter가 이중 처리 방지 로직(DB 재조회 + 상태 확인)도 추가하였다.

## 자체 리뷰 체크리스트

- [x] 개별 문서 단위 트랜잭션 경계 설정 (배치 전체 롤백 방지)
- [x] `@Transactional` 메서드가 별도 빈(`DocumentTransactionService`)에 위치 (self-invocation 문제 없음)
- [x] `DocumentCleanupService`에서 직접 save/delete 호출 없음
- [x] R2 삭제(네트워크 I/O)는 트랜잭션 밖에서 수행 (`processExpiredDocuments`의 fileStorage.delete)
- [x] 빌드 성공 (`./gradlew compileJava`)

## 잔여 이슈 (이번 스코프 외)

1. **`deletePendingSingleDocument` 내 R2 삭제가 트랜잭션 안에 있음:**
   네트워크 I/O인 `fileStorage.delete()`가 `@Transactional` 안에서 호출된다.
   R2 삭제가 느려지면 DB 커넥션을 오래 점유할 수 있다.
   `processExpiredDocuments()`처럼 R2 삭제를 트랜잭션 밖으로 분리하는 것이 바람직하다.

2. **`setOrphanExpiry`에서 detached entity 가능성:**
   `DocumentCleanupService.cleanupOrphanDocuments()`에서 조회한 Document를
   `transactionService.setOrphanExpiry()`에 전달할 때, 해당 엔티티는
   트랜잭션 바깥에서 조회되었으므로 detached 상태일 수 있다.
   `save()`가 merge를 수행하므로 동작은 하지만,
   `expireSingleDocument()`처럼 ID로 재조회하는 패턴이 더 안전하다.
