package com.drafturl.api.domain.document.service;

import com.drafturl.api.domain.document.repository.DocumentRepository;
import com.drafturl.api.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * URL-safe slug(nanoid 12자리) 생성기.
 * SecureRandom + 커스텀 알파벳 [A-Za-z0-9] 사용.
 * DocumentRepository로 충돌을 확인하고 최대 3회 재시도한다.
 */
@Component
public class SlugGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SLUG_LENGTH = 12;
    private static final int MAX_RETRIES = 3;

    private final SecureRandom random = new SecureRandom();
    private final DocumentRepository documentRepository;

    public SlugGenerator(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * 유일한 slug를 생성한다.
     *
     * @return 고유 slug (12자리)
     * @throws BusinessException 최대 재시도 횟수 초과 시 (INTERNAL_ERROR)
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String slug = generateRandom();
            if (documentRepository.findBySlug(slug).isEmpty()) {
                return slug;
            }
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "slug 생성 실패: 최대 재시도 횟수 초과");
    }

    private String generateRandom() {
        char[] chars = new char[SLUG_LENGTH];
        for (int i = 0; i < SLUG_LENGTH; i++) {
            chars[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(chars);
    }
}
