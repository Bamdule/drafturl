package com.drafturl.api.domain.tag.repository;

import com.drafturl.api.domain.tag.entity.DocumentTag;
import com.drafturl.api.domain.tag.entity.DocumentTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DocumentTagRepository extends JpaRepository<DocumentTag, DocumentTagId> {

    List<DocumentTag> findById_DocumentIdIn(Collection<String> documentIds);

    boolean existsById_DocumentIdAndId_TagId(String documentId, Long tagId);

    long countById_DocumentId(String documentId);

    @Query("SELECT COUNT(dt) FROM DocumentTag dt JOIN Document d ON d.id = dt.id.documentId WHERE dt.id.tagId = :tagId AND d.status = 'ACTIVE'")
    long countActiveDocumentsForTag(@Param("tagId") Long tagId);
}
