package com.drafturl.api.domain.tag.repository;

import com.drafturl.api.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserId(UUID userId);

    Optional<Tag> findByIdAndUserId(Long id, UUID userId);

    @Query("""
        SELECT t.id AS id, t.name AS name,
               COUNT(CASE WHEN d.status = com.drafturl.api.domain.document.DocumentStatus.ACTIVE THEN 1 ELSE null END) AS documentCount
        FROM Tag t
        LEFT JOIN DocumentTag dt ON dt.id.tagId = t.id
        LEFT JOIN Document d ON d.id = dt.id.documentId
        WHERE t.userId = :userId
        GROUP BY t.id, t.name
        ORDER BY t.name
    """)
    List<TagWithCountProjection> findTagsWithCountByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    @Query("SELECT COUNT(t) > 0 FROM Tag t WHERE t.userId = :userId AND LOWER(t.name) = LOWER(:name)")
    boolean existsByUserIdAndNameIgnoreCase(@Param("userId") UUID userId, @Param("name") String name);

    interface TagWithCountProjection {
        Long getId();
        String getName();
        Long getDocumentCount();
    }
}
