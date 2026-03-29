package com.drafturl.api.domain.oauth2;

import com.drafturl.api.global.config.McpOAuth2Properties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthorizationCodeStore {

    private final ConcurrentHashMap<String, AuthorizationCode> store = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlSeconds;

    public AuthorizationCodeStore(McpOAuth2Properties properties) {
        this.ttlSeconds = properties.authCodeTtlSeconds();
    }

    public AuthorizationCode generate(UUID userId, String clientId, String redirectUri,
                                       String codeChallenge, String scope) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthorizationCode authCode = new AuthorizationCode(
                code, userId, clientId, redirectUri, codeChallenge, scope, Instant.now());
        store.put(code, authCode);
        return authCode;
    }

    public Optional<AuthorizationCode> consume(String code) {
        AuthorizationCode authCode = store.remove(code);
        if (authCode == null) {
            return Optional.empty();
        }
        if (authCode.createdAt().plusSeconds(ttlSeconds).isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(authCode);
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(ttlSeconds);
        store.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }
}
