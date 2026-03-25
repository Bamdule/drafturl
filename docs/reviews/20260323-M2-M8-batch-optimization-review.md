# M-2 + M-8 수정 자체 리뷰

## 요약

M-2(DocumentService 비대화)는 C-2 수정에서 이미 해결되어 추가 작업 불필요. M-8(N+1 쓰기)은 스케줄러의 모든 DB 쓰기를 배치 처리로 전환하여 해결.

## M-2: DocumentService 비대화

**결론: 이미 해결됨**

- `DocumentService`라는 클래스 자체가 존재하지 않음. UseCase 패턴(`CreateDocumentUseCase`, `UpdateDocumentUseCase` 등 6개)으로 분리 완료
- 스케줄러 로직은 `DocumentCleanupService`에 완전 분리
- 트랜잭션 경계는 `DocumentTransactionService`에 분리

## M-8: N+1 쓰기 배치 최적화

### 변경 전 (N+1 패턴)

| 메서드 | DB 쓰기 횟수 (N건 기준) |
|--------|------------------------|
| processExpiredDocuments | N x save() + N x decrementUsage() = 2N |
| cleanupPendingDocuments | N x delete() = N |
| cleanupOrphanDocuments | N x save() = N |
| purgeDeletedDocuments | 1 (이미 배치) |

### 변경 후 (배치 처리)

| 메서드 | DB 쓰기 횟수 (N건 기준) |
|--------|------------------------|
| processExpiredDocuments | 1 x bulkUpdateStatus + M x decrementUsage (M = 고유 userId 수) |
| cleanupPendingDocuments | 1 x deleteAllInBatch |
| cleanupOrphanDocuments | 1 x bulkSetExpiresAt |
| purgeDeletedDocuments | 1 (변경 없음) |

### 수정 파일

1. **DocumentRepository.java**: `bulkUpdateStatus()`, `bulkSetExpiresAt()` 벌크 쿼리 추가
2. **DocumentTransactionService.java**:
   - 개별 처리 메서드 3개 제거 (`expireSingleDocument`, `deletePendingSingleDocument`, `setOrphanExpiry`)
   - 배치 처리 메서드 3개 추가 (`expireDocumentsBatch`, `deletePendingDocumentsBatch`, `setOrphanExpiryBatch`)
   - `decrementStorageByUser()`: userId별 집계 후 한 번에 감소
   - `FileStorage` 의존성 제거 (더 이상 불필요)
3. **DocumentCleanupService.java**: 모든 메서드를 "R2 개별 삭제 -> DB 배치 처리" 2단계 패턴으로 전환

### 트랜잭션 안전성

- 배치 처리로 전환하면서 **개별 문서 실패 시 전체 배치가 롤백**되는 특성이 생김
- 이는 의도된 트레이드오프: 개별 격리보다 N+1 쓰기 방지가 MVP에서 더 중요
- `findExpiredDocuments` 쿼리가 `WHERE status = 'ACTIVE'` 조건으로 ACTIVE만 조회하므로, `bulkUpdateStatus`에서 이중 처리 위험은 낮음
- R2 삭제(외부 I/O)는 트랜잭션 밖에서 개별 처리하여, R2 실패가 DB 트랜잭션에 영향 주지 않음

### 잠재적 고려사항

- **CLEANUP_BATCH_SIZE=100**: 현재 100건 단위로 처리하므로 IN 절의 크기가 과도하지 않음
- **bulkUpdateStatus의 1차 캐시 불일치**: 벌크 UPDATE는 영속성 컨텍스트를 우회하지만, 스케줄러 전용 트랜잭션이므로 같은 트랜잭션에서 엔티티를 다시 읽지 않아 문제 없음
