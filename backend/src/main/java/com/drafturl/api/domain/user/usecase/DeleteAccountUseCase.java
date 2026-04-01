package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.entity.WithdrawalLog;
import com.drafturl.api.domain.user.repository.UserRepository;
import com.drafturl.api.domain.user.repository.WithdrawalLogRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;
    private final DocumentRepository documentRepository;

    public DeleteAccountUseCase(UserRepository userRepository,
                                 WithdrawalLogRepository withdrawalLogRepository,
                                 DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.withdrawalLogRepository = withdrawalLogRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"));

        // 탈퇴 로그 기록 (이메일은 SHA-256 해시로 저장)
        withdrawalLogRepository.save(new WithdrawalLog(hashEmail(user.getEmail()), null));

        // 유저의 ACTIVE 문서에 1시간 만료 부여 → 기존 배치가 R2 파일과 함께 정리
        documentRepository.bulkSetExpiresAtByUserId(userId, LocalDateTime.now().plusHours(1));

        // 유저 삭제 (FK cascade: documents.user_id→NULL, storage_usage→CASCADE, refresh_tokens→CASCADE)
        userRepository.delete(user);
    }

    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
