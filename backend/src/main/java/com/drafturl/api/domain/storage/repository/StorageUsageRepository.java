package com.drafturl.api.domain.storage.repository;

import com.drafturl.api.domain.storage.entity.StorageUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, UUID> {

    Optional<StorageUsage> findByUserId(UUID userId);

    /**
     * Atomic increment로 사용량을 업데이트한다.
     * Race Condition 방지를 위해 DB 레벨에서 원자적 증감을 수행한다.
     * GREATEST를 사용하여 음수 bytes가 전달되는 케이스를 방어한다.
     */
    @Modifying
    @Query("UPDATE StorageUsage s SET s.totalBytes = GREATEST(s.totalBytes + :bytes, 0), " +
            "s.documentCount = GREATEST(s.documentCount + :count, 0), " +
            "s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.userId = :userId")
    void incrementUsage(@Param("userId") UUID userId,
                        @Param("bytes") long bytes,
                        @Param("count") int count);

    /**
     * Atomic decrement로 사용량을 감소시킨다.
     * 문서 삭제 시 호출된다.
     * GREATEST를 사용하여 음수 방지.
     */
    @Modifying
    @Query("UPDATE StorageUsage s SET s.totalBytes = GREATEST(s.totalBytes - :bytes, 0), " +
            "s.documentCount = GREATEST(s.documentCount - :count, 0), " +
            "s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.userId = :userId")
    void decrementUsage(@Param("userId") UUID userId,
                        @Param("bytes") long bytes,
                        @Param("count") int count);

}
