package com.drafturl.api.domain.document.service;

import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.port.FileStorage;
import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.document.DocumentStatus;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄러에서 호출하는 문서 정리 로직을 담당하는 서비스.
 * UseCase의 비대화를 방지하기 위해 분리되었다.
 *
 * R2 파일 삭제(외부 I/O)는 개별 처리하되, DB 변경은 DocumentTransactionService의
 * 배치 트랜잭션 메서드를 호출하여 N+1 쓰기를 방지한다.
 */
// Phase 2에서 Clock 주입으로 전환하면 테스트 용이성 향상
@Service
public class DocumentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCleanupService.class);

    private static final int CLEANUP_BATCH_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentTransactionService transactionService;
    private final FileStorage fileStorage;

    public DocumentCleanupService(DocumentRepository documentRepository,
                                   DocumentTransactionService transactionService,
                                   FileStorage fileStorage) {
        this.documentRepository = documentRepository;
        this.transactionService = transactionService;
        this.fileStorage = fileStorage;
    }

    /**
     * 만료된 ACTIVE 문서를 처리한다.
     * - R2 파일 개별 삭제 시도 (외부 I/O이므로 개별 처리)
     * - status를 EXPIRED로 벌크 변경
     * - StorageUsage를 userId별로 집계하여 한 번에 감소
     * - 30일 이상 경과한 EXPIRED/DELETED 문서를 물리 삭제
     */
    public void processExpiredDocuments() {
        LocalDateTime now = LocalDateTime.now();
        Pageable limit = PageRequest.of(0, CLEANUP_BATCH_SIZE);
        List<Document> expiredDocuments = documentRepository.findExpiredDocuments(now, limit);

        if (expiredDocuments.isEmpty()) {
            purgeDeletedDocuments();
            return;
        }

        // 1단계: R2 파일 개별 삭제 (외부 I/O — 배치 불가)
        List<Document> r2Processed = new ArrayList<>(expiredDocuments.size());
        int r2Failed = 0;

        for (Document document : expiredDocuments) {
            try {
                fileStorage.delete(document.getR2Key());
                r2Processed.add(document);
            } catch (Exception e) {
                // R2 삭제 실패해도 DB 상태 전이는 진행 (R2 파일은 나중에 재시도 가능)
                log.warn("만료 문서 R2 파일 삭제 실패 (DB 처리는 계속): id={}, r2Key={}",
                        document.getId(), document.getR2Key(), e);
                r2Processed.add(document);
                r2Failed++;
            }
        }

        // 2단계: 벌크 상태 변경 + userId별 집계된 StorageUsage 감소 (단일 트랜잭션)
        try {
            transactionService.expireDocumentsBatch(r2Processed);
            log.info("만료 문서 처리 완료: processed={}, r2Failed={}", r2Processed.size(), r2Failed);
        } catch (Exception e) {
            log.error("만료 문서 벌크 처리 실패: count={}", r2Processed.size(), e);
            Sentry.captureException(e);
        }

        // 30일 이상 경과한 EXPIRED/DELETED 문서 물리 삭제
        purgeDeletedDocuments();
    }

    /**
     * 5분 초과 PENDING 문서를 정리한다.
     * - R2 파일 개별 삭제 시도 (외부 I/O)
     * - DB 벌크 물리 삭제 (deleteAllInBatch)
     */
    public void cleanupPendingDocuments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        Pageable limit = PageRequest.of(0, CLEANUP_BATCH_SIZE);
        List<Document> pendingDocuments = documentRepository.findStalePendingDocuments(cutoff, limit);

        if (pendingDocuments.isEmpty()) {
            return;
        }

        // 1단계: R2 파일 개별 삭제 (외부 I/O — 배치 불가)
        List<Document> toDelete = new ArrayList<>(pendingDocuments.size());
        for (Document document : pendingDocuments) {
            try {
                fileStorage.delete(document.getR2Key());
                toDelete.add(document);
            } catch (Exception e) {
                // R2 삭제 실패해도 DB 삭제는 진행 (PENDING 문서는 어차피 불완전한 상태)
                log.warn("PENDING 문서 R2 파일 삭제 실패 (DB 삭제는 계속): id={}, r2Key={}",
                        document.getId(), document.getR2Key(), e);
                toDelete.add(document);
            }
        }

        // 2단계: DB 벌크 물리 삭제 (단일 트랜잭션)
        try {
            transactionService.deletePendingDocumentsBatch(toDelete);
            log.info("PENDING 문서 정리 완료: processed={}", toDelete.size());
        } catch (Exception e) {
            log.error("PENDING 문서 벌크 삭제 실패: count={}", toDelete.size(), e);
            Sentry.captureException(e);
        }
    }

    /**
     * 30일 이상 경과한 EXPIRED/DELETED 문서를 물리 삭제한다.
     * R2 파일은 만료/삭제 시점에 이미 삭제된 상태이므로 DB 레코드만 삭제한다.
     * 벌크 삭제(deleteAllInBatch)로 N+1 쓰기를 방지한다.
     */
    private void purgeDeletedDocuments() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        Pageable limit = PageRequest.of(0, CLEANUP_BATCH_SIZE);
        List<DocumentStatus> statuses = List.of(DocumentStatus.EXPIRED, DocumentStatus.DELETED);
        List<Document> purgableDocuments = documentRepository.findPurgableDocuments(statuses, cutoff, limit);

        if (purgableDocuments.isEmpty()) {
            return;
        }

        try {
            transactionService.purgeDocuments(purgableDocuments);
            log.info("문서 물리 삭제 완료: count={}", purgableDocuments.size());
        } catch (Exception e) {
            log.error("문서 벌크 물리 삭제 실패: count={}", purgableDocuments.size(), e);
            Sentry.captureException(e);
        }
    }

    /**
     * 고아 문서에 만료 시간을 부여한다.
     * userId가 null이고 expiresAt이 null인 ACTIVE 문서에 24시간 만료를 벌크 설정한다.
     */
    public void cleanupOrphanDocuments() {
        Pageable limit = PageRequest.of(0, CLEANUP_BATCH_SIZE);
        List<Document> orphanDocuments = documentRepository.findOrphanDocuments(limit);

        if (orphanDocuments.isEmpty()) {
            return;
        }

        try {
            transactionService.setOrphanExpiryBatch(orphanDocuments, LocalDateTime.now().plusHours(24));
            log.info("고아 문서 만료 설정 완료: processed={}", orphanDocuments.size());
        } catch (Exception e) {
            log.error("고아 문서 벌크 만료 설정 실패: count={}", orphanDocuments.size(), e);
            Sentry.captureException(e);
        }
    }
}
