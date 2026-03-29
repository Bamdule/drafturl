package com.drafturl.api.domain.oauth2;

import java.time.Instant;
import java.util.List;

public record RegisteredClient(
        String clientId,
        List<String> redirectUris,
        String clientName,
        Instant createdAt,
        Instant expiresAt
) {}
