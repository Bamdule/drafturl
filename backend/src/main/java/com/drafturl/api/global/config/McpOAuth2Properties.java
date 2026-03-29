package com.drafturl.api.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mcp-oauth2")
public record McpOAuth2Properties(
        String issuer,
        long authCodeTtlSeconds
) {
    public McpOAuth2Properties {
        if (authCodeTtlSeconds <= 0) {
            authCodeTtlSeconds = 300;
        }
    }
}
