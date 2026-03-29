package com.drafturl.api.domain.oauth2;

import java.time.Instant;
import java.util.UUID;

public record AuthorizationCode(
        String code,
        UUID userId,
        String clientId,
        String redirectUri,
        String codeChallenge,
        String scope,
        Instant createdAt
) {}
