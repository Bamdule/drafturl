package com.drafturl.api.domain.document.repository;

import com.drafturl.api.domain.document.entity.Document;
import com.drafturl.api.domain.document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, String> {

    Optional<Document> findBySlug(String slug);

    Page<Document> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, DocumentStatus status, Pageable pageable);

    /**
     * 만료된 ACTIVE 문서 조회 (expiresAt이 지난 문서).
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'ACTIVE' " +
            "AND d.expiresAt IS NOT NULL AND d.expiresAt < :now " +
            "ORDER BY d.expiresAt ASC")
    List<Document> findExpiredDocuments(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 5분 초과 PENDING 문서 조회 (생성 후 방치된 문서).
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'PENDING' " +
            "AND d.createdAt < :cutoff " +
            "ORDER BY d.createdAt ASC")
    List<Document> findStalePendingDocuments(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    /**
     * 30일 이상 경과한 EXPIRED/DELETED 문서 조회 (물리 삭제 대상).
     */
    @Query("SELECT d FROM Document d WHERE d.status IN :statuses " +
            "AND d.updatedAt < :cutoff " +
            "ORDER BY d.updatedAt ASC")
    List<Document> findPurgableDocuments(@Param("statuses") List<DocumentStatus> statuses,
                                         @Param("cutoff") LocalDateTime cutoff,
                                         Pageable pageable);

    /**
     * 고아 문서 조회 (userId가 null이고 expiresAt이 null인 ACTIVE 문서).
     */
    @Query("SELECT d FROM Document d WHERE d.userId IS NULL " +
            "AND d.expiresAt IS NULL AND d.status = 'ACTIVE' " +
            "ORDER BY d.createdAt ASC")
    List<Document> findOrphanDocuments(Pageable pageable);

    /**
     * 벌크 상태 변경: 지정된 ID 목록의 문서 상태를 일괄 변경한다.
     */
    @Modifying
    @Query("UPDATE Document d SET d.status = :status, d.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE d.id IN :ids")
    int bulkUpdateStatus(@Param("ids") Collection<String> ids,
                         @Param("status") DocumentStatus status);

    /**
     * 벌크 만료 시간 설정: 지정된 ID 목록의 문서에 expiresAt을 일괄 설정한다.
     */
    @Modifying
    @Query("UPDATE Document d SET d.expiresAt = :expiresAt, d.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE d.id IN :ids")
    int bulkSetExpiresAt(@Param("ids") Collection<String> ids,
                         @Param("expiresAt") LocalDateTime expiresAt);
}
