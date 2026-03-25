package com.drafturl.api.domain.document.service;

import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.storage.service.StorageUsageService;
import com.drafturl.api.domain.document.DocType;
import com.drafturl.api.domain.document.DocumentStatus;
import com.drafturl.api.global.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 문서의 트랜잭션 경계를 담당하는 서비스.
 * Spring AOP 프록시 기반 @Transactional이 동일 클래스 내부 호출에서 동작하지 않는 문제를 해결하기 위해
 * UseCase와 분리한다.
 *
 * 스케줄러(DocumentCleanupService)에서 배치 처리 시 트랜잭션 경계를 제공하는 메서드도 포함한다.
 * 벌크 UPDATE 쿼리로 N+1 쓰기를 방지하며, StorageUsage는 userId별로 집계하여 한 번에 감소시킨다.
 */
@Service
public class DocumentTransactionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentTransactionService.class);

    private final DocumentRepository documentRepository;
    private final StorageUsageService storageUsageService;

    public DocumentTransactionService(DocumentRepository documentRepository,
                                       StorageUsageService storageUsageService) {
        this.documentRepository = documentRepository;
        this.storageUsageService = storageUsageService;
    }

    /**
     * 5단계: PENDING 상태로 문서 INSERT (트랜잭션 1).
     */
    @Transactional
    public Document insertPendingDocument(String id, String slug, UUID userId, String title,
                                          DocType docType, String r2Key, long contentSize,
                                          LocalDateTime expiresAt) {
        Document document = new Document(id, slug, userId, title, docType, r2Key,
                contentSize, DocumentStatus.PENDING, expiresAt);
        return documentRepository.save(document);
    }

    /**
     * 7단계: ACTIVE로 전이 + StorageUsage 업데이트 (트랜잭션 2).
     * 로그인 사용자인 경우만 StorageUsage를 업데이트한다.
     * 첫 문서 생성 시 StorageUsage 레코드가 없으면 생성한다.
     */
    @Transactional
    public Document activateDocument(String documentId, UUID userId, long contentSize) {
        Document managed = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "문서를 찾을 수 없습니다: " + documentId));

        managed.activate();
        Document saved = documentRepository.save(managed);

        // 로그인 사용자인 경우 StorageUsage 업데이트
        if (userId != null) {
            storageUsageService.incrementUsage(userId, contentSize, 1);
        }

        return saved;
    }

    /**
     * 문서 수정: DB UPDATE (documents + StorageUsage) -- 단일 트랜잭션.
     *
     * R2 덮어쓰기 성공 후 이 메서드가 호출된다.
     * "MVP에서는 허용 가능한 불일치로 간주":
     * R2 덮어쓰기 성공 후 이 DB UPDATE가 실패하면 R2에는 새 콘텐츠가 저장되었지만
     * 메타데이터(contentSize, updatedAt)는 이전 상태로 남는다.
     * 콘텐츠 자체는 정상 서빙되므로 MVP에서는 허용한다.
     */
    @Transactional
    public Document updateDocument(String documentId, UUID userId, String title,
                                   long newContentSize, long oldContentSize) {
        Document managed = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "문서를 찾을 수 없습니다: " + documentId));

        if (title != null) {
            managed.updateTitle(title);
        }
        managed.updateContentSize(newContentSize);
        Document saved = documentRepository.save(managed);

        // 로그인 사용자인 경우 StorageUsage 업데이트 (크기 차이만큼)
        if (userId != null) {
            long sizeDiff = newContentSize - oldContentSize;
            if (sizeDiff > 0) {
                storageUsageService.incrementUsage(userId, sizeDiff, 0);
            } else if (sizeDiff < 0) {
                storageUsageService.decrementUsage(userId, -sizeDiff, 0);
            }
        }

        return saved;
    }

    /**
     * 문서 삭제: soft delete (status=DELETED) + StorageUsage 감소 -- 단일 트랜잭션.
     * 이중 처리 방지: ACTIVE 상태가 아닌 문서는 이미 처리된 것으로 간주한다.
     */
    @Transactional
    public Document softDeleteDocument(String documentId, UUID userId, long contentSize) {
        Document managed = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "문서를 찾을 수 없습니다: " + documentId));

        if (managed.getStatus() != DocumentStatus.ACTIVE) {
            log.info("삭제 처리 건너뜀 (이미 처리됨): id={}, status={}", documentId, managed.getStatus());
            return managed;
        }

        managed.markDeleted();
        Document saved = documentRepository.save(managed);

        // 로그인 사용자인 경우 StorageUsage 감소
        if (userId != null) {
            storageUsageService.decrementUsage(userId, contentSize, 1);
        }

        return saved;
    }

    /**
     * 30일 이상 경과한 EXPIRED/DELETED 문서를 벌크 물리 삭제한다.
     * 단일 트랜잭션으로 deleteAllInBatch를 실행하여 원자성을 보장한다.
     */
    @Transactional
    public void purgeDocuments(List<Document> documents) {
        documentRepository.deleteAllInBatch(documents);
    }

    // ========== 배치 최적화 메서드 (N+1 쓰기 방지) ==========

    /**
     * 만료 문서 배치 처리: 벌크 상태 변경 + userId별 집계된 StorageUsage 감소.
     * 개별 save() 대신 벌크 UPDATE 쿼리로 N+1 쓰기를 방지한다.
     *
     * @param documents ACTIVE 상태임이 확인된 만료 문서 목록
     */
    @Transactional
    public void expireDocumentsBatch(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        // 1. 벌크 상태 변경: ACTIVE -> EXPIRED
        Collection<String> ids = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());
        documentRepository.bulkUpdateStatus(ids, DocumentStatus.EXPIRED);

        // 2. userId별 StorageUsage 집계 후 한 번씩만 감소
        decrementStorageByUser(documents);
    }

    /**
     * PENDING 문서 배치 물리 삭제.
     * R2 파일 삭제는 호출자(DocumentCleanupService)가 처리한 후 이 메서드를 호출한다.
     */
    @Transactional
    public void deletePendingDocumentsBatch(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }
        documentRepository.deleteAllInBatch(documents);
    }

    /**
     * 고아 문서 배치 만료 시간 설정.
     * 개별 save() 대신 벌크 UPDATE 쿼리로 N+1 쓰기를 방지한다.
     */
    @Transactional
    public void setOrphanExpiryBatch(List<Document> documents, LocalDateTime expiresAt) {
        if (documents.isEmpty()) {
            return;
        }
        Collection<String> ids = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());
        documentRepository.bulkSetExpiresAt(ids, expiresAt);
    }

    /**
     * userId별로 문서의 contentSize와 문서 수를 집계하여 StorageUsage를 한 번에 감소시킨다.
     */
    private void decrementStorageByUser(List<Document> documents) {
        // userId별로 그룹핑 (userId가 null인 비로그인 문서는 제외)
        Map<UUID, List<Document>> byUser = documents.stream()
                .filter(d -> d.getUserId() != null)
                .collect(Collectors.groupingBy(Document::getUserId));

        for (Map.Entry<UUID, List<Document>> entry : byUser.entrySet()) {
            UUID userId = entry.getKey();
            List<Document> userDocs = entry.getValue();
            long totalBytes = userDocs.stream().mapToLong(Document::getContentSize).sum();
            int count = userDocs.size();
            storageUsageService.decrementUsage(userId, totalBytes, count);
        }
    }
}
