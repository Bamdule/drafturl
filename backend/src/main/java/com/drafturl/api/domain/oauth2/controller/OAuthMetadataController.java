package com.drafturl.api.domain.oauth2.controller;

import com.drafturl.api.global.config.McpOAuth2Properties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class OAuthMetadataController {

    private final McpOAuth2Properties properties;

    public OAuthMetadataController(McpOAuth2Properties properties) {
        this.properties = properties;
    }

    @GetMapping(value = "/.well-known/oauth-protected-resource", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> protectedResourceMetadata() {
        return Map.of(
                "resource", properties.issuer() + "/mcp/transport",
                "authorization_servers", List.of(properties.issuer()),
                "bearer_methods_supported", List.of("header")
        );
    }

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> authorizationServerMetadata() {
        String issuer = properties.issuer();
        return Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", issuer + "/oauth2/authorize"),
                Map.entry("token_endpoint", issuer + "/oauth2/token"),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code", "refresh_token")),
                Map.entry("code_challenge_methods_supported", List.of("S256")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("none")),
                Map.entry("registration_endpoint", issuer + "/oauth2/register"),
                Map.entry("scopes_supported", List.of("document:read", "document:write"))
        );
    }
}
