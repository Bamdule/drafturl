package com.drafturl.api.domain.user.usecase;

import com.drafturl.api.domain.user.entity.User;
import com.drafturl.api.domain.user.repository.UserRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DeleteAccountUseCase {

    private final UserRepository userRepository;

    public DeleteAccountUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"));

        // 유저 삭제 시 FK cascade 처리:
        // - documents.user_id → SET NULL (고아 문서 스케줄러가 R2 파일과 함께 정리)
        // - storage_usage → CASCADE 삭제
        // - refresh_tokens → CASCADE 삭제
        userRepository.delete(user);
    }
}
