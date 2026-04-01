package com.drafturl.api.domain.user.repository;

import com.drafturl.api.domain.user.entity.WithdrawalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WithdrawalLogRepository extends JpaRepository<WithdrawalLog, UUID> {
}
