package com.drafturl.api.domain.user.repository;

import com.drafturl.api.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.userId = :userId AND rt.isRevoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);
}
