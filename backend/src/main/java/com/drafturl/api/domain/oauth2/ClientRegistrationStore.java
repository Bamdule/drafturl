package com.drafturl.api.domain.oauth2;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientRegistrationStore {

    private static final long TTL_SECONDS = 604_800; // 7 days

    private final ConcurrentHashMap<String, RegisteredClient> store = new ConcurrentHashMap<>();

    public RegisteredClient register(List<String> redirectUris, String clientName) {
        validateRedirectUris(redirectUris);

        String clientId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        RegisteredClient client = new RegisteredClient(
                clientId, redirectUris, clientName, now, now.plusSeconds(TTL_SECONDS));
        store.put(clientId, client);
        return client;
    }

    public Optional<RegisteredClient> findByClientId(String clientId) {
        RegisteredClient client = store.get(clientId);
        if (client == null || client.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(client);
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        Instant now = Instant.now();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private void validateRedirectUris(List<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new InvalidRegistrationException("invalid_redirect_uri",
                    "redirect_uris is required and must not be empty");
        }
        for (String uri : redirectUris) {
            if (!isValidRedirectUri(uri)) {
                throw new InvalidRegistrationException("invalid_redirect_uri",
                        "redirect_uri must be localhost or HTTPS: " + uri);
            }
        }
    }

    private boolean isValidRedirectUri(String uri) {
        try {
            URI parsed = URI.create(uri);
            String host = parsed.getHost();
            String scheme = parsed.getScheme();
            if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
                return "http".equals(scheme) || "https".equals(scheme);
            }
            return "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    public static class InvalidRegistrationException extends RuntimeException {
        private final String error;
        private final String errorDescription;

        public InvalidRegistrationException(String error, String errorDescription) {
            super(errorDescription);
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public String getError() { return error; }
        public String getErrorDescription() { return errorDescription; }
    }
}
